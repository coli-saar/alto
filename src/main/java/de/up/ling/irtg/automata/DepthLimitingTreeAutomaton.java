package de.up.ling.irtg.automata;

import com.google.common.collect.Iterables;
import de.saar.basic.Pair;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;

import de.up.ling.tree.Tree;
import java.util.ArrayList;

public class DepthLimitingTreeAutomaton<State> extends TreeAutomaton<Pair<State, Integer>> {

    private final TreeAutomaton<State> subTA;
    private final Interner<State> subInterner;
    private final Signature subSignature;
    public final int maxDepth;
    
    public DepthLimitingTreeAutomaton(TreeAutomaton<State> subTA, int maxDepth) {
        super(subTA.signature);
        this.subTA = subTA;
        this.subSignature = subTA.signature;
        this.subInterner = subTA.stateInterner;
        this.maxDepth = maxDepth;
        for (int state: subTA.getFinalStates()) {
            // final states all have a depth of 0
            addFinalState(stateInterner.addObject(new Pair<>(subInterner.resolveId(state), 0)));
        }
    }
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        return null;
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        Pair<State, Integer> state = stateInterner.resolveId(parentState);
        int currentDepth = stateInterner.resolveId(parentState).right;
        if (currentDepth == maxDepth) {
            return new ArrayList<>();
        }
        int subParentState = subInterner.resolveObject(state.left);
        var subIterable = subTA.getRulesTopDown(labelId, subParentState);
        return Iterables.transform(subIterable, (r) -> {
            ArrayList<Pair<State, Integer>> children = new ArrayList<>();
            int[] subChildren = r.getChildren();
            int[] childrenList = new int[subChildren.length];
            for (int i = 0; i < subChildren.length; i++) {
                var childState = new Pair<>(subTA.getStateForId(subChildren[i]), currentDepth+1);
                childrenList[i] = stateInterner.addObject(childState);
            }
            return createRule(parentState, labelId, childrenList, r.getWeight());
        });
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }
}
