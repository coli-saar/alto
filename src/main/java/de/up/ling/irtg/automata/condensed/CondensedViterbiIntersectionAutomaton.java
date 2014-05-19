/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2DoubleMap;
import de.up.ling.irtg.util.ArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;


/*
 TODO: 
 - collectStatePairs: remove CartesianIterator, use IntCartesianIterator to avoid boxing
 */
/**
 *
 * @author koller
 */
public class CondensedViterbiIntersectionAutomaton<LeftState, RightState> extends GenericCondensedIntersectionAutomaton<LeftState, RightState> {

    private final Int2DoubleMap viterbiStateMap;        ///< Maps a state from this automaton to a probability
    private final Int2ObjectMap<Rule> viterbiRuleMap;   ///< Maps a state to its best rule

    public CondensedViterbiIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left, right, sigMapper);

        viterbiStateMap = new ArrayInt2DoubleMap();
        viterbiStateMap.defaultReturnValue(0.0); // if a state is not in this map, return 0

        viterbiRuleMap = new ArrayMap<>();
    }

    @Override
    protected void collectOutputRule(Rule outputRule) {
        // Check, if this state has been seen before
        int newState = outputRule.getParent();

        int[] children = outputRule.getChildren();
        double childWeight = outputRule.getWeight();

        // multiply the weight of all childs of the rule
        for (int i = 0; i < children.length; i++) {
            childWeight *= viterbiStateMap.get(children[i]);
        }

        if (viterbiStateMap.get(newState) < childWeight) {
            // current rule is new, or better!
            viterbiRuleMap.put(newState, outputRule);
            viterbiStateMap.put(newState, childWeight);
        }
    }

    @Override
    protected void addAllOutputRules() {
        viterbiRuleMap.values().forEach(this::storeRule);
    }
    
    public static void main(String[] args) throws Exception {
        GenericCondensedIntersectionAutomaton.main(args, false, (left, right) -> left.intersectViterbi(right));
    }
}
