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
import java.util.stream.Collectors;

/**
 *
 * @author koller
 */
public class SGraphDecompositionAutomaton extends TreeAutomaton<SGraph> {
    private GraphAlgebra algebra;
    private SGraph completeGraph;

    SGraphDecompositionAutomaton(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);

        this.algebra = algebra;
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
        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        String label = signature.resolveSymbolId(labelId);
        List<SGraph> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());

        System.err.println("grbu: " + label + children);

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals(GraphAlgebra.OP_MERGE)) {
                if (!children.get(0).overlapsOnlyInSources(children.get(1)) || !children.get(0).nodenamesForSourcesAgree(children.get(1))) {
                    return Collections.EMPTY_LIST;
                } else {
                    SGraph result = children.get(0).merge(children.get(1));

                    if (result == null) {
                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                        return Collections.EMPTY_LIST;
                    } else {
                        result.setEqualsMeansIsomorphy(false);
                        return sing(result, labelId, childStates);
                    }
                }
            } else if( label.startsWith(GraphAlgebra.OP_RENAME) ||
                       label.startsWith(GraphAlgebra.OP_FORGET) ||
                       label.startsWith(GraphAlgebra.OP_FORGET_ALL) ||
                       label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT) ||
                       label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT) ) {
                // delegate source-renaming operations to the algebra
                SGraph result = algebra.evaluate(label, children);
                
                if (result == null) {
                    System.err.println(label + " returned null: " + children.get(0));
                    return Collections.EMPTY_LIST;
                } else {
                    result.setEqualsMeansIsomorphy(false);
                    return sing(result, labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<Rule>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
                    System.err.println("Unparsable operation: " + label);
                    return Collections.EMPTY_LIST;
                }

                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    matchedSubgraph.setEqualsMeansIsomorphy(false);
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
