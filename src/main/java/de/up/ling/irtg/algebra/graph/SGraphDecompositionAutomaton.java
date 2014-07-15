/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class SGraphDecompositionAutomaton extends TreeAutomaton<SGraph> {
    private SGraph completeGraph;

    SGraphDecompositionAutomaton(SGraph completeGraph, Signature signature) {
        super(signature);

        this.completeGraph = completeGraph;
        int x = addState(completeGraph);
        finalStates.add(x);
    }
    
    private Rule makeRule(SGraph parent, int labelId, int[] childStates) {
        int parentState = addState(parent);
        return createRule(parentState, labelId, childStates, 1);
    }
    
    private static <E> Iterable<E> sing(E object) {
        return Collections.singletonList(object);
    }
    
    private Iterable<Rule> sing(SGraph parent, int labelId, int[] childStates) {
        return sing(makeRule(parent, labelId, childStates));
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        String label = signature.resolveSymbolId(labelId);
        SGraph[] children = (SGraph[]) Arrays.stream(childStates).mapToObj(q -> getStateForId(labelId)).toArray();

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals("merge")) {
                if( ! children[0].overlapsOnlyInSources(children[1])) {
                    return Collections.EMPTY_LIST;
                } else {
                    return sing(children[0].merge(children[1]), labelId, childStates);
                }                
            } else if (label.startsWith("r_")) {
                String[] parts = label.split("_");
                
                if( parts.length == 2 ) {
                    parts = new String[] { "r", "root", parts[1] };
                }
                
                // this should be ok
                return sing(children[0].renameSource(parts[1], parts[2]), labelId, childStates);
            } else if( label.equals("f")) {
                // this should be ok
                return sing(children[0].forgetSourcesExcept(Collections.EMPTY_SET));
            } else if( label.startsWith("f_")) {
                String[] parts = label.split("_");
                Set<String> retainedSources = new HashSet<>();
                for( int i = 1; i < parts.length; i++ ) {
                    retainedSources.add(parts[i]);
                }
                
                return sing(children[0].forgetSourcesExcept(retainedSources), labelId, childStates);
            } else {
                List<Rule> rules = new ArrayList<Rule>();                
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));
                
                completeGraph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
                    rules.add(makeRule(matchedSubgraph, labelId, childStates));
                });
                
                return rules;
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return false;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return true;
    }
}
