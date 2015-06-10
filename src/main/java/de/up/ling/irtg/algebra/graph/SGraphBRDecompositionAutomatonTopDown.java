/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.saar.basic.Pair;
import de.up.ling.irtg.script.SGraphParsingEvaluation;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A top-down decomposition automaton for the s-graph algebra, using
 * {@code SComponentRepresentation}s as states.
 * @author grpschwitz
 */
public class SGraphBRDecompositionAutomatonTopDown extends TreeAutomaton<SComponentRepresentation>{

    private final GraphInfo completeGraphInfo;
    
    
    private final Set<SComponentRepresentation>[] storedConstants;

    @Override
    public Set<SComponentRepresentation> getStoredConstantsForID(int labelID) {
        return storedConstants[labelID];
    }
    
   
    
    
    private final Int2ObjectMap<Int2ObjectMap<List<Rule>>> storedRules;
    
    //final Long2ObjectMap<Long2IntMap> storedStates;
    
    private final Map<SComponent, SComponent> storedComponents;
    
    /**
     * Initializes a decomposition automaton for {@code completeGraph} with respect to {@code algebra}.
     * @param completeGraph
     * @param algebra 
     */
    public SGraphBRDecompositionAutomatonTopDown(SGraph completeGraph, GraphAlgebra algebra) {
        super(algebra.getSignature());
        //getStateInterner().setTrustingMode(true);

        hasStoredConstants = true;//this speeds up the pattern matching.
        
        completeGraphInfo = new GraphInfo(completeGraph, algebra);
        storedComponents = new HashMap<>(); 
        
        
        
        storedConstants = new HashSet[algebra.getSignature().getMaxSymbolId()+1];
        Map<String, Integer> symbols = algebra.getSignature().getSymbolsWithArities();
        for (String label : symbols.keySet()) {
            if (symbols.get(label) == 0) {
                int labelID = algebra.getSignature().getIdForSymbol(label);
                storedConstants[labelID] = new HashSet<>();
                SGraph sgraph;
                try {
                    sgraph = algebra.parseString(label);
                } catch (java.lang.Exception e) {
                    sgraph = null;
                    System.err.println("parsing error when creating Top Down automaton!");
                }
                completeGraphInfo.getSGraph().foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        storedConstants[labelID].add(new SComponentRepresentation(matchedSubgraph, storedComponents, completeGraphInfo));
                        //System.err.println("found constant: "+labelID+"/"+matchedSubgraph.toIsiAmrString());
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });
                
            }
        }
        Set<SComponentRepresentation> completeGraphStates = new HashSet<>();
        SGraph bareCompleteGraph = completeGraph.forgetSourcesExcept(new HashSet<>());
        completeGraphStates.add(new SComponentRepresentation(bareCompleteGraph, storedComponents, completeGraphInfo));
        for (int source = 0; source < completeGraphInfo.getNrSources(); source++) {
            Set<SComponentRepresentation> newHere = new HashSet<>();
            for (SComponentRepresentation oldRep : completeGraphStates) {
                for (SComponent comp : oldRep.getComponents()) {
                    Int2ObjectMap<SComponent> nonsplitChildren = comp.getAllNonSplits(storedComponents, completeGraphInfo);
                    for (int v : nonsplitChildren.keySet()) {
                        SComponentRepresentation child = oldRep.forgetReverse(source, v, comp, nonsplitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                    Int2ObjectMap<Set<SComponent>> splitChildren = comp.getAllSplits(storedComponents, completeGraphInfo);
                    for (int v : splitChildren.keySet()) {
                        SComponentRepresentation child = oldRep.forgetReverse(source, v, comp, splitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                }
            }
            completeGraphStates.addAll(newHere);
        }
        for (SComponentRepresentation completeRep : completeGraphStates) {
            int x = addState(completeRep);
            finalStates.add(x);
        }
        
        
        storedRules = new Int2ObjectOpenHashMap<>();
        
    
    }



    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        
        Int2ObjectMap<List<Rule>> rulesHere = storedRules.get(parentState);

        // check stored rules
        if (rulesHere != null) {
            List<Rule> rules = rulesHere.get(labelId);
            if (rules != null) {
                SGraphParsingEvaluation.cachedAnswers+=rules.size();
                switch (signature.getArity(labelId)) {
                    case 0: SGraphParsingEvaluation.averageLogger.increaseValue("constants recognised"); break;
                    case 1: SGraphParsingEvaluation.averageLogger.increaseValue("unaries recognised"); break;
                    case 2: SGraphParsingEvaluation.averageLogger.increaseValue("merges recognised"); break;
                }
                return rules;
            }
        }

        String label = signature.resolveSymbolId(labelId);
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        SComponentRepresentation parent = getStateForId(parentState);
        List<Rule> rules = new ArrayList<>();
        
        if (label.equals(GraphAlgebra.OP_MERGE)) {
            SGraphParsingEvaluation.averageLogger.increaseValue("merge tests");
            Set<SComponent> parentComponents = parent.getComponents();
            
            getAllNonemptyComponentDistributions(parentComponents).forEach(pair -> {
                SComponentRepresentation child0 = parent.getChildFromComponents(pair.getLeft());
                SComponentRepresentation child1 = parent.getChildFromComponents(pair.getRight());
                rules.add(makeRule(parentState, labelId, new SComponentRepresentation[]{child0, child1}));
                rules.add(makeRule(parentState, labelId, new SComponentRepresentation[]{child1, child0}));
                if (!child0.isConnected() || !child1.isConnected()) {
                    SGraphParsingEvaluation.averageLogger.increaseValueBy("total disconnected merge rules", 2);
                }
            });
            SGraphParsingEvaluation.averageLogger.increaseValueBy("total merge rules", rules.size());
            
            
            
        } else if (label.startsWith(GraphAlgebra.OP_COMBINEDMERGE)) {
            SGraphParsingEvaluation.averageLogger.increaseValue("comibed rename-merge tests");
            List<SComponentRepresentation[]> allSplits = new ArrayList<>();
            Set<SComponent> parentComponents = parent.getComponents();
            
            getAllNonemptyComponentDistributions(parentComponents).forEach(pair -> {
                SComponentRepresentation child0 = parent.getChildFromComponents(pair.getLeft());
                SComponentRepresentation child1 = parent.getChildFromComponents(pair.getRight());
                allSplits.add(new SComponentRepresentation[]{child0, child1});
                allSplits.add(new SComponentRepresentation[]{child1, child0});
            });
            for (SComponentRepresentation[] childStates : allSplits) {
                
                String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_COMBINEDMERGE.length());
                int[] renameSources = completeGraphInfo.getlabelSources(signature.getIdForSymbol(renameLabel));
                
                SComponentRepresentation renamedRight = childStates[1].renameReverse(renameSources[0], renameSources[1]);
                if (renamedRight != null) {
                    rules.add(makeRule(parentState, labelId, new SComponentRepresentation[]{childStates[0], renamedRight}));
                }
                
            }
        } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
            SGraphParsingEvaluation.averageLogger.increaseValue("forget tests");
            int forgottenSource = completeGraphInfo.getlabelSources(labelId)[0];
            
            if (parent.getSourceNode(forgottenSource) == -1) {
                SGraphParsingEvaluation.averageLogger.increaseValue("successfull forget tests");
                for (SComponent comp : parent.getComponents()) {
                    Int2ObjectMap<SComponent> nonsplitChildren = comp.getAllNonSplits(storedComponents, completeGraphInfo);
                    for (int v : nonsplitChildren.keySet()) {
                        SComponentRepresentation child = parent.forgetReverse(forgottenSource, v, comp, nonsplitChildren.get(v));
                        if (child != null) {
                            rules.add(makeRule(parentState, labelId, new SComponentRepresentation[]{child}));
                        }
                    }
                    Int2ObjectMap<Set<SComponent>> splitChildren = comp.getAllSplits(storedComponents, completeGraphInfo);
                    for (int v : splitChildren.keySet()) {
                        SComponentRepresentation child = parent.forgetReverse(forgottenSource, v, comp, splitChildren.get(v));
                        if (child != null) {
                            rules.add(makeRule(parentState, labelId, new SComponentRepresentation[]{child}));
                        }
                    }
                }
                SGraphParsingEvaluation.averageLogger.increaseValueBy("total forget rules", rules.size());
            }
            //else just dont add a rule
            
            
        } else if (label.startsWith(GraphAlgebra.OP_RENAME)) {
            SGraphParsingEvaluation.averageLogger.increaseValue("rename tests");
            int[] renamedSources = completeGraphInfo.getlabelSources(labelId);
            SComponentRepresentation child = parent.renameReverse(renamedSources[0], renamedSources[1]);
            if (child != null) {
                SGraphParsingEvaluation.averageLogger.increaseValue("successfull rename tests");
                rules.add(makeRule(parentState, labelId, new SComponentRepresentation[]{child}));
            }
            
            
        } else {
            SGraphParsingEvaluation.averageLogger.increaseValue("constant tests");
            if (storedConstants[labelId].contains(parent)) {
            SGraphParsingEvaluation.averageLogger.increaseValue("constants found");
                rules.add(makeRule(parentState, labelId, new SComponentRepresentation[0]));
            }
        }
        
        SGraphParsingEvaluation.newAnswers+= rules.size();
        return memoize(rules, labelId, parentState);
        
    }

    
    
    
    
    
    private Rule makeRule(int parentState, int labelId, SComponentRepresentation[] children) {
        int[] childStates = new int[children.length];
        
        for (int i = 0; i<children.length; i++) {
            childStates[i] = addState(children[i]);
        }
        return createRule(parentState, labelId, childStates, 1);
    }
    
    
    
    
    private Iterable<Rule> memoize(List<Rule> rules, int labelId, int parentState) {
        Int2ObjectMap<List<Rule>> rulesHere = storedRules.get(parentState);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(parentState, rulesHere);
        }

        rulesHere.put(labelId, rules);
        return rules;
    }
    
    
    
    
    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = completeGraphInfo.getSGraph().getNode(nodename);

                if (!completeGraphInfo.getSGraph().getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + completeGraphInfo.getSGraph());
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : completeGraphInfo.getSGraph().getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : completeGraphInfo.getSGraph().getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    
    
    private List<Pair<Set<SComponent>, Set<SComponent>>> getAllNonemptyComponentDistributions(Set<SComponent> components) {
        if (components.isEmpty()) {
            return new ArrayList<>();
        } else {
            Set<SComponent> input = new HashSet<>(components);
            SComponent comp = components.iterator().next();
            input.remove(comp);
            Set<SComponent> with = new HashSet<>();
            with.add(comp);
            
            return getAllNonemptyComponentDistributionsRecursive(input, new Pair<>(with, new HashSet<>()));
        }
    }
    
    private List<Pair<Set<SComponent>, Set<SComponent>>> getAllNonemptyComponentDistributionsRecursive(Set<SComponent> todo, Pair<Set<SComponent>, Set<SComponent>> decided) {
        if (todo.isEmpty()) {
            List<Pair<Set<SComponent>, Set<SComponent>>> ret = new ArrayList<>();
            if (!decided.getRight().isEmpty() && !decided.getLeft().isEmpty()) {
                ret.add(decided);
            }
            return ret;
        } else {
            Set<SComponent> newTodo = new HashSet<>(todo);
            SComponent comp = todo.iterator().next();
            newTodo.remove(comp);
            Set<SComponent> withLeft = new HashSet<>(decided.getLeft());
            Set<SComponent> withRight = new HashSet<>(decided.getRight());
            Set<SComponent> withoutLeft = new HashSet<>(decided.getLeft());
            Set<SComponent> withoutRight = new HashSet<>(decided.getRight());
            withLeft.add(comp);
            withoutRight.add(comp);
            Pair<Set<SComponent>, Set<SComponent>> newDecidedWith = new Pair<>(withLeft, withRight);
            Pair<Set<SComponent>, Set<SComponent>> newDecidedWithout = new Pair<>(withoutLeft, withoutRight);
            List<Pair<Set<SComponent>, Set<SComponent>>> ret = getAllNonemptyComponentDistributionsRecursive(newTodo, newDecidedWith);
            ret.addAll(getAllNonemptyComponentDistributionsRecursive(newTodo, newDecidedWithout));
            return ret;
        }
    }
    
    @Override
    public boolean supportsBottomUpQueries() {
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean supportsTopDownQueries() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }
    
}
