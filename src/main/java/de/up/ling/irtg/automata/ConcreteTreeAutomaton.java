/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {

    public ConcreteTreeAutomaton() {
        super(new Signature());
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

    @Override
    public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return explicitIsBottomUpDeterministic;
    }

    @Override
    public void foreachRuleBottomUpForSets(IntSet labelIds, List<Set<Integer>> childStateSets, int[] labelRemap, Function<Rule, Void> fn) {
        System.err.println("cta frbupfs");
        
        for (int label : labelIds) {
            int remapped = labelRemap[label];

            if (signature.getArity(remapped) == childStateSets.size()) {
                StateListToStateMap smap = explicitRulesBottomUp.get(remapped);

                if (smap != null) {
                    smap.foreachRuleForStateSets(childStateSets, fn);
                }
            }
        }
    }

    //  strange -- the following optimization should work, but doesn't.
//    @Override
//    public Collection<Integer> getLabelsTopDown(int parentState) {
//        IntSet ret = new IntOpenHashSet();
//        
//        for( int label : explicitRulesTopDown.keySet() ) {
//            if( explicitRulesTopDown.get(label).containsKey(parentState)) {
//                ret.add(label);
//            }
//        }
//        
//        return ret;
//    }
}
