/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.BinaryPartnerFinder;
import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_MERGE;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.ParseTester;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.mpf.DynamicMergePartnerFinder;
import de.up.ling.irtg.algebra.graph.mpf.MergePartnerFinder;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SGraphBRDecompositionAutomatonBottomUp extends TreeAutomaton<BoundaryRepresentation> {
    private final RuleCache storedRules;    
    public final GraphInfo completeGraphInfo;    
    final GraphAlgebra algebra;
    public Map<BoundaryRepresentation, Set<Rule>> rulesTopDown;
    public Map<String, Integer> decompLengths;
    public MergePartnerFinder startStateMPF;
    
    Long2ObjectMap<Long2IntMap> storedStates;

    public SGraphBRDecompositionAutomatonBottomUp(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);
        
        this.algebra = algebra;
        //getStateInterner().setTrustingMode(true);

        completeGraphInfo = new GraphInfo(completeGraph, algebra, signature);
        storedRules = new BinaryRuleCache();
        
        //storedRulesTopDown = new Int2ObjectOpenHashMap<>();
        
        
        
        stateInterner.setTrustingMode(true);
        storedStates = new Long2ObjectOpenHashMap<>();
        Long2IntMap edgeIDMap = new Long2IntOpenHashMap();
        edgeIDMap.defaultReturnValue(-1);
        
        startStateMPF = new DynamicMergePartnerFinder(0, completeGraphInfo.getNrSources(), completeGraphInfo.getNrNodes(), this);
        //BoundaryRepresentation completeRep = new BoundaryRepresentation(completeGraph, completeGraphInfo);
        //int x = addState(completeRep);
        //finalStates.add(x);
        
    }
    
    void preinitialize() {
        //override this in child instances
    }

    Rule makeRule(BoundaryRepresentation parent, int labelId, int[] childStates) {

        /*StringBuilder message = new StringBuilder();
         message.append(parent.toString(this)+" from " + signature.resolveSymbolId(labelId));
         for (int i = 0; i<childStates.length; i++){
         message.append(" __ "+getStateForId(childStates[i]).toString(this));
         }
         System.out.println(message);
         SGraph graph = parent.getGraph(completeGraph, this);
         System.out.println("sgraph: " + graph.toIsiAmrString());*/
        
        
        int parentState = addState(parent);
        
        // add final state if needed
        if (parent.isCompleteGraph()) {
                finalStates.add(parentState);
        }
        
        return createRule(parentState, labelId, childStates, 1);
    }

    
        @Override
    protected int addState(BoundaryRepresentation stateBR) {
        int stateID = -1;
        Long2IntMap edgeIDMap = storedStates.get(stateBR.vertexID);
        if (edgeIDMap != null){
            stateID = edgeIDMap.get(stateBR.edgeID);
        }
        
        if (stateID == -1){
            stateID = super.addState(stateBR);//this is kind of ugly?
            if (edgeIDMap == null){
                edgeIDMap = new Long2IntOpenHashMap();
                edgeIDMap.defaultReturnValue(-1);
                storedStates.put(stateBR.vertexID, edgeIDMap);
            }
            edgeIDMap.put(stateBR.edgeID, stateID);
        }
        return stateID;
    }
    

    static <E> Iterable<E> sing(E object) {
        return Collections.singletonList(object);
    }

    Iterable<Rule> sing(BoundaryRepresentation parent, int labelId, int[] childStates) {
//        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }

    public void makeTrusting() {
        this.stateInterner.setTrustingMode(true);
    }

    

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Iterable<Rule> cachedResult = storedRules.get(labelId, childStates);
        
        if( cachedResult != null ) {
            ParseTester.cachedAnswers++;
            switch (signature.getArity(labelId)) {
                case 0: ParseTester.averageLogger.increaseValue("constants recognised"); break;
                case 1: ParseTester.averageLogger.increaseValue("unaries recognised"); break;
                case 2: ParseTester.averageLogger.increaseValue("merges recognised"); break;
            }
            return cachedResult;
        }
        ParseTester.newAnswers++;
        //ParseTester.averageLogger.increaseValue("TotalRulesChecked");
        
        String label = signature.resolveSymbolId(labelId);
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        List<BoundaryRepresentation> children = new ArrayList<>();
        for (int i = 0; i < childStates.length; i++) {
            children.add(getStateForId(childStates[i]));
        }

        if (label == null) {
            return Collections.EMPTY_LIST;
        } else if (label.equals(GraphAlgebra.OP_MERGE)) {
            ParseTester.averageLogger.increaseValue("MergeRulesChecked");
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }
            if (!children.get(0).isMergeable(children.get(1))) { // ensure result is connected
                ParseTester.averageLogger.increaseValue("MergeFail");
                return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                BoundaryRepresentation result = children.get(0).merge(children.get(1));

                if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                    return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //System.err.println(result.toString());
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return storedRules.put(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            }
        } else if (label.startsWith(GraphAlgebra.OP_MERGE)) {
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }
            ParseTester.averageLogger.increaseValue("CombinedMergeRulesChecked");
            String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_MERGE.length()+1);

            BoundaryRepresentation tempResult = children.get(1).applyForgetRename(renameLabel, signature.getIdForSymbol(renameLabel), true);
            if (tempResult == null) {
                ParseTester.averageLogger.increaseValue("m1RenameFail");
                return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            }

            if (!children.get(0).isMergeable(tempResult)) { // ensure result is connected
                ParseTester.averageLogger.increaseValue("m1MergeFail");
                return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                BoundaryRepresentation result = children.get(0).merge(tempResult);

                if (result == null) {
                    System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                    return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return storedRules.put(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            }




        } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                || label.startsWith(GraphAlgebra.OP_SWAP)
                || label.startsWith(GraphAlgebra.OP_FORGET)
                || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {
            //ParseTester.averageLogger.increaseValue("UnaryRulesChecked");
            if (label.startsWith(GraphAlgebra.OP_RENAME)) {
                //ParseTester.averageLogger.increaseValue("RenameRulesChecked");
            } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
                //ParseTester.averageLogger.increaseValue("SwapRulesChecked");
            } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
               // ParseTester.averageLogger.increaseValue("ForgetRulesChecked");
            }
            BoundaryRepresentation arg = children.get(0);

            for (Integer sourceToForget : arg.getForgottenSources(label, labelId))//check if we may forget.
            {
                if (!arg.isForgetAllowed(sourceToForget, completeGraphInfo.getSGraph(), completeGraphInfo)) {
                    return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;//
                }
            }

            // now we can apply the operation.
            BoundaryRepresentation result = arg.applyForgetRename(label, labelId, true);// maybe do the above check in here? might be more efficient.

            if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                //result.setEqualsMeansIsomorphy(false);//is this a problem??
                if (label.startsWith(GraphAlgebra.OP_RENAME)) {
                    //ParseTester.averageLogger.increaseValue("successfull renames");
                } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
                   // ParseTester.averageLogger.increaseValue("successfull swaps");
                } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
                   // ParseTester.averageLogger.increaseValue("successfull forgets");
                }
                return storedRules.put(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
            }
        } else {
            //ParseTester.averageLogger.increaseValue("ConstantRulesChecked");
            List<Rule> rules = new ArrayList<>();
            SGraph sgraph = algebra.getAllConstantLabelInterpretations().get(labelId);//IsiAmrParser.parse(new StringReader(label));

            if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                return storedRules.put(Collections.EMPTY_LIST, labelId, childStates);//return Collections.EMPTY_LIST;
            }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraphInfo.graph);
            completeGraphInfo.getSGraph().foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                    //ParseTester.averageLogger.increaseValue("Constants found");
                    matchedSubgraph.setEqualsMeansIsomorphy(false);
                    rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, completeGraphInfo), labelId, childStates));
                } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                }
            });

            return storedRules.put(rules, labelId, childStates);//return rules;
        }
    }

    

    boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
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

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return getRulesTopDownFromExplicit(labelId, parentState);
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


    /*@Override
    public IntCollection getPartnersForPatternMatching(int stateID, int labelID) {
        if (signature.resolveSymbolId(labelID).equals(GraphAlgebra.OP_MERGE)) {
            return startStateMPF.getAllMergePartners(stateID);
        } else {
            return super.getPartnersForPatternMatching(stateID, labelID);
        }
        
    }
    
    private BitSet seenStatesForPatternMatching;
    
    @Override
    public void addStateForPatternMatching(int stateID) {
        if (seenStatesForPatternMatching == null) {
            seenStatesForPatternMatching = new BitSet();
        }
        if (!seenStatesForPatternMatching.get(stateID)) {
            seenStatesForPatternMatching.set(stateID);
            startStateMPF.insert(stateID);
        }
        super.addStateForPatternMatching(stateID); //To change body of generated methods, choose Tools | Templates.
    }*/
    
    

    boolean doBolinas(){
        return false;
    }
    
    private Boolean algebraIsPure;
    private int mergeLabelID;
    
    /**
     * Is true iff the merge operation is the only binary operation in the algebras signature.
     * @return 
     */
    private boolean isAlgebraPure() {
        if (algebraIsPure == null) {
            
            algebraIsPure = true;
            for (String label : signature.getSymbols()) {
                if (signature.getArityForLabel(label) == 2) {
                    if (label.equals(OP_MERGE)) {
                        mergeLabelID = signature.getIdForSymbol(label);
                    } else {
                        algebraIsPure = false;
                    }
                }
            }
        }
        return algebraIsPure;
    }
    
    /**
     * returns the ID of the merge label in the algebra.
     * @return 
     */
    private int getMergeLabelID() {
        if (algebraIsPure == null) {
            
            algebraIsPure = true;
            for (String label : signature.getSymbols()) {
                if (signature.getArityForLabel(label) == 2) {
                    if (label.equals(OP_MERGE)) {
                        mergeLabelID = signature.getIdForSymbol(label);
                    } else {
                        algebraIsPure = false;
                    }
                }
            }
        }
        return mergeLabelID;
    }
    
    @Override
    public BinaryPartnerFinder makeNewBinaryPartnerFinder(TreeAutomaton auto) {
        if (isAlgebraPure()) {
            return new MPFBinaryPartnerFinder((SGraphBRDecompositionAutomatonBottomUp)auto); //To change body of generated methods, choose Tools | Templates.
        } else {
            return new ImpureMPFBinaryPartnerFinder((SGraphBRDecompositionAutomatonBottomUp)auto);
        }
    }
    
    private class MPFBinaryPartnerFinder extends BinaryPartnerFinder{
        MergePartnerFinder mpf;
        BitSet seen = new BitSet();
        public MPFBinaryPartnerFinder(SGraphBRDecompositionAutomatonBottomUp auto) {
            mpf = new DynamicMergePartnerFinder(0 , auto.completeGraphInfo.getNrSources(), auto.completeGraphInfo.getNrNodes(), auto);
        }
        
        @Override
        public IntCollection getPartners(int labelID, int stateID) {
            if (labelID == getMergeLabelID()) {
                return mpf.getAllMergePartners(stateID);
            } else {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }

        @Override
        public void addState(int stateID) {
            if (!seen.get(stateID)) {
                mpf.insert(stateID);
                seen.set(stateID);
            }
        }
        
    }
    
    private class ImpureMPFBinaryPartnerFinder extends BinaryPartnerFinder{
        MergePartnerFinder mpf;
        IntSet backupset;
        public ImpureMPFBinaryPartnerFinder(SGraphBRDecompositionAutomatonBottomUp auto) {
            mpf = new DynamicMergePartnerFinder(0 , auto.completeGraphInfo.getNrSources(), auto.completeGraphInfo.getNrNodes(), auto);
            backupset = new IntOpenHashSet();
        }
        
        @Override
        public IntCollection getPartners(int labelID, int stateID) {
            if (signature.resolveSymbolId(labelID).equals(OP_MERGE)) {
                return mpf.getAllMergePartners(stateID);
            } else {
                return backupset;
            }
        }

        @Override
        public void addState(int stateID) {
            if (!backupset.contains(stateID)) {
                mpf.insert(stateID);
                backupset.add(stateID);
            }
        }
        
    }
    
}
