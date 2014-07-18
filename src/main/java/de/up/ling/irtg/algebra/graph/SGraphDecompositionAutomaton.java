/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author koller
 */
public class SGraphDecompositionAutomaton extends TreeAutomaton<SGraph> {

    private GraphAlgebra algebra;
    private SGraph completeGraph;
    private IntTrie<Int2ObjectMap<Iterable<Rule>>> storedRules;

    SGraphDecompositionAutomaton(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);

        this.algebra = algebra;

        storedRules = new IntTrie<>();

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
//        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }

    private Iterable<Rule> memoize(Iterable<Rule> rules, int labelId, int[] childStates) {
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(childStates, rulesHere);
        }

        rulesHere.put(labelId, rules);
        return rules;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

        // check stored rules
        if (rulesHere != null) {
            Iterable<Rule> rules = rulesHere.get(labelId);
            if (rules != null) {
                return rules;
            }
        }

        String label = signature.resolveSymbolId(labelId);
        List<SGraph> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals(GraphAlgebra.OP_MERGE)) {
                if (!children.get(0).overlapsOnlyInSources(children.get(1)) 
                        || !children.get(0).nodenamesForSourcesAgree(children.get(1)) 
                        || !children.get(0).hasCommonSource(children.get(1))) { // ensure result is connected
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);
                } else {
                    SGraph result = children.get(0).merge(children.get(1));

                    if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                        return memoize(Collections.EMPTY_LIST, labelId, childStates);
                    } else {
                        result.setEqualsMeansIsomorphy(false);
                        return memoize(sing(result, labelId, childStates), labelId, childStates);
                    }
                }
            } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                    || label.startsWith(GraphAlgebra.OP_FORGET)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                    || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {

                SGraph arg = children.get(0);

                // check whether forgetting a source would create an edge between
                // a source-less node and the outside of the subgraph; such subgraphs
                // can never be completed (thanks to Frank Drewes for this tip)
                Iterable<String> forgottenSources = GraphAlgebra.getForgottenSources(label, arg);
                Iterable<String> forgottenSourceNodes = Iterables.transform(forgottenSources, x -> arg.getNodeForSource(x));

                if (hasCrossingEdgesFromNodes(forgottenSourceNodes, arg)) {
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);
                }

                // delegate source-renaming operations to the algebra
                SGraph result = algebra.evaluate(label, children);

                if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);
                } else {
                    result.setEqualsMeansIsomorphy(false);
                    return memoize(sing(result, labelId, childStates), labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<Rule>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);
                }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        rules.add(makeRule(matchedSubgraph, labelId, childStates));
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });

                return memoize(rules, labelId, childStates);
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }

    private boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {

                GraphNode node = completeGraph.getNode(nodename);

                for (GraphEdge edge : completeGraph.getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : completeGraph.getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
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

    public static void main(String[] args) throws Exception {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/coref.irtg"));
        Map<String, String> input = new HashMap<String, String>();
        input.put("graph", "(u91<root> / want-01    :ARG0 (u92<coref1> / bill)  :ARG1 (u93 / like-01      :ARG0 (u94 / girl)  	  :ARG1 u92)   :dummy u94)");
        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            TreeAutomaton chart = irtg.parse(input);
            System.err.println((System.nanoTime() - start) / 1000000);
        }

    }
}
