/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {

    private IntTrie<Int2ObjectMap<Set<Rule>>> ruleTrie = null;

    public ConcreteTreeAutomaton() {
        super(new Signature());
        isExplicit = true;
        ruleTrie = new IntTrie<Int2ObjectMap<Set<Rule>>>();
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
    public void foreachRuleBottomUpForSets(final IntSet labelIds, List<IntSet> childStateSets, final int[] labelRemap, final Function<Rule, Void> fn) {
//        System.err.println("cta frbupfs");
        ensureRuleTrie();

        final IntSet remappedLabelIds = new IntOpenHashSet();
        for (int labelId : labelIds) {
            remappedLabelIds.add(labelRemap[labelId]);
        }

        ruleTrie.foreachValueForKeySets(childStateSets, new Function<Int2ObjectMap<Set<Rule>>, Void>() {
            public Void apply(Int2ObjectMap<Set<Rule>> ruleMap) {
                for (int label : ruleMap.keySet()) {
                    if (remappedLabelIds.contains(label)) {
                        for (Rule rule : ruleMap.get(label)) {
                            fn.apply(rule);
                        }
                    }
                }

                return null;
            }
        });

//        for (int label : labelIds) {
//            int remapped = labelRemap[label];
//
//            if (signature.getArity(remapped) == childStateSets.size()) {
//                StateListToStateMap smap = explicitRulesBottomUp.get(remapped);
//
//                if (smap != null) {
//                    smap.foreachRuleForStateSets(childStateSets, fn);
//                }
//            }
//        }
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
    @Override
    protected void storeRule(Rule rule) {
        super.storeRule(rule); //To change body of generated methods, choose Tools | Templates.
        storeRuleInTrie(rule);
    }
    
    
    
    private void storeRuleInTrie(Rule rule) {
        Int2ObjectMap<Set<Rule>> knownRuleMap = ruleTrie.get(rule.getChildren());

        if (knownRuleMap == null) {
            knownRuleMap = new Int2ObjectOpenHashMap<Set<Rule>>();
            ruleTrie.put(rule.getChildren(), knownRuleMap);
        }

        Set<Rule> knownRules = knownRuleMap.get(rule.getLabel());

        if (knownRules == null) {
            knownRules = new HashSet<Rule>();
            knownRuleMap.put(rule.getLabel(), knownRules);
        }

        knownRules.add(rule);
    }

    private void ensureRuleTrie() {
        if (ruleTrie == null) {
            System.err.println("reindexing ...");

            long startTime = System.nanoTime();

            ruleTrie = new IntTrie<Int2ObjectMap<Set<Rule>>>();
            for (Rule rule : getRuleIterable()) {
                storeRuleInTrie(rule);
            }

            System.err.println("reindexing trie: " + (System.nanoTime() - startTime) / 1000000 + "ms");
        }
    }
}
