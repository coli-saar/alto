/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.index.BinaryBottomUpRuleIndex;
import de.up.ling.irtg.automata.index.MapTopDownIndex;
import de.up.ling.irtg.automata.index.RuleStore;
import de.up.ling.irtg.codec.BolinasGraphOutputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.siblingfinder.SiblingFinder;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * A bottom-up decomposition automaton for the s-graph algebra, using
 * {@code BoundaryRepresentation}s as states.
 * 
 * @author groschwitz
 */
public class SGraphBRDecompositionAutomatonBottomUp extends TreeAutomaton<BoundaryRepresentation> {
//    private final BottomUpRuleIndex storedRules;    
    final GraphInfo graphInfo;    
    private final GraphAlgebra algebra;
    

    /**
     * Initializes a decomposition automaton for {@code completeGraph} with respect to {@code algebra}.
     */
    public SGraphBRDecompositionAutomatonBottomUp(SGraph completeGraph, GraphAlgebra algebra) {
        super(algebra.getSignature());
        
        this.algebra = algebra;

        graphInfo = new GraphInfo(completeGraph, algebra);
        
        ruleStore = new RuleStore(this, new MapTopDownIndex(this), new BinaryBottomUpRuleIndex(this));
        
//        storedRules = new BinaryBottomUpRuleIndex();
        
        
        
        
        Long2IntMap edgeIDMap = new Long2IntOpenHashMap();
        edgeIDMap.defaultReturnValue(-1);
        
        
    }

    private Rule makeRule(BoundaryRepresentation parent, int labelId, int[] childStates) {
        int parentState = addState(parent);
        
        // add final state if needed
        if (parent.isCompleteGraph()) {
                finalStates.add(parentState);
        }
        
        return createRule(parentState, labelId, childStates, 1);
    }


    
    

    private static <E> Collection<E> sing(E object) {
        return Collections.singletonList(object);
    }

