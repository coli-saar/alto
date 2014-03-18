/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author johannes
 */
public class ConcreteCondensedTreeAutomaton<State> extends CondensedTreeAutomaton<State> {
    
    public ConcreteCondensedTreeAutomaton() {
        super(new Signature());
//        isExplicit = true;
    }
    
    /**
     * Creates a new CondensedTreeAutomaton based on the rules and final states of another TreeAutomaton.
     * Rules in the original TreeAutomaton, that have the same child states and the the same parent state, 
     * but differ in their label, will be merged to form a CondensedRule
     * @param origin
     */
    public static <E> ConcreteCondensedTreeAutomaton<E> fromTreeAutomaton(TreeAutomaton<E> origin) {
        ConcreteCondensedTreeAutomaton<E> ret = new ConcreteCondensedTreeAutomaton<E>();
        
        ret.signature = origin.getSignature();
        ret.stateInterner = origin.getStateInterner();
        
        ret.ruleTrie = new CondensedRuleTrie();
        ret.topDownRules = new Int2ObjectOpenHashMap<Int2ObjectMap<Set<CondensedRule>>>();
        ret.absorbTreeAutomaton(origin);
        
        System.err.println("condensed: " + ret.toStringCondensed());
        
        System.err.println("condensed as ordinary automaton: " + ret);
        
        assert ret.equals(origin);
        
        return ret;
    }
    
    @Override
    public int addState(State state) {
        return super.addState(state); 
    }
    
    @Override
    public void addFinalState(int state) {
        super.addFinalState(state); 
    }
        
    public void addRule(CondensedRule rule) {
        storeRule(rule);
    }
    
    @Override
    public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates) {
        return getCondensedRuleBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState) {
        return getCondensedRulesTopDownFromExplicit(labelId, parentState);
    }

    /**
     * Copies all rules from a TreeAutomaton to this automaton. Merges rules, that can be condensed.
     * @param auto
     */
    public void absorbTreeAutomaton(TreeAutomaton<State> auto) {
        for (Rule rule : auto.getRuleSet()) {
            System.err.println("absorb rule: " + rule.toString(auto));
            storeRule(rule, auto);
        }
    }
    
    /**
     * Creates a condensed rule based on a given rule and the automaton, that has created it.
     * The new rule will be stored in the internal data structures, see storeRule
     * @param rule
     * @param auto
     */
    private void storeRule(Rule rule, TreeAutomaton<State> auto) {
        // condense
        int[] newChildren = rule.getChildren(); // convertChildrenStates(rule.getChildren(), auto);
        int newParent = rule.getParent(); // addState(auto.getStateForId(rule.getParent()));
        int newLabel = rule.getLabel(); // signature.addSymbol(rule.getLabel(auto), newChildren.length);
        
        int newLabelSetID = -1;
        
        CondensedRuleTrie finalTrie = ruleTrie.getFinalTrie(newChildren);
        Int2ObjectMap<Set<CondensedRule>> ruleMap = finalTrie.getLabelSetIDToRulesMap();
        
        // maintain invariant: for each children list and parent, there is only one label set which connects them
        // on the other hand, may have to search for parent in more than just the first rule
        
        // iterate over all labelSetIDs for the current children.
        for (int possibleLabelSetID : ruleMap.keySet()) {
            // if the parentstate for the current labelSetID matches, we found or labelSet
            if (finalTrie.getParents(possibleLabelSetID).contains(newParent)) {
                newLabelSetID = possibleLabelSetID;
            }
        }
        
        // In case there is no existing entry for the childs and parent state of the given rule,
        // we have to create one.
        if (newLabelSetID == -1) {
            IntSet newLabels = new IntOpenHashSet();
            newLabels.add(newLabel);
            newLabelSetID = labelSetInterner.addObject(newLabels);
        }
        
        CondensedRule newRule = createRuleRaw(newParent, newLabelSetID, newChildren, rule.getWeight());
        
        newRule.setExtra(rule.getExtra());

        storeRule(newRule);
        
        // Absorb final states
        if (auto.getFinalStates().contains(rule.getParent())) {
            finalStates.add(newParent);
        }
        System.err.println("created Rule: " + newRule.toString(this));

//            System.err.println("OUT: " + newRule.toString(this));
    }

    @Override
    public void makeAllRulesCondensedExplicit() {
        isCondensedExplicit = true; 
    }
    
}
