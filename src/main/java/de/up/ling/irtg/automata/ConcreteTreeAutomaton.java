/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * An automaton which can be constructed by explicitly adding rules, and which
 * stores these rules explicitly in memory.
 * 
 * The main added functionality of the automaton is given by the addRule and
 * addFinalState methods.
 * 
 * @author koller
 * @param <State>
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    // see below
    // private long numRules = 0;
    
    /**
     * Constructs a new instance without any final states of rules.
     * 
     * This creates a new signature for the automaton.
     */
    public ConcreteTreeAutomaton() {
        this(new Signature());
    }

    /**
     * Constructs a new instance without any final states or rules.
     * 
     * This instance will use the given signature.
     * 
     * @param signature 
     */
    public ConcreteTreeAutomaton(Signature signature) {
        super(signature);
        ruleStore.setExplicit(true);
    }
    
    /**
     * Constructs a new instance without any final states or rules, which
     * will use the given state interner to enumerate its states.
     * 
     * The signature used will be the one given.
     * 
     * Only use this if you know what you're doing.
     * 
     * @param signature
     * @param interner 
     */
    public ConcreteTreeAutomaton(Signature signature, Interner<State> interner) {
        super(signature, interner);
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
    
    /**
     * Adds the rule to the automaton.
     * 
     * The rule should be constructed with one of the createRule methods of
     * the automaton to ensure that it is interpreted correctly.
     * 
     * @param rule 
     */
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