    private Collection<Rule> sing(BoundaryRepresentation parent, int labelId, int[] childStates) {
//        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }


    

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Iterable<Rule> cachedResult = ruleStore.getRulesBottomUpRaw(labelId, childStates);
        
        if( cachedResult != null ) {
            switch (signature.getArity(labelId)) {
                case 0: AverageLogger.increaseValue("constants recognised"); break;
                case 1: AverageLogger.increaseValue("unaries recognised"); break;
                case 2: AverageLogger.increaseValue("merges recognised"); break;
            }
            return cachedResult;
        }
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
            AverageLogger.increaseValue("MergeRulesChecked");
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }
            if (!children.get(0).isMergeable(children.get(1))) { // ensure result is connected
                AverageLogger.increaseValue("MergeFail");
                return Collections.EMPTY_LIST;
            } else {
                BoundaryRepresentation result = children.get(0).merge(children.get(1));

                if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                    return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //System.err.println(result.toString());
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return cacheRules(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            }
        } else if (label.startsWith(GraphAlgebra.OP_COMBINEDMERGE)) {
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }
            AverageLogger.increaseValue("CombinedMergeRulesChecked");
            String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_COMBINEDMERGE.length());

            BoundaryRepresentation tempResult = children.get(1).applyForgetRename(renameLabel, signature.getIdForSymbol(renameLabel), true);
            if (tempResult == null) {
                AverageLogger.increaseValue("m1RenameFail");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            }

            if (!children.get(0).isMergeable(tempResult)) { // ensure result is connected
                AverageLogger.increaseValue("m1MergeFail");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                BoundaryRepresentation result = children.get(0).merge(tempResult);

                if (result == null) {
                    System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                    return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return cacheRules(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            }




        } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                || label.startsWith(GraphAlgebra.OP_SWAP)
                || label.startsWith(GraphAlgebra.OP_FORGET)
                /*|| label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)*/) {
            //ParseTester.averageLogger.increaseValue("UnaryRulesChecked");
            /*if (label.startsWith(GraphAlgebra.OP_RENAME)) {
                ParseTester.averageLogger.increaseValue("RenameRulesChecked");
            } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
                ParseTester.averageLogger.increaseValue("SwapRulesChecked");
            } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
                ParseTester.averageLogger.increaseValue("ForgetRulesChecked");
            }*/
            BoundaryRepresentation arg = children.get(0);

            for (Integer sourceToForget : arg.getForgottenSources(label, labelId))//check if we may forget.
            {
                if (!arg.isForgetAllowed(sourceToForget)) {
                    return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;//
                }
            }

            // now we can apply the operation.
            BoundaryRepresentation result = arg.applyForgetRename(label, labelId, true);// maybe do the above check in here? might be more efficient.

            if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                //result.setEqualsMeansIsomorphy(false);//is this a problem??
                /*if (label.startsWith(GraphAlgebra.OP_RENAME)) {
                    ParseTester.averageLogger.increaseValue("successfull renames");
                } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
                   ParseTester.averageLogger.increaseValue("successfull swaps");
                } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
                   ParseTester.averageLogger.increaseValue("successfull forgets");
                }*/
                return cacheRules(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
            }
        } else {
            //ParseTester.averageLogger.increaseValue("ConstantRulesChecked");
            List<Rule> rules = new ArrayList<>();
            SGraph sgraph = algebra.getAllConstantLabelInterpretations().get(labelId);//IsiAmrParser.parse(new StringReader(label));

            if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//return Collections.EMPTY_LIST;
            }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraphInfo.graph);
            graphInfo.getSGraph().foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                    //ParseTester.averageLogger.increaseValue("Constants found");
                    matchedSubgraph.setEqualsMeansIsomorphy(false);
                    rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, graphInfo), labelId, childStates));
                } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                }
            });

            return cacheRules(rules, labelId, childStates);//return rules;
        }
    }

    /**
     * This caches rules for future reference, if the same bottom-up
     * question is asked again.
     */
    protected Collection<Rule> cacheRules(Collection<Rule> rules, int labelID, int[] children) {
//        System.err.println("cache: " + rules.size() + " " + Util.mapToList(rules, rule -> rule.toString(this)));
        
        
        // Jonas' original implementation -- replaced by AK
//        System.err.println("cache: " + Util.mapToList(rules, rule -> rule.toString(this)));
        return ruleStore.setRules(rules, labelID, children);
    }
    

    private boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = graphInfo.getSGraph().getNode(nodename);

                if (!graphInfo.getSGraph().getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + graphInfo.getSGraph());
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : graphInfo.getSGraph().getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : graphInfo.getSGraph().getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
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
    
    
    
    private Boolean algebraIsPure;
    private int mergeLabelID;
    
    /**
     * Is true iff the merge operation is the only binary operation in the algebras signature.
     */
    private boolean isAlgebraPure() {
        if (algebraIsPure == null) {
            
            algebraIsPure = true;
            for (String label : signature.getSymbols()) {
                if (signature.getArityForLabel(label) == 2) {
                    if (label.equals(GraphAlgebra.OP_MERGE)) {
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
     */
    int getMergeLabelID() {
        if (algebraIsPure == null) {
            
            algebraIsPure = true;
            for (String label : signature.getSymbols()) {
                if (signature.getArityForLabel(label) == 2) {
                    if (label.equals(GraphAlgebra.OP_MERGE)) {
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
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return ruleStore.getRulesTopDown(labelId, parentState);
    }
    
    
    
    @Override
    public SiblingFinder newSiblingFinder(int labelID) {
        if (labelID == this.getMergeLabelID()) {
            return new MergePartnerFinder();
        } else {
            return super.newSiblingFinder(labelID);
        }
    }
    
    @Override
    public boolean useSiblingFinder() {
        return true;
    }
    
    
    SinglesideMergePartnerFinder makeNewSinglesideMergePartnerFinder() {
        if (isAlgebraPure()) {
            return new DynamicMergePartnerFinder(0 , this.graphInfo.getNrSources(), this.graphInfo.getNrNodes(), this); //To change body of generated methods, choose Tools | Templates.
        } else {
            System.err.println("WARNING: impure algebra found, falling back on SetPartnerFinder (default)");
            return new StorageMPF(this);
            //return new ImpureMPFBinaryPartnerFinder(this);
        }
    }
    
    private class MergePartnerFinder extends SiblingFinder {
        private final SinglesideMergePartnerFinder bpfLeft;
        private final SinglesideMergePartnerFinder bpfRight;

        public MergePartnerFinder() {
            super(2);
            bpfLeft = makeNewSinglesideMergePartnerFinder();
            bpfRight = makeNewSinglesideMergePartnerFinder();
        }
        



        @Override
        public String toString() {
            return "\n"+bpfLeft.toString()+"\n"+bpfRight.toString()+"\n";
        }

        @Override
        public List<int[]> getPartners(int stateID, int pos) {
            List<int[]> ret = new ArrayList<>();
            if (pos == 0) {
                for (int partner : bpfRight.getAllMergePartners(stateID)) {
                    ret.add(new int[]{stateID, partner});
                }
            } else {
                for (int partner : bpfLeft.getAllMergePartners(stateID)) {
                    ret.add(new int[]{partner, stateID});
                }
            }
            return ret;
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            if (pos == 0) {
                bpfLeft.insert(stateID);
            } else if (pos == 1) {
                bpfRight.insert(stateID);
            } else {
                System.err.println("Error: tried to at a state at position "+pos+" to the arity-2 merge partner finder");
            }
        }
    }
    
    //original MergePartnerFinder
    interface SinglesideMergePartnerFinder {
        /**
         * stores the graph state represented by the int, for future reference
         */
        void insert(int graph);

        /**
         * returns all graph states that are potential merge partners for the parameter graph.
         */
        IntList getAllMergePartners(int graph);

        /**
         * prints all stored graphs, and the structure how they are stored, via System.out
         */
        void print(String prefix, int indent);

    }
    
    private static class DynamicMergePartnerFinder implements SinglesideMergePartnerFinder {

        private final IntSet vertices;
        private final SinglesideMergePartnerFinder[] children;
        private final int sourceNr;
        private final int sourcesRemaining;
        private final SGraphBRDecompositionAutomatonBottomUp auto;
        private final int botIndex = 0;//the index for the children if the source is not assigned

        public DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto)//maybe give expected size of finalSet as parameter?
        {
            this.auto = auto;

            this.vertices = new IntOpenHashSet();

            sourceNr = currentSource;
            children = new SinglesideMergePartnerFinder[nrNodes+1];
            sourcesRemaining = nrSources;

        }

        private DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto, IntSet vertices)//maybe give expected size of finalSet as parameter?
        {
            this.auto = auto;

            this.vertices = vertices;

            sourceNr = currentSource;
            children = new SinglesideMergePartnerFinder[nrNodes+1];
            sourcesRemaining = nrSources;

        }

        @Override
        public void insert(int rep) {

            int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
            insertInto(vNr, rep);//if source is not assigned, vNr is -1.
        }

        private void insertInto(int vNr, int rep) {
            int index = vNr+1;//if source is not assigned, then index=0=botIndex.
            if (children[index] == null) {
                IntSet newVertices = new IntOpenHashSet();
                newVertices.addAll(vertices);
                if (vNr!= -1) {
                    newVertices.add(vNr);
                }
                if (sourcesRemaining == 1) {
                    //children[index] = new StorageMPF(auto);
                    children[index] = new EdgeMPF(newVertices, auto);
                } else {
                    children[index] = new DynamicMergePartnerFinder(sourceNr + 1, sourcesRemaining - 1, children.length-1, auto, newVertices);
                }
            }

            children[index].insert(rep);
        }

        @Override
        public IntList getAllMergePartners(int rep) {
            int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
            int index = vNr+1;
            IntList ret = new IntArrayList();//list is fine, since the two lists we get bottom up are disjoint anyway.


            if (vNr != -1) {
                if (children[index] != null) {
                    ret.addAll(children[index].getAllMergePartners(rep));
                }
                if (children[botIndex] != null){
                    ret.addAll(children[botIndex].getAllMergePartners(rep));
                }
            } else {
                for (SinglesideMergePartnerFinder child : children) {
                    if (child != null) {
                        ret.addAll(child.getAllMergePartners(rep));
                    }
                }
            }

            return ret;
        }

        @Override
        public void print(String prefix, int indent) {
            int indentSpaces = 5;
            StringBuilder indenter = new StringBuilder();
            for (int i = 0; i < indent * indentSpaces; i++) {
                indenter.append(" ");
            }
            System.out.println(indenter.toString() + prefix + "S" + String.valueOf(sourceNr) + "(#V="+vertices.size()+")"+":");
            for (int i = 0; i < indentSpaces; i++) {
                indenter.append(" ");
            }
            for (int i = 0; i < children.length; i++) {
                String newPrefix = "V" + String.valueOf(i) + ": ";

                if (children[i] != null) {
                    children[i].print(newPrefix, indent + 1);
                } else {
                    System.out.println(indenter.toString() + newPrefix + "--");
                }
            }
        }
    }
    
    private static class EdgeMPF implements SinglesideMergePartnerFinder{
        private final int[] local2GlobalEdgeIDs;
        private final Int2IntMap global2LocalEdgeIDs;//maybe better just use global ids and an int to object map for children?
        private final int currentIndex;
        private final SGraphBRDecompositionAutomatonBottomUp auto;
        private final SinglesideMergePartnerFinder[] children;
        private final boolean[] childIsEdgeMPF;
        private final StorageMPF storeHere;
        private final int parentEdge;

        /**
         * Top level constructor.
         */
        public EdgeMPF(IntSet vertices, SGraphBRDecompositionAutomatonBottomUp auto) {
            currentIndex = -1;
            local2GlobalEdgeIDs = auto.graphInfo.getAllIncidentEdges(vertices);
            Arrays.sort(local2GlobalEdgeIDs);
            global2LocalEdgeIDs = new Int2IntOpenHashMap();
            for (int i = 0; i<local2GlobalEdgeIDs.length; i++) {
                global2LocalEdgeIDs.put(local2GlobalEdgeIDs[i], i);
            }
            this.auto = auto;
            children = new SinglesideMergePartnerFinder[local2GlobalEdgeIDs.length];
            childIsEdgeMPF = new boolean[local2GlobalEdgeIDs.length];
            parentEdge = -1;
            storeHere = new StorageMPF(auto);
        }
        
        /**
         * Nested constructor.
         * @param local2Global
         * @param global2Local
         */
        private EdgeMPF(int[] local2Global, Int2IntMap global2Local, int currentIndex, SGraphBRDecompositionAutomatonBottomUp auto, int parentEdge) {
            local2GlobalEdgeIDs = local2Global;
            global2LocalEdgeIDs = global2Local;
            this.currentIndex = currentIndex;
            this.auto = auto;
            children = new SinglesideMergePartnerFinder[local2GlobalEdgeIDs.length - currentIndex];
            childIsEdgeMPF = new boolean[local2GlobalEdgeIDs.length - currentIndex];
            this.parentEdge = parentEdge;
            storeHere = new StorageMPF(auto);
        }




        @Override
        public void insert(int rep) {
            BoundaryRepresentation bRep = auto.getStateForId(rep);

            int nextEdgeIndex; // index of the next relevant edge in bRep.getSortedInBEdges()
            if (parentEdge == -1) {
                nextEdgeIndex = 0;
            } else {
                nextEdgeIndex = bRep.getSortedInBEdges().indexOf(parentEdge)+1;
            }


            if (nextEdgeIndex >= bRep.getSortedInBEdges().size()) {
                storeHere.insert(rep);//there are no more relevant edges!
            } else {
                int nextEdge = bRep.getSortedInBEdges().getInt(nextEdgeIndex);//get the next relevant edge
                int localEdgeID = global2LocalEdgeIDs.get(nextEdge);//find the local ID for it
                int childIndex = localEdgeID-currentIndex-1;

                SinglesideMergePartnerFinder targetChild = children[childIndex];
                if (targetChild == null) {
                    if (currentIndex == local2GlobalEdgeIDs.length - 1) {
                        //in this case we are at the end of the line, and can use direct lookup instead of nested one.
                        childIsEdgeMPF[childIndex]=false;
                        targetChild = new StorageMPF(auto);
                    } else {
                        //here we need nested lookup. 
                        targetChild = new EdgeMPF(local2GlobalEdgeIDs, global2LocalEdgeIDs, localEdgeID, auto, nextEdge);
                        childIsEdgeMPF[childIndex]=true;
                    }
                    children[childIndex] = targetChild;
                }


                targetChild.insert(rep);
            }
        }

        @Override
        public IntList getAllMergePartners(int rep) {
            IntList ret = new IntArrayList();
            IntList repNonEdgesLocal = new IntArrayList();
            BoundaryRepresentation bRep = auto.getStateForId(rep);
            for (int localID = 0; localID<local2GlobalEdgeIDs.length; localID++) {
                if (!bRep.getInBoundaryEdges().contains(local2GlobalEdgeIDs[localID])) {
                    repNonEdgesLocal.add(localID);
                }
            }
            ret.addAll(storeHere.getAllMergePartners(rep));
            int listIndex = 0;
            for (int i : repNonEdgesLocal) {
                if (children[i] != null) {
                    if (childIsEdgeMPF[i]) {
                        ret.addAll(((EdgeMPF)children[i]).getAllMergePartners(rep, repNonEdgesLocal.subList(listIndex+1, repNonEdgesLocal.size())));
                    } else {
                        ret.addAll(children[i].getAllMergePartners(rep));
                    }
                }
                listIndex++;
            }
            /*auto.getStateForId(rep).getInBoundaryEdges().forEach(edgeID -> {
                if (children[edgeID] != null) {
                    ret.addAll(children[edgeID].getAllMergePartners(rep));
                }
            });*/
            return ret;
        }

        private IntList getAllMergePartners(int rep, IntList remainingRepNonEdgesLocal) {//does not quite work, since children are general MergePartnerFinder
            IntList ret = new IntArrayList();
            ret.addAll(storeHere.getAllMergePartners(rep));
            int listIndex = 0;
            for (int i : remainingRepNonEdgesLocal) {
                int childID = i-currentIndex-1;
                if (children[childID]!= null) {
                    if (childIsEdgeMPF[childID]) {
                        ret.addAll(((EdgeMPF)children[childID]).getAllMergePartners(rep, remainingRepNonEdgesLocal.subList(listIndex+1, remainingRepNonEdgesLocal.size())));
                    } else {
                        ret.addAll(children[childID].getAllMergePartners(rep));
                    }
                }
                listIndex++;
            }
            return ret;
        }

        @Override
        public void print(String prefix, int indent) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    private static class StorageMPF implements SinglesideMergePartnerFinder{
        private final IntList finalSet;//list is fine, since every subgraph gets sorted in at most once.
        private final SGraphBRDecompositionAutomatonBottomUp auto;

        public StorageMPF(SGraphBRDecompositionAutomatonBottomUp auto){
            finalSet = new IntArrayList();
            this.auto = auto;
        }

        @Override
        public void insert(int rep) {
            finalSet.add(rep);
        }

        @Override
        public IntList getAllMergePartners(int rep) {
            return finalSet;
        }

        @Override
        public void print(String prefix, int indent) {
            int indentSpaces= 5;
            StringBuilder indenter = new StringBuilder();
            for (int i= 0; i<indent*indentSpaces; i++){
                indenter.append(" ");
            }
            StringBuilder content = new StringBuilder();
            for (int i : finalSet)
            {
                //content.append(String.valueOf(i)+",");
                content.append(auto.getStateForId(i).toString()+",");
            }
            System.out.println(indenter.toString()+prefix+content);
        }

    }
    
    
    
    
    
    
    
    //this part is only for writing
    private boolean actuallyWrite=true;
    private final int flushThreshold = 10000;
    private int count;
    private final BitSet foundByR = new BitSet();
    private final BitSet foundByO = new BitSet();
    
    //R=rename, O=other than rename
    //for merge (need 2):
    private final Int2ObjectMap<IntSet> ORight2OLeft = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<IntSet> RRight2OLeft = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Int2ObjectMap<String>> needOR = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Int2ObjectMap<String>> needOO = new Int2ObjectOpenHashMap<>();
    
    //need only one (from r, f, or merge):
    private final Int2ObjectMap<Set<String>> needR = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Set<String>> needO = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<String> rule2Parent = new Object2IntOpenHashMap<>();//if the int is negative, the state comes from a rename rule.
    
    
    private void foundO(Writer writer, int state) throws Exception {
        
        foundByO.set(state);
        
        Int2ObjectMap<String> needFurtherO = needOO.get(state);
        if (needFurtherO != null) {
            for (int otherO : needFurtherO.keySet()) {
                String rule = needFurtherO.get(otherO);
                putStringInSetByInt(otherO, rule, needO);
            }
            needOO.remove(state);
            for (IntSet set : ORight2OLeft.values()) {
                set.remove(state);
            }
        }
        
        IntSet leftOs = ORight2OLeft.get(state);
        if (leftOs != null) {
            for (int leftO : leftOs) {
                Int2ObjectMap<String> needFurtherOLeft = needOO.get(leftO);
                if (needFurtherOLeft != null) {
                    String rule = needFurtherOLeft.get(state);
                    putStringInSetByInt(leftO, rule, needO);
                    needFurtherOLeft.remove(state);
                    if (needFurtherOLeft.isEmpty()) {
                        needOO.remove(leftO);
                    }
                }
            }
            ORight2OLeft.remove(state);
        }
        
        Int2ObjectMap<String> needFurtherR = needOR.get(state);
        if (needFurtherR != null) {
            for (int otherR : needFurtherR.keySet()) {
                String rule = needFurtherR.get(otherR);
                putStringInSetByInt(otherR, rule, needR);
            }
            needOR.remove(state);
            for (IntSet set : RRight2OLeft.values()) {
                set.remove(state);
            }
        }
        
        
        Set<String> rules = needO.get(state);
        if (rules != null) {
            for (String rule : rules) {
                writer.write(rule);
                int recState = rule2Parent.getInt(rule);
                
                if (recState >= 0) {
                    if (!foundByO.get(recState)) {
                        foundO(writer, recState);
                    }
                } else {
                    if (!foundByR.get(-recState)) {
                        foundR(writer, -recState);
                    }
                }
                rule2Parent.removeInt(rule);
            }
            needO.remove(state);
        }
    }
    
    private void foundR(Writer writer, int state) throws Exception {
        
        foundByR.set(state);
        
        IntSet leftOs = RRight2OLeft.get(state);
        if (leftOs != null) {
            for (int leftO : leftOs) {
                Int2ObjectMap<String> needFurtherOLeft = needOR.get(leftO);
                if (needFurtherOLeft != null) {
                    String rule = needFurtherOLeft.get(state);
                    putStringInSetByInt(leftO, rule, needO);
                    needFurtherOLeft.remove(state);
                    if (needFurtherOLeft.isEmpty()) {
                        needOR.remove(leftO);
                    }
                }
            }
            RRight2OLeft.remove(state);
        }
        
        
        Set<String> rules = needR.get(state);
        if (rules != null) {
            for (String rule : rules) {
                writer.write(rule);
                int recState = rule2Parent.getInt(rule);
                
                if (recState >= 0) {
                    if (!foundByO.get(recState)) {
                        foundO(writer, recState);
                    }
                } else {
                    if (!foundByR.get(-recState)) {
                        foundR(writer, -recState);
                    }
                }
                rule2Parent.removeInt(rule);
            }
            needR.remove(state);
        }
    }
    
    private static void putStringInSetByInt(int i, String s, Int2ObjectMap<Set<String>> setMap) {
        Set<String> set = setMap.get(i);
        if (set == null) {
            set = new HashSet<>();
            setMap.put(i, set);
        }
        set.add(s);
    }
    
    private static void putIntInSetByInt(int i, int value, Int2ObjectMap<IntSet> setMap) {
        IntSet set = setMap.get(i);
        if (set == null) {
            set = new IntOpenHashSet();
            setMap.put(i, set);
        }
        set.add(value);
    }
    
    private static void putStringInDepht2IntTrie(int l, int r, String s, Int2ObjectMap<Int2ObjectMap<String>> depth2Trie, Int2ObjectMap<IntSet> second2FirstIndex) {
        Int2ObjectMap<String> r2Value = depth2Trie.get(l);
        if (r2Value == null) {
            r2Value = new Int2ObjectOpenHashMap<>();
            depth2Trie.put(l, r2Value);
        }
        r2Value.put(r, s);
        putIntInSetByInt(r, l, second2FirstIndex);
    }

    /**
     * Writes all rules in this decomposition automaton, with one restriction:
     * There are no rename operations on states only reachable via rename.
     * Rules of the form {@literal c-> m(a, b)} and {@literal c-> m(b,a)} are both written into the
     * writer (this is different from previous implementations).
     */
    public boolean writeAutomatonRestricted(Writer writer) {
        count = 0;

        boolean tempDoStore = isStoring();
        setStoring(false);
        
        boolean ret = processAllRulesBottomUp(rule -> {
            if (actuallyWrite) {
                try {
                    writeRule(writer, rule);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        
        setStoring(tempDoStore);
        
        return ret;
    }
    
    @Override
    public String toString() {
        return "";
    }
    
    private void writeRule(Writer writer, Rule rule) throws Exception {

        
        boolean found = false;
        Object2IntMap<String> rule2ParentLocal = new Object2IntOpenHashMap<>();
        
        
        int parent = rule.getParent();
        int arity = rule.getArity();
        
        String ruleString = getRuleString(rule, false);
        
        switch(arity) {
            case 0:
                writer.write(ruleString);
                found = true;
                break;
            case 1:
                int child = rule.getChildren()[0];
                if (foundByO.get(child)) {
                    writer.write(ruleString);
                    found = true;
                } else {
                    if (rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME)) {
                        rule2ParentLocal.put(ruleString, -parent);//the minus to store that this was rename
                    } else {
                        rule2ParentLocal.put(ruleString, parent);
                    }
                    putStringInSetByInt(child, ruleString, needO);
                }
                break;
            case 2:
                int child1 = rule.getChildren()[0];
                int child2 = rule.getChildren()[1];
                String ruleStringR = getRuleString(rule, true);
                //Rule ruleSwapped = createRule(rule.getParent(), rule.getLabel(), new int[]{child2, child1}, 1);
                //String ruleStringRswapped = getRuleString(ruleSwapped, true);
                if (foundByO.get(child1)) {
                    if (foundByO.get(child2)) {
                        writer.write(ruleString);
                        found = true;
                    } else {
                        putStringInSetByInt(child2, ruleString, needO);
                        rule2ParentLocal.put(ruleString, parent);
                    }
                    if (foundByR.get(child2)) {
                        writer.write(ruleStringR);
                        found = true;
                    } else {
                        putStringInSetByInt(child2, ruleStringR, needR);
                        rule2ParentLocal.put(ruleStringR, parent);
                    }
                } else {
                    if (foundByO.get(child2)) {
                        putStringInSetByInt(child1, ruleString, needO);
                        rule2ParentLocal.put(ruleString, parent);
                    } else {
                        putStringInDepht2IntTrie(child1, child2, ruleString, needOO, ORight2OLeft);
                        rule2ParentLocal.put(ruleString, parent);
                    }
                    if (foundByR.get(child2)) {
                        putStringInSetByInt(child1, ruleStringR, needO);
                        rule2ParentLocal.put(ruleStringR, parent);
                    } else {
                        putStringInDepht2IntTrie(child1, child2, ruleStringR, needOR, RRight2OLeft);
                        rule2ParentLocal.put(ruleStringR, parent);
                    }
                }
        }
        
            
            if (found) {
                if (rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME)) {
                    foundR(writer, parent);
                } else {
                    foundO(writer, parent);
                }
            } else {
                // FIXME: Is this recursive structure intended?
                rule2Parent.putAll(rule2Parent);
            }

            count++;
            if (count % flushThreshold == 0) {
                writer.flush();
            }
            
    }
    
    private String getRuleString(Rule rule, boolean useRenameInMerge) {
        String renameStatePrefix = "0R";
        String parentPrefix = rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME) ? renameStatePrefix : "";

        StringBuilder sb = new StringBuilder(Tree.encodeLabel(parentPrefix + encodeShort(rule.getParent())) + (finalStates.contains(rule.getParent()) ? "!" : "") + " -> " + Tree.encodeLabel(rule.getLabel(this)));

        boolean first = true;
        if (rule.getChildren().length > 0) {
            sb.append("(");

            for (int child : rule.getChildren()) {
                String childStateString;
                if (first) {
                    first = false;
                    childStateString = (child == 0) ? "null" : Tree.encodeLabel(encodeShort(child));
                } else {
                    childStateString = (child == 0) ? "null" : Tree.encodeLabel((useRenameInMerge ? renameStatePrefix : "") + encodeShort(child));
                    sb.append(", ");
                }

                sb.append(childStateString);
            }

            sb.append(")");
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    private String encodeShort(int stateId){
        return String.valueOf(stateId)+"_"+getStateForId(stateId).allSourcesToString();
    }

    
    
    
    
    
    
    
    /**
     * Writes the decomposition automata for all specified graphs in the corpus,
     * with one restriction: There are no rename operations on states only
     * reachable via rename. Rules of the form {@literal c-> m(a, b)} and {@literal c-> m(b,a)} are
     * both written into the writer (this is different from previous
     * implementations).
     * Output files are formatted CORPUSID_NODECOUNTnodes.rtg
     * @param targetFolderPath The folder into which the automata are written.
     * @param corpus The corpus to parse. The graph interpretation must be
     * labeled "graph".
     * @param startIndex At which index to start (graphs are ordered by node count)
     * @param stopIndex At which index to stop (graphs are ordered by node count)
     * @param sourceCount How many sources to use in decomposition.
     * @param maxNodes A hard cap on the number of nodes allowed per graph
     * (violating graphs are not parsed). 0 for no restriction.
     * @param maxPerNodeCount If for a node count n this is less then the number
     * of graphs with n nodes, then {@code maxPerNodeCount} many are chosen
     * randomly.
     * @param onlyBolinas Whether only graphs expressible in the Bolinas format
     * should be examined.
     * @throws Exception
     */
    public static void writeDecompositionAutomata(String targetFolderPath, Corpus corpus, int startIndex, int stopIndex, int sourceCount, int maxNodes, int maxPerNodeCount, boolean onlyBolinas) throws Exception {
        
        
        BolinasGraphOutputCodec bolCodec = new BolinasGraphOutputCodec();
        List<Instance>[] graphsToParse = new List[maxNodes];
        
        int id = 1;
        for (Instance instance : corpus) {
            instance.setComments("id", String.valueOf(id));
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bolCodec.write(graph, stream);
            if (!(onlyBolinas && stream.toString().startsWith("()\n")) && graph.getAllNodeNames().size()<=maxNodes) {
                GraphAlgebra alg = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, sourceCount);
                
                Writer rtgWriter = new StringWriter();
                SGraphBRDecompositionAutomatonBottomUp botupAuto = new SGraphBRDecompositionAutomatonBottomUp(graph, alg);
                botupAuto.actuallyWrite = false;
                boolean foundFinalState = botupAuto.writeAutomatonRestricted(rtgWriter);
                rtgWriter.close();
                if (foundFinalState) {
                    int n = graph.getAllNodeNames().size();
                    if (graphsToParse[n-1]==null) {
                        graphsToParse[n-1]=new ArrayList<>();
                    }
                    graphsToParse[n-1].add(instance);
                }
            }
            id++;
        }
        
        //add the instances we want to parse to our custom corpus.
        Corpus corpusToParse = new Corpus();
        Random r = new Random();
        for (int i = 0; i<maxNodes; i++) {
            if (graphsToParse[i] != null) {
                System.out.println("Adding " +Math.min(graphsToParse[i].size(), maxPerNodeCount) + " graphs with " + (i+1) + " nodes.");
                if (graphsToParse[i].size()<=maxPerNodeCount) {
                    for (Instance target : graphsToParse[i]) {
                        corpusToParse.addInstance(target);
                    }
                } else {
                    for (int k = 0; k<maxPerNodeCount; k++) {
                        int targetIndex = r.nextInt(graphsToParse[i].size());
                        Instance target = graphsToParse[i].remove(targetIndex);

                        corpusToParse.addInstance(target);
                    }
                }
            }
        }
        System.out.println("Chosen IDs:");
        corpusToParse.forEach(instance-> System.out.println(instance.getComments().get("id")));
        System.out.println("---------------------");
        
        corpusToParse.sort(Comparator.comparingInt(instance -> ((SGraph)instance.getInputObjects().get("graph")).getAllNodeNames().size()
        ));
        
        
        IntList successfull = new IntArrayList();
        
        Iterator<Instance> it = corpusToParse.iterator();
        int i = 0;
        int instancesTried = 0;
        while (it.hasNext()) {
            Instance instance = it.next();
            if (i>= startIndex && i < stopIndex) {
                instancesTried++;
                System.out.println(i);
                SGraph graph = (SGraph)instance.getInputObjects().get("graph");
                int instanceID = Integer.valueOf(instance.getComments().get("id"));
                GraphAlgebra alg = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, sourceCount);
                Writer rtgWriter = new FileWriter(targetFolderPath+String.valueOf(instanceID)+"_"+graph.getAllNodeNames().size()+"nodes"+".rtg");
                SGraphBRDecompositionAutomatonBottomUp botupAuto = new SGraphBRDecompositionAutomatonBottomUp(graph, alg);
                boolean foundFinalState = botupAuto.writeAutomatonRestricted(rtgWriter);
                rtgWriter.close();
                if (foundFinalState) {
                    successfull.add(instanceID);
                }
            }
            i++;
        }
        System.out.println(String.valueOf(successfull.size()) + " graphs were parsed successfully, out of " + String.valueOf(instancesTried));
        
        
    }
    
    
    
    
    
    
    
    
    /**
     * Writes the decomposition automata for a given corpus.
     * Calls {@code writeDecompositionAutomata} with the given arguments.
     * Call without arguments to receive help.
     * Takes eight arguments:
     * 1. The path to the the corpus. It should have "graph" and "string"
     * interpretations.
     * 2. How many sources to use in decomposition.
     * 3. At which index to start (inclusive, graphs are ordered by node count)
     * 4. At which index to stop (exclusive, graphs are ordered by node count)
     * Here indices are 0-based.
     * 5. A hard cap on the number of nodes allowed per graph
     * (violating graphs are not parsed). 0 for no restriction.
     * 6. A parameter k. If for a node count n, k is less then the number
     * of graphs with n nodes, then k are chosen randomly.
     * 7. targetFolderPath: The folder into which the automata are written.
     * 8. 'onlyBolinas' or 'all', depending on whether only graphs expressible
     * in the Bolinas format should be examined.
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        if (args.length<5) {
            System.out.println("Need eight arguments: corpusPath sourceCount startIndex stopIndex maxNodes maxPerNodeCount targetFolderPath 'onlyBolinas'/'all'");
            return;
        }
        
        String corpusPath = args[0];
        int sourceCount = Integer.valueOf(args[1]);
        int start = Integer.valueOf(args[2]);
        int stop = Integer.valueOf(args[3]);
        int maxNodes = Integer.valueOf(args[4]);
        if (maxNodes == 0) {
            maxNodes = 256;//this is arbitrarily chosen.
        }
        int maxPerNodeCount = Integer.valueOf(args[5]);
        if (maxPerNodeCount == 0) {
            maxPerNodeCount = Integer.MAX_VALUE;
        }
        String targetPath = args[6];
        boolean onlyBolinas = args.length>=8 && args[7].equals("onlyBolinas");
        
        Reader corpusReader = new FileReader(corpusPath);
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(null);
        Interpretation graphInt = new Interpretation(new GraphAlgebra(), null, "graph");
        Interpretation stringInt = new Interpretation(new StringAlgebra(), null, "graph");
        irtg.addInterpretation(graphInt);
        irtg.addInterpretation(stringInt);
        Corpus corpus = Corpus.readCorpus(corpusReader, irtg);
        writeDecompositionAutomata(targetPath, corpus, start, stop, sourceCount, maxNodes, maxPerNodeCount, onlyBolinas);
    }
    
    

    
}
