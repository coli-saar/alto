/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.graph.AMDecompositionAutomaton;
import de.up.ling.irtg.algebra.graph.AMSignatureBuilder;
import static de.up.ling.irtg.algebra.graph.AMSignatureBuilder.DOMAIN;
import static de.up.ling.irtg.algebra.graph.AMSignatureBuilder.MOD;
import static de.up.ling.irtg.algebra.graph.AMSignatureBuilder.OBJ;
import static de.up.ling.irtg.algebra.graph.AMSignatureBuilder.POSS;
import static de.up.ling.irtg.algebra.graph.AMSignatureBuilder.SUBJ;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra;
import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.index.BinaryBottomUpRuleIndex;
import de.up.ling.irtg.automata.index.MapTopDownIndex;
import de.up.ling.irtg.automata.index.RuleStore;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.siblingfinder.SiblingFinder;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.ParseException;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 *
 * @author jonas
 */
public class AlignmentTrackingAutomaton extends TreeAutomaton<Pair<Pair<BoundaryRepresentation, AMDecompositionAutomaton.Type>, Integer>> {

    public static final String SEPARATOR = "__@@__";
    
    private final AMDecompositionAutomaton decomp;
    private final Map<Integer, Set<String>> index2nns;
    private final Int2IntMap state2decompstate;
    private final Int2IntMap state2head;
    private final Map<Integer, Double> scoreConst;
    
