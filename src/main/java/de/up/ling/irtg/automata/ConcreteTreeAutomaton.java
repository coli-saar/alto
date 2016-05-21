/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 * @param <State>
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    private long numRules = 0;
    
    public ConcreteTreeAutomaton() {
        this(new Signature());
    }

    public ConcreteTreeAutomaton(Signature signature) {
        super(signature);
        ruleStore.setExplicit(true);
    }

    @Override
    public int addState(State state) {
        return super.addState(state);
    }

    @Override
    public void addFinalState(int state) {
        super.addFinalState(state);
    }
    

    public void addRule(Rule rule) {
        storeRuleBottomUp(rule);
        storeRuleTopDown(rule);
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return ruleStore.isBottomUpDeterministic();
    }

    @Override
    public void foreachRuleBottomUpForSets(final IntSet labelIds, List<IntSet> childStateSets, final SignatureMapper signatureMapper, final Consumer<Rule> fn) {
        ruleStore.foreachRuleBottomUpForSets(labelIds, childStateSets, signatureMapper, fn);
    }

    // TODO - this doesn't work, because numRules overcounts
    // when the same rule is added twice. The clean solution
    // would be to read the number of rules from an index.
//    /**
//     * Returns the number of rules in this automaton.
//     * This method is fast for concrete automata, because
//     * they count rules as they are added.
//     * 
//     * @return 
//     */
//    @Override
//    public long getNumberOfRules() {
//        return numRules;
//    }
}
