/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.HashSet;
import java.util.Set;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;



/**
 * A recursively defined Trie for CondensedRules
 * @author koller, modified by gontrum to use primitive int as keys.
 * @param <V>
 */
public class CondensedRuleTrie {
    private final Int2ObjectMap<CondensedRuleTrie> map;
    private final Int2ObjectMap<Set<CondensedRule>> labelSetIDToRules;
    private final Int2ObjectMap<IntSet> labelSetIDToParentStateSet;

    public CondensedRuleTrie() {
        map = new Int2ObjectOpenHashMap<CondensedRuleTrie>();
        labelSetIDToRules = new Int2ObjectOpenHashMap<Set<CondensedRule>>();
        labelSetIDToParentStateSet = new Int2ObjectOpenHashMap<IntSet>();
    }

    /**
     * Stores a sequence of ints (the Array) in the Trie 
     * and maps the final state to the given rule.
     * @param childstates
     * @param rule 
     */
    public void put(int[] childstates, int labelSetID, CondensedRule rule) {
        put(childstates, labelSetID, rule, 0);
    }
    
    /**
     * Recursive version of put.
     * Go as deep as the length of the given array.
     * @param childstates
     * @param rule
     * @param index 
     */
    private void put(int[] childstates, int labelSetID, CondensedRule rule, int index) {
        if( index == childstates.length) {
            if (labelSetIDToRules.containsKey(labelSetID)) {
                IntSet statesHere = labelSetIDToParentStateSet.get(labelSetID);
                if (statesHere == null) {
                    statesHere = new IntOpenHashSet();
                    labelSetIDToParentStateSet.put(labelSetID, statesHere);
                }
                statesHere.add(rule.getParent());
                labelSetIDToRules.get(labelSetID).add(rule);
            } else {
                Set<CondensedRule> internalSet = new HashSet<CondensedRule>();
                internalSet.add(rule);
                labelSetIDToRules.put(labelSetID, internalSet);
            }
        } else {
            int keyHere = childstates[index];
            CondensedRuleTrie nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                nextTrie = new CondensedRuleTrie();
                map.put(keyHere, nextTrie);
            }
            
            nextTrie.put(childstates, labelSetID, rule, index+1);
        }
    }
    
    /**
     * Returns a set of values, that is mapped to the final state
     * we reach with the sequence of transitions in childstates.
     * @param childstates
     * @return 
     */
    public Set<CondensedRule> get(int[] childstates, int labelSetID) {
        return get(childstates, labelSetID, 0);
    }
    
    private Set<CondensedRule> get(int[] childstates, int labelSetID, int index) {
        if( index == childstates.length) {
            if (labelSetIDToRules.containsKey(labelSetID)) {
                return labelSetIDToRules.get(labelSetID);
            } else return new HashSet<CondensedRule>();
        } else {
            int keyHere = childstates[index];
            CondensedRuleTrie nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                return new HashSet<CondensedRule>();
            } else {
                return nextTrie.get(childstates, labelSetID, index+1);
            }
        }
    }
    
    
    /**
     * Returns the Trie at the final state of the given childstates.
     * @param childstates
     * @return
     */
    protected CondensedRuleTrie getFinalTrie(int [] childstates) {
        return getFinalTrie(childstates, 0); 
    }
    
    private CondensedRuleTrie getFinalTrie(int[] childstates, int index) {
        if (index == childstates.length) {
            return this;
        } else {
            int keyHere = childstates[index];
            CondensedRuleTrie nextTrie = map.get(keyHere);
            if (nextTrie == null) {
                return new CondensedRuleTrie();
            } else {
                return nextTrie.getFinalTrie(childstates, index + 1);
            }
        }
    }
    
    /**
     * Returns the subtrie that we reach with a transition with the given
     * symbol.
     *
     * @param id
     * @return
     */
    public CondensedRuleTrie getSubtrie(int id) {
        return map.get(id);
    }
    
    public IntSet getParents(int labelSetID) {
        return labelSetIDToParentStateSet.get(labelSetID);
    }
    
    protected Int2ObjectMap<Set<CondensedRule>> getLabelSetIDToRulesMap() {
        return labelSetIDToRules;
    }
    
    
    public IntSet getStoredKeys() {
        return labelSetIDToRules.keySet();
    }
    
    /**
     * Returns an IntSet with the symbols for all outgoing transitions.
     * @return 
     */
    public IntSet getBranches() {
        return map.keySet();
    }
    
}