    AlignmentTrackingAutomaton(ApplyModifyGraphAlgebra alg, Signature signature, SGraph graph, Map<Integer, Set<String>> index2nns,
            Function<SGraph, Double> scoreConst) {
        super(signature);
        decomp = new AMDecompositionAutomaton(alg, null, graph);
        this.index2nns = index2nns;
        state2decompstate = new Int2IntOpenHashMap();
        state2head = new Int2IntOpenHashMap();
        ruleStore = new RuleStore(this, new MapTopDownIndex(this), new BinaryBottomUpRuleIndex(this));
        IsiAmrInputCodec codec = new IsiAmrInputCodec();
        this.scoreConst = new HashMap<>();
        for (int id = 1; id <= alg.getSignature().getMaxSymbolId(); id++) {
            if (alg.getSignature().getArity(id) == 0) {
                SGraph c = codec.read(alg.getSignature().resolveSymbolId(id).split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)[0]);
//                System.err.println(alg.getSignature().resolveSymbolId(id));
//                System.err.println(c.toIsiAmrStringWithSources());
//                System.err.println(scoreConst.apply(c));
                this.scoreConst.put(id, scoreConst.apply(c));
            }
        }
    }
    
    AlignmentTrackingAutomaton(ApplyModifyGraphAlgebra alg, Signature signature, SGraph graph, Map<Integer, Set<String>> index2nns) {
        this(alg, signature, graph, index2nns, (g -> 1.0));
    }

    @Override
    public Iterable getRulesBottomUp(int labelId, int[] childStates) {
        String label = signature.resolveSymbolId(labelId);
        String[] labelParts = label.split(SEPARATOR);
        switch (childStates.length) {
            case 0:
            {
                List<Rule> ret = new ArrayList<>();
                int index = Integer.valueOf(labelParts[0]);
                for (Rule rule : decomp.getRulesBottomUp(decomp.getSignature().getIdForSymbol(labelParts[1]), new int[0])) {
                    BoundaryRepresentation resHere = decomp.getStateForId(rule.getParent()).left;
                    
                    //check whether the resulting graph fragment matches the actual nodes mentioned in the alignment -- we make a simple test with the root here, should be enough besides pathological cases
                    if (index2nns.get(index).contains(resHere.getGraph().getNodeForSource("root"))) {
                        ret.add(createRule(makeState(decomp.getStateForId(rule.getParent()), index), labelId, childStates, scoreConst.get(decomp.getSignature().getIdForSymbol(labelParts[1]))));
                    }
                }
                return cacheRules(ret, labelId, childStates);
            }
            case 2:
            {
                String[] heads = label.split("_");
                if (Integer.valueOf(heads[0]) != state2head.get(childStates[0]) || Integer.valueOf(heads[1]) != state2head.get(childStates[1])) {
                    return Collections.EMPTY_LIST;
                }
                List<Rule> ret = new ArrayList<>();
                int[] decompStates = new int[2];
                decompStates[0] = state2decompstate.get(childStates[0]);
                decompStates[1] = state2decompstate.get(childStates[1]);
                int head = labelParts[1].startsWith(ApplyModifyGraphAlgebra.OP_APPLICATION) ? state2head.get(childStates[0]) : state2head.get(childStates[0]);
                for (Rule rule : decomp.getRulesBottomUp(decomp.getSignature().getIdForSymbol(labelParts[1]), decompStates)) {
                    ret.add(createRule(makeState(decomp.getStateForId(rule.getParent()), head), labelId, childStates, 1.0));
                }
                return cacheRules(ret, labelId, childStates);
            }
            default:
                throw new UnsupportedOperationException("Not supported (can only deal with 0 or 2 children).");
        }
    }
    
    private int makeState(Pair<BoundaryRepresentation, AMDecompositionAutomaton.Type> left, int right) {
        int leftID = decomp.getIdForState(left);
        int stateID = addState(new Pair(left, right));
        state2decompstate.put(stateID, leftID);
        state2head.put(stateID, right);
        if (decomp.getFinalStates().contains(leftID)) {
            addFinalState(stateID);
        }
        return stateID;
    }

    @Override
    public Iterable getRulesTopDown(int labelId, int parentState) {
        return ruleStore.getRulesTopDown(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }
    
    /**
     * This caches rules for future reference, if the same bottom-up
     * question is asked again.
     * @param rules
     * @param labelID
     * @param children
     * @return 
     */
    protected Collection<Rule> cacheRules(Collection<Rule> rules, int labelID, int[] children) {
//        System.err.println("cache: " + rules.size() + " " + Util.mapToList(rules, rule -> rule.toString(this)));
        
        
        // Jonas' original implementation -- replaced by AK
//        System.err.println("cache: " + Util.mapToList(rules, rule -> rule.toString(this)));
        return ruleStore.setRules(rules, labelID, children);
    }

    @Override
    public boolean useSiblingFinder() {
        return true;
    }

    @Override
    public SiblingFinder newSiblingFinder(int labelID) {
        if (signature.getArity(labelID) == 2) {
            String[] parts = signature.resolveSymbolId(labelID).split(SEPARATOR);
            int leftHead = Integer.valueOf(parts[0].split("_")[0]);
            int rightHead = Integer.valueOf(parts[0].split("_")[1]);
            int decompLabelID = decomp.getSignature().getIdForSymbol(parts[1]);
            return new SF(leftHead, rightHead, decompLabelID);
        } else {
            return super.newSiblingFinder(labelID);
        }
    }
    
    
    private class SF extends SiblingFinder {

        private final int leftHead;
        private final int rightHead;
        private final SiblingFinder decompSF;
        //private final String decompLabel;//for debugging
        
        private SF(int leftHead, int rightHead, int decompLabelID) {
            super(2);
            this.leftHead = leftHead;
            this.rightHead = rightHead;
            decompSF = decomp.newSiblingFinder(decompLabelID);
            //decompLabel = decomp.getSignature().resolveSymbolId(decompLabelID);
        }
        
        @Override
        public Iterable<int[]> getPartners(int stateID, int pos) {
            if (headMatches(stateID, pos)) {
                //System.err.println("get "+state2decompstate.get(stateID) + " pos " + pos + " head "+getStateForId(stateID).right+" "+decompLabel+"["+leftHead+","+rightHead+"]");
                List<int[]> ret = new ArrayList<>();
                decompSF.getPartners(state2decompstate.get(stateID), pos).forEach(childPair -> {
                    int[] newPair = new int[2];
                    newPair[pos] = stateID;
                    newPair[(pos+1)%2] = getIdForState(new Pair(decomp.getStateForId(childPair[(pos+1)%2]), otherHead(pos)));
                    ret.add(newPair);
                });
                return ret;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            if (headMatches(stateID, pos)) {
                //System.err.println("add "+state2decompstate.get(stateID) + " pos " + pos + " head "+getStateForId(stateID).right+" "+decompLabel+"["+leftHead+","+rightHead+"]");
                decompSF.addState(state2decompstate.get(stateID), pos);
            }
        }
        
        private boolean headMatches(int stateID, int pos) {
            Pair<Pair<BoundaryRepresentation, AMDecompositionAutomaton.Type>, Integer> state = getStateForId(stateID);
            switch(pos) {
                case 0: return state.right == leftHead;
                case 1: return state.right == rightHead;
                default: throw new IllegalArgumentException();
            }
        }
        
        private int otherHead(int pos) {
            switch(pos) {
                case 0: return rightHead;
                case 1: return leftHead;
                default: throw new IllegalArgumentException();
            }
        }
        
    }
    
    
    
    public static AlignmentTrackingAutomaton create(SGraph graph, String[] alignments, int sentLength, boolean coref) throws IllegalArgumentException, ParseException {
        return create(graph, alignments, sentLength, coref, (g -> 1.0));
    }
    
    
    public static AlignmentTrackingAutomaton create(SGraph graph, String[] alignments, int sentLength, boolean coref,
            Function<SGraph, Double> scoreConst) throws IllegalArgumentException, ParseException {
        Signature sig = new Signature();
        Signature plainSig = new Signature();
        Map<Integer, Set<String>> index2nns = new HashMap();
        for (String alString : alignments) {
            Alignment al = Alignment.read(alString, 0);
            index2nns.put(al.span.start, al.nodes);
            Set<String> consts = AMSignatureBuilder.getConstantsForAlignment(al, graph, coref);
            consts.stream().forEach(c -> sig.addSymbol(al.span.start+SEPARATOR+c, 0));
            consts.stream().forEach(c -> plainSig.addSymbol(c, 0));
        }
        Set<String> sources = new HashSet<>();
        sources.add(SUBJ);
        sources.add(OBJ);
        sources.add(OBJ+2);
        sources.add(OBJ+3);
        sources.add(OBJ+4);
        sources.add(OBJ+5);
        sources.add(OBJ+6);
        sources.add(OBJ+7);
        sources.add(OBJ+8);
        sources.add(OBJ+9);
        sources.add(DOMAIN);
        sources.add(POSS);
        sources.add(MOD);
        sources.add(SUBJ);
        sources.add(OBJ);
        sources.add(OBJ+2);
        sources.add(OBJ+3);
        sources.add(OBJ+4);
        sources.add(OBJ+5);
        sources.add(OBJ+6);
        sources.add(OBJ+7);
        sources.add(OBJ+8);
        sources.add(OBJ+9);
        sources.add(DOMAIN);
        sources.add(POSS);
        sources.add(MOD);
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            String eSrc = AMSignatureBuilder.edge2Source(e, graph);
            if (eSrc.startsWith("op") || eSrc.startsWith("snt")) {
                sources.add(eSrc);
                sources.add(eSrc);
            }
        }
        
        for (String s : sources) {
            plainSig.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+s, 2);
            plainSig.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+s, 2);
            for (int i = 0; i<sentLength; i++) {
                for (int j = 0; j<sentLength; j++) {
                    if (i != j) {
                        sig.addSymbol(i+"_"+j+SEPARATOR+ApplyModifyGraphAlgebra.OP_APPLICATION+s, 2);
                        sig.addSymbol(i+"_"+j+SEPARATOR+ApplyModifyGraphAlgebra.OP_MODIFICATION+s, 2);
                    }
                }
            }
        }
        return new AlignmentTrackingAutomaton(new ApplyModifyGraphAlgebra(plainSig), sig, graph, index2nns, scoreConst);
    }
    
    
}
