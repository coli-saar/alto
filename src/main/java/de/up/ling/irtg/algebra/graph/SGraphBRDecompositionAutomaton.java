/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class SGraphBRDecompositionAutomaton extends TreeAutomaton<BoundaryRepresentation> {

    private final GraphAlgebra algebra;
    private final SGraph completeGraph;
    private final LongSet allEdges;
    private IntTrie<Int2ObjectMap<Iterable<Rule>>> storedRules;
    private final PairwiseShortestPaths pwsp;
    public Map<BoundaryRepresentation, Set<Rule>> rulesTopDown;
    public Map<String, Integer> decompLengths;
    private final Map<String, Integer> sourcenameToInt;
    private final Map<String, Integer> nodenameToInt;
    private final String[] intToSourcename;
    private final String[] intToNodename;

    SGraphBRDecompositionAutomaton(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);

        this.algebra = algebra;
        //getStateInterner().setTrustingMode(true);

        //find all sources used in algebra:
        Set<String> sources = new HashSet<>();
        sourcenameToInt = new HashMap<>();
        nodenameToInt = new HashMap<>();
        for (String symbol : signature.getSymbols())//this adds all sources from the signature, but be careful, this is kind of a hack. Maybe better just give this a list of sources directly?
        {
            if (symbol.startsWith(GraphAlgebra.OP_FORGET)) {
                String[] parts = symbol.split("_");
                sources.add(parts[1]);
            } else if (symbol.startsWith(GraphAlgebra.OP_RENAME)) {
                String[] parts = symbol.split("_");
                if (parts.length == 2) {
                    sources.add("root");
                }
                for (int i = 1; i < parts.length; i++) {
                    sources.add(parts[i]);
                }
            }
        }

        intToSourcename = new String[sources.size()];
        int i = 0;
        for (String source : sources) {
            sourcenameToInt.put(source, i);
            intToSourcename[i] = source;
            i++;
        }
        intToNodename = new String[completeGraph.getAllNodeNames().size()];
        i = 0;
        for (String nodename : completeGraph.getAllNodeNames()) {
            nodenameToInt.put(nodename, i);
            intToNodename[i] = nodename;
            i++;
        }

        allEdges = new LongOpenHashSet();
        completeGraph.getGraph().edgeSet().stream().forEach((edge) -> {
            allEdges.add(NumbersCombine.combine(nodenameToInt.get(edge.getSource().getName()), nodenameToInt.get(edge.getTarget().getName())));
        });
        for (int j = 0; j < intToNodename.length; j++) {
            allEdges.add(NumbersCombine.combine(j, j));
        }

        storedRules = new IntTrie<>();
        this.completeGraph = completeGraph;
        int x = addState(new BoundaryRepresentation(completeGraph, this));
        finalStates.add(x);

        pwsp = new PairwiseShortestPaths(completeGraph, this);
    }

    public int getNumberNodes() {
        return completeGraph.getGraph().vertexSet().size();
    }

    private Rule makeRule(BoundaryRepresentation parent, int labelId, int[] childStates) {
        
        
        /*StringBuilder message = new StringBuilder();
        message.append(parent.toString(this)+" from " + signature.resolveSymbolId(labelId));
        for (int i = 0; i<childStates.length; i++){
            message.append(" __ "+getStateForId(childStates[i]).toString(this));
        }
        System.out.println(message);
        SGraph graph = parent.getGraph(completeGraph, this);
        System.out.println("sgraph: " + graph.toIsiAmrString());*/
        
        int parentState = addState(parent);
        return createRule(parentState, labelId, childStates, 1);
    }

    private static <E> Iterable<E> sing(E object) {
        return Collections.singletonList(object);
    }
    
    private Iterable<Rule> sing(BoundaryRepresentation parent, int labelId, int[] childStates) {
//        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }


    private Iterable<Rule> memoize(Iterable<Rule> rules, int labelId, int[] childStates) {
        // memoize rule
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(childStates, rulesHere);
        }

        rulesHere.put(labelId, rules);

     // add final state if needed
     for( Rule rule : rules ) {
         BoundaryRepresentation parent = getStateForId(rule.getParent());

         if( parent.isIdenticalExceptSources(completeGraph, completeGraph, this)) {
            finalStates.add(rule.getParent());
         }
         }
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
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        List<BoundaryRepresentation> children = new ArrayList<>();
        for (int i = 0; i < childStates.length; i++) {
            children.add(getStateForId(childStates[i]));
        }

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals(GraphAlgebra.OP_MERGE)) {
                if (!children.get(0).isMergeable(pwsp, children.get(1))) { // ensure result is connected
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    BoundaryRepresentation result = children.get(0).merge(children.get(1));

                    if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                        return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                    } else {
                        //result.setEqualsMeansIsomorphy(false);//is this a problem??
                        return memoize(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                    }
                }
            } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                    || label.startsWith(GraphAlgebra.OP_FORGET)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                    || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {

                BoundaryRepresentation arg = children.get(0);

                for (Integer sourceToForget : arg.getForgottenSources(label, this))//check if we may forget.
                {
                    if (!arg.isForgetAllowed(sourceToForget, completeGraph, this)) {
                        return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;//
                    }
                }

                // now we can apply the operation.
                BoundaryRepresentation result = arg.applyForgetRename(label, this);// maybe do the above check in here? might be more efficient.

                if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return memoize(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);//return Collections.EMPTY_LIST;
                }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, this), labelId, childStates));
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });

                return memoize(rules, labelId, childStates);//return rules;
            }
        } catch (de.up.ling.irtg.algebra.graph.ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }

    
    public Iterable<Rule> getRulesBottomUpMPF(int labelId, int[] childStates) {
        String label = signature.resolveSymbolId(labelId);
        
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        List<BoundaryRepresentation> children = new ArrayList<>();
        for (int i = 0; i < childStates.length; i++) {
            children.add(getStateForId(childStates[i]));
        }

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals(GraphAlgebra.OP_MERGE)) {
                if (!children.get(0).isMergeableMPF(pwsp, children.get(1))) { // ensure result is connected
                    return Collections.EMPTY_LIST;
                } else {
                    BoundaryRepresentation result = children.get(0).merge(children.get(1));

                    if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                        return Collections.EMPTY_LIST;
                    } else {
                        //result.setEqualsMeansIsomorphy(false);//is this a problem??
                        return sing(result, labelId, childStates);
                    }
                }
            } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                    || label.startsWith(GraphAlgebra.OP_FORGET)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                    || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {

                BoundaryRepresentation arg = children.get(0);

                for (Integer sourceToForget : arg.getForgottenSources(label, this))//check if we may forget.
                {
                    if (!arg.isForgetAllowed(sourceToForget, completeGraph, this)) {
                        return Collections.EMPTY_LIST;//
                    }
                }

                // now we can apply the operation.
                BoundaryRepresentation result = arg.applyForgetRename(label, this);// maybe do the above check in here? might be more efficient.

                if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                    return Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return sing(result, labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                    return Collections.EMPTY_LIST;
                }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, this), labelId, childStates));
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });

                return rules;
            }
        } catch (de.up.ling.irtg.algebra.graph.ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }
    
    
    private boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = completeGraph.getNode(nodename);

                if (!completeGraph.getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + completeGraph);
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

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

    public int getIntForSource(String source) {
        return sourcenameToInt.get(source);
    }

    public int getIntForNode(String nodename) {
        return nodenameToInt.get(nodename);
    }

    public String getSourceForInt(int source) {
        return intToSourcename[source];
    }

    public String getNodeForInt(int node) {
        return intToNodename[node];
    }

    public int getNrSources() {
        return intToSourcename.length;
    }

    public long[] getAllIncidentEdges(IntSet vertices) {
        LongSet res = new LongOpenHashSet();
        for (LongIterator it = allEdges.iterator(); it.hasNext();) {
            long edge = it.nextLong();
            if (vertices.contains(NumbersCombine.getFirst(edge))
                    || vertices.contains(NumbersCombine.getSecond(edge))) {
                res.add(edge);
            }
        }
        for (int i : vertices) {
            res.add(NumbersCombine.combine(i, i));
        }
        return res.toLongArray();
    }

    public long[] getAllEdges() {
        return allEdges.toLongArray();
    }

}
