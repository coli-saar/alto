/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.index.BinaryBottomUpRuleIndex;
import de.up.ling.irtg.automata.index.MapTopDownIndex;
import de.up.ling.irtg.automata.index.RuleStore;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.siblingfinder.SiblingFinder;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Just like SGraphBUDecompositionAutomatonBottomUp, but within a continuous string of
 * renames, this may not rename the same node twice.
 * @author Jonas
 */
public class SGraphBUDecompositionAutoRestrictedRenames extends TreeAutomaton<Pair<Integer, BitSet>> {

    private static final BitSet EMPTY_SET = new BitSet();
    private final SGraphBRDecompositionAutomatonBottomUp decomp;
    private final Int2IntMap state2decompState;
    private final Int2ObjectMap<IntSet> decompState2states;
    private final SGraph completeGraph;
    
    public SGraphBUDecompositionAutoRestrictedRenames(SGraph completeGraph, GraphAlgebra algebra) {
        super(algebra.getSignature());
        decomp = new SGraphBRDecompositionAutomatonBottomUp(completeGraph, algebra);
        state2decompState = new Int2IntOpenHashMap();
        decompState2states = new Int2ObjectOpenHashMap<>();
        ruleStore = new RuleStore(this, new MapTopDownIndex(this), new BinaryBottomUpRuleIndex(this));
        this.completeGraph = completeGraph;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        int[] decompChildren = new int[childStates.length];
        for (int i = 0; i<childStates.length; i++) {
            decompChildren[i] = state2decompState.get(childStates[i]);
        }
        String label = signature.resolveSymbolId(labelId);
        BitSet retBits = EMPTY_SET;
        if (label.startsWith(GraphAlgebra.OP_RENAME)) {
            String[] parts = label.split("_");
            int oldSource = decomp.graphInfo.getIntForSource(parts[1]);
            int newSource = decomp.graphInfo.getIntForSource(parts[2]);
            BitSet oldRenames = getStateForId(childStates[0]).right;
            if (oldRenames.get(oldSource)) {
                return ruleStore.setRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            retBits = new BitSet();
            retBits.or(oldRenames);
            retBits.set(newSource);
        }
        List<Rule> ret = new ArrayList<>();
        for (Rule rule : decomp.getRulesBottomUp(labelId, decompChildren)) {
            int decompParent = rule.getParent();
            int parent = addState(new Pair(decompParent, retBits));
            state2decompState.put(parent, decompParent);
            IntSet parentsHere = decompState2states.get(decompParent);
            if (parentsHere == null) {
                parentsHere = new IntOpenHashSet();
                decompState2states.put(decompParent, parentsHere);
            }
            parentsHere.add(parent);
            ret.add(createRule(parent, labelId, childStates, 1));
            SGraph graphHere = decomp.getStateForId(decompParent).getGraph();
            if (graphHere.equals(completeGraph)) {
//                boolean sources = true;
//                for (String source : completeGraph.getAllSources()) {
//                    String otherNode = graphHere.getNodeForSource(source);
//                    String node = completeGraph.getNodeForSource(source);
//                    if (!node.equals(otherNode)) {
//                        sources = false;
//                    }
//                }
//                if (sources) {
if (!finalStates.contains(parent)) {
                    finalStates.add(parent);
}
//                }
            }
        }
        return ruleStore.setRules(ret, labelId, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return ruleStore.getRulesTopDown(labelId, parentState);
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
    
    @Override
    public SiblingFinder newSiblingFinder(int labelID) {
        if (labelID == decomp.getMergeLabelID()) {
            return new MergePartnerFinder();
        } else {
            return super.newSiblingFinder(labelID);
        }
    }
    
    @Override
    public boolean useSiblingFinder() {
        return true;
    }
    
    protected class MergePartnerFinder extends SiblingFinder {
        private final SGraphBRDecompositionAutomatonBottomUp.SinglesideMergePartnerFinder bpfLeft;
        private final SGraphBRDecompositionAutomatonBottomUp.SinglesideMergePartnerFinder bpfRight;

        public MergePartnerFinder() {
            super(2);
            bpfLeft = decomp.makeNewSinglesideMergePartnerFinder();
            bpfRight = decomp.makeNewSinglesideMergePartnerFinder();
        }
        



        @Override
        public String toString() {
            return "\n"+bpfLeft.toString()+"\n"+bpfRight.toString()+"\n";
        }

        @Override
        public List<int[]> getPartners(int stateID, int pos) {
            List<int[]> ret = new ArrayList<>();
            if (pos == 0) {
                for (int partner : bpfRight.getAllMergePartners(state2decompState.get(stateID))) {
                    for (int partnerState : decompState2states.get(partner)) {
                        ret.add(new int[]{stateID, partnerState});
                    }
                }
            } else {
                for (int partner : bpfLeft.getAllMergePartners(state2decompState.get(stateID))) {
                    for (int partnerState : decompState2states.get(partner)) {
                        ret.add(new int[]{partnerState, stateID});
                    }
                }
            }
            return ret;
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            switch (pos) {
                case 0:
                    bpfLeft.insert(state2decompState.get(stateID));
                    break;
                case 1:
                    bpfRight.insert(state2decompState.get(stateID));
                    break;
                default:
                    System.err.println("Error: tried to at a state at position "+pos+" to the arity-2 merge partner finder");
                    break;
            }
        }
    }
    
    public static void main(String[] args) {
        //SGraph graph = new IsiAmrInputCodec().read("(l<0>/love :ARG0 (i/i) :ARG1 (r/rose))");
        SGraph graph = new IsiAmrInputCodec().read("(w<rt>/want :ARG0 (c/cat :mod (l/little)) :ARG1 (s/sleep :ARG0 c))");
        //GraphAlgebra alg2 = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, 2);
        Signature sig = new Signature();
        sig.addSymbol(GraphAlgebra.OP_MERGE, 2);
        Set<String> sources = new HashSet<>();
        sources.add("rt");
        sources.add("S");
        sources.add("O");
        sources.add("MOD");
        for (String s : sources) {
            sig.addSymbol(GraphAlgebra.OP_FORGET+s, 1);
            for (String s2 : sources) {
                if (!s.equals(s2)) {
                    sig.addSymbol(GraphAlgebra.OP_RENAME+s+"_"+s2, 1);
                }
            }
        }
        sig.addSymbol("(w<rt>/want :ARG0 (c<S>) :ARG1 (s<O>))", 0);
        sig.addSymbol("(s<rt>/sleep :ARG0 (c<S>))", 0);
        sig.addSymbol("(l<rt>/cat)", 0);
        sig.addSymbol("(c<MOD> :mod (l<rt>/little))", 0);
        GraphAlgebra alg2 = new GraphAlgebra(sig);
        SGraphBUDecompositionAutoRestrictedRenames decomp = new SGraphBUDecompositionAutoRestrictedRenames(graph, alg2);
        decomp.processAllRulesBottomUp(null);
        //System.err.println(decomp);
        System.err.println(alg2.getSignature());
        System.err.println(decomp.getFinalStates().stream().map(id -> {
            return decomp.decomp.getStateForId(decomp.state2decompState.get(id.intValue()));
        }).collect(Collectors.toSet()));
        System.err.println("treecount: " + decomp.countTrees());
    } 
    
}
