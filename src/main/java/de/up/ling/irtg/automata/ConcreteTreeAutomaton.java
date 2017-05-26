/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.siblingfinder.SiblingFinder;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
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

    @Override
    public SiblingFinder newSiblingFinder(int labelID) {
        if (signature.getArity(labelID) >=2) {
            if (allSfRuleIndices == null) {
                initSfRuleIndices();
            }
            return new ConcreteSiblingFinder(labelID, allSfRuleIndices[labelID]);
        } else {
            return super.newSiblingFinder(labelID);
        }
    }
    
    @Override
    public boolean useSiblingFinder() {
        return false;//TODO: test whether using sibling finder would be faster than current default method.
    }
    
    private List<Rule>[] allSfRuleIndices = null;
    
    private void initSfRuleIndices() {
        allSfRuleIndices = new List[signature.getMaxSymbolId()+1];
        for (int i = 0; i<allSfRuleIndices.length; i++) {
            allSfRuleIndices[i] = new ArrayList<>();
        }
        for (Rule rule : getRuleSet()) {
            if (signature.getArity(rule.getLabel())>=2) {
                allSfRuleIndices[rule.getLabel()].add(rule);
            }
        }
    }
    
    private class ConcreteSiblingFinder extends SiblingFinder {

        Int2ObjectMap<List<RuleWithMarkedChildren>>[] pos2state2rules;
        
        public ConcreteSiblingFinder(int labelID, List<Rule> rules) {
            super(signature.getArity(labelID));
            pos2state2rules = new Int2ObjectMap[signature.getArity(labelID)];
            for (int i = 0; i<pos2state2rules.length; i++) {
                pos2state2rules[i] = new Int2ObjectOpenHashMap<>();
            }
            rules.forEach((rule) -> {
                addInitRule(new RuleWithMarkedChildren(rule));
            });
        }
        
        private void addInitRule(RuleWithMarkedChildren rule) {
            int[] children = rule.getChildren();
            for (int i = 0; i<children.length; i++) {
                List<RuleWithMarkedChildren> list = pos2state2rules[i].get(children[i]);
                if (list == null) {
                    list = new ArrayList<>();
                    pos2state2rules[i].put(children[i], list);
                }
                list.add(rule);
            }
        }
        
        @Override
        public Iterable<int[]> getPartners(int stateID, int pos) {
            List<int[]> ret = new ArrayList<>();
            List<RuleWithMarkedChildren> list = pos2state2rules[pos].get(stateID);
            if (list != null) {
                list.stream().filter((rule) -> (rule.allOthersSeen(pos))).forEachOrdered((rule) -> {
                    ret.add(rule.getChildren());
                });
            }
            return ret;
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            List<RuleWithMarkedChildren> set = pos2state2rules[pos].get(stateID);
            if (set != null) {
                set.forEach((rule) -> {
                    rule.setSeen(pos);
                });
            }
        }
        
        
    }
    
    private static class RuleWithMarkedChildren {
        private final Rule rule;
        private final boolean[] seen;
        
        private RuleWithMarkedChildren(Rule rule) {
            this.rule = rule;
            this.seen = new boolean[rule.getArity()];//intitialized as false;
        }
        
        private void setSeen(int pos) {
            seen[pos] = true;
        }
        
        private boolean allOthersSeen(int pos) {
            boolean ret = true;
            for (int i = 0; i<seen.length; i++) {
                if (i != pos && !seen[i]) {
                    ret = false;
                }
            }
            return ret;
        }
        
        private int[] getChildren() {
            return rule.getChildren();
        }
        
    }
    
}
