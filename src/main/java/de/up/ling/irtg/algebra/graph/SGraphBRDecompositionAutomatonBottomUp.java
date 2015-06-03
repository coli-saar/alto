/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.automata.BinaryPartnerFinder;
import de.up.ling.irtg.algebra.graph.BRUtil;
import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_MERGE;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.ParseTester;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BolinasGraphOutputCodec;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        } else if (label.startsWith(GraphAlgebra.OP_MERGE)) {
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }
            ParseTester.averageLogger.increaseValue("CombinedMergeRulesChecked");
            String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_MERGE.length()+1);

            BoundaryRepresentation tempResult = children.get(1).applyForgetRename(renameLabel, signature.getIdForSymbol(renameLabel), true);
            if (tempResult == null) {
                ParseTester.averageLogger.increaseValue("m1RenameFail");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            }

            if (!children.get(0).isMergeable(tempResult)) { // ensure result is connected
                ParseTester.averageLogger.increaseValue("m1MergeFail");
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
                if (label.startsWith(GraphAlgebra.OP_RENAME)) {
                    //ParseTester.averageLogger.increaseValue("successfull renames");
                } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
                   // ParseTester.averageLogger.increaseValue("successfull swaps");
                } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
                   // ParseTester.averageLogger.increaseValue("successfull forgets");
                }
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

            return cacheRules(rules, labelId, childStates);//return rules;
        }
    }

    
    protected Iterable<Rule> cacheRules(Iterable<Rule> rules, int labelID, int[] children) {
        Iterable<Rule> ret = rules;
        if (doStore) {
            ret = storedRules.put(rules, labelID, children);
        }
        return ret;
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
                rule2Parent.remove(rule);
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
                rule2Parent.remove(rule);
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

    public boolean writeAutomatonRestricted(Writer writer) throws Exception {
        stateInterner.setTrustingMode(true);
        count = 0;

        boolean tempDoStore = isDoStore();
        setDoStore(false);
        
        boolean ret = processAllRulesBottomUp(rule -> {
            if (actuallyWrite) {
                try {
                    writeRule(writer, rule);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        
        setDoStore(tempDoStore);
        
        return ret;
    }
    
    public void writeRule(Writer writer, Rule rule) throws Exception {

        
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
                /*if (foundByO.get(child2)) {
                    if (foundByR.get(child1)) {
                        writer.write(ruleStringRswapped);
                    } else {
                        putStringInSetByInt(child1, ruleStringRswapped, needR);
                    }
                } else {
                    if (foundByR.get(child1)) {
                        putStringInSetByInt(child2, ruleStringRswapped, needO);
                    } else {
                        putStringInDepht2IntTrie(child2, child1, ruleStringRswapped, needOR, RRight2OLeft);
                    }                /*if (foundByO.get(child2)) {
                    if (foundByR.get(child1)) {
                        writer.write(ruleStringRswapped);
                    } else {
                        putStringInSetByInt(child1, ruleStringRswapped, needR);
                    }
                } else {
                    if (foundByR.get(child1)) {
                        putStringInSetByInt(child2, ruleStringRswapped, needO);
                    } else {
                        putStringInDepht2IntTrie(child2, child1, ruleStringRswapped, needOR, RRight2OLeft);
                    }
                }*/
        }
        
            
            if (found) {
                if (rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME)) {
                    foundR(writer, parent);
                } else {
                    foundO(writer, parent);
                }
            } else {
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

    
    public static void writeDecompositionAutomata(String targetFolderPath, IrtgInducer inducer, int startIndex, int stopIndex, int sourceCount, int maxNodes, int maxPerNodeCount, boolean onlyBolinas) throws Exception {
        
        
        BolinasGraphOutputCodec bolCodec = new BolinasGraphOutputCodec();
        IntList[] graphsToParse = new IntList[maxNodes];
        
        for (int i = 0; i<inducer.getCorpus().size(); i++) {
            IrtgInducer.TrainingInstance instance = inducer.getCorpus().get(i);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bolCodec.write(instance.graph, stream);
            if (!(onlyBolinas && stream.toString().startsWith("()\n")) && instance.graph.getAllNodeNames().size()<=maxNodes) {
                GraphAlgebra alg = new GraphAlgebra();
                BRUtil.makeIncompleteDecompositionAlgebra(alg, instance.graph, sourceCount);
                
                Writer rtgWriter = new StringWriter();
                SGraphBRDecompositionAutomatonBottomUp botupAuto = new SGraphBRDecompositionAutomatonBottomUp(instance.graph, alg, alg.getSignature());
                botupAuto.actuallyWrite = false;
                boolean foundFinalState = botupAuto.writeAutomatonRestricted(rtgWriter);
                rtgWriter.close();
                if (foundFinalState) {
                    int n = instance.graph.getAllNodeNames().size();
                    if (graphsToParse[n-1]==null) {
                        graphsToParse[n-1]=new IntArrayList();
                    }
                    graphsToParse[n-1].add(i);
                }
            }
        }
        
        //add the instances we want to parse to our custom corpus.
        List<IrtgInducer.TrainingInstance> corpus = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i<maxNodes; i++) {
            if (graphsToParse[i] != null) {
                System.out.println("Adding " +Math.min(graphsToParse[i].size(), maxPerNodeCount) + " graphs with " + (i+1) + " nodes.");
                if (graphsToParse[i].size()<=maxPerNodeCount) {
                    for (int target : graphsToParse[i]) {
                        corpus.add(inducer.getCorpus().get(target));
                    }
                } else {
                    for (int k = 0; k<maxPerNodeCount; k++) {
                        int targetIndex = r.nextInt(graphsToParse[i].size());
                        int target = graphsToParse[i].remove(targetIndex);

                        corpus.add(inducer.getCorpus().get(target));
                    }
                }
            }
        }
        System.out.println("Chosen IDs:");
        corpus.forEach(instance-> System.out.println(instance.id));
        System.out.println("---------------------");
        
        corpus.sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()
        ));
        
        
        IntList successfull = new IntArrayList();
        
        int stop = Math.min(corpus.size(), stopIndex);
        
        for (int i = startIndex; i<stop; i++) {
            System.out.println(i);
            IrtgInducer.TrainingInstance instance = corpus.get(i);
            GraphAlgebra alg = new GraphAlgebra();
            BRUtil.makeIncompleteDecompositionAlgebra(alg, instance.graph, sourceCount);
            Writer rtgWriter = new FileWriter(targetFolderPath+String.valueOf(instance.id)+"_"+instance.graph.getAllNodeNames().size()+"nodes"+".rtg");
            SGraphBRDecompositionAutomatonBottomUp botupAuto = new SGraphBRDecompositionAutomatonBottomUp(instance.graph, alg, alg.getSignature());
            boolean foundFinalState = botupAuto.writeAutomatonRestricted(rtgWriter);
            rtgWriter.close();
            if (foundFinalState) {
                successfull.add(instance.id);
            }
            
                
            
            //System.out.println(String.valueOf(auto.ruleCount));
        }
        System.out.println(String.valueOf(successfull.size()) + " graphs were parsed successfully, out of " + String.valueOf(stop-startIndex));
        
        //resWriter.close();
        
    }
    
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
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        writeDecompositionAutomata(targetPath, inducer, start, stop, sourceCount, maxNodes, maxPerNodeCount, onlyBolinas);
    }



    
}
