/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.automata.index.MapTopDownIndex;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.FastutilUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author koller
 * @param <State>
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    public ConcreteTreeAutomaton() {
        this(new Signature());
    }

    public ConcreteTreeAutomaton(Signature signature) {
        super(signature);
        isExplicit = true;
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
        storeRule(rule);
    }
    
    public void removeAllRules() {
        explicitRulesBottomUp = new IntTrie<Int2ObjectMap<Set<Rule>>>();

        explicitRulesTopDown = new MapTopDownIndex();

        unprocessedUpdatesForRulesForRhsState = new ArrayList<Rule>();
        unprocessedUpdatesForBottomUp = new ArrayList<>();
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
        getExplicitRulesBottomUp();
        return explicitIsBottomUpDeterministic;
    }

    @Override
    public void foreachRuleBottomUpForSets(final IntSet labelIds, List<IntSet> childStateSets, final SignatureMapper signatureMapper, final Consumer<Rule> fn) {
        getExplicitRulesBottomUp().foreachValueForKeySets(childStateSets, ruleMap -> {
            // TODO: This is optimized for the PCFG case, where the label sets are typically much
            // larger than the sets of rules with the same child states. Adapt IntTrie iteration/contains
            // trick here to iterate over smaller set. Take special care to remap in the right direction.

            FastutilUtils.forEach(ruleMap.keySet(), label -> {
                if (labelIds.contains(signatureMapper.remapForward(label))) {
                    ruleMap.get(label).forEach(fn);
                }
            });
        });
    }
}
