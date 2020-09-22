/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.HashSet;
import java.util.Set;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;



/**
 * A recursively defined Trie for CondensedRules
 * @author koller, modified by gontrum to use primitive int as keys.
 */
public class CondensedRuleTrie {
    private final Int2ObjectMap<CondensedRuleTrie> map;
    private final Int2ObjectMap<Set<CondensedRule>> labelSetIDToRules;
    private final Int2ObjectMap<IntSet> labelSetIDToParentStateSet;

    public CondensedRuleTrie() {
        map = new Int2ObjectOpenHashMap<>();
        labelSetIDToRules = new Int2ObjectOpenHashMap<>();
        labelSetIDToParentStateSet = new Int2ObjectOpenHashMap<>();
    }

    /**
     * Stores a sequence of ints (the Array) in the Trie 
     * and maps the final state to the given rule.
     */
    public void put(int[] childstates, int labelSetID, CondensedRule rule) {
        put(childstates, labelSetID, rule, 0);
        assert labelSetIDToParentStateSet.keySet().equals(labelSetIDToRules.keySet());
    }
    
    
    /**
     * Recursive version of put.
     * Go as deep as the length of the given array.
     */
    private void put(int[] childstates, int labelSetID, CondensedRule rule, int index) {
        if( index == childstates.length) {
            if (labelSetIDToParentStateSet.containsKey(labelSetID)) {
                labelSetIDToParentStateSet.get(labelSetID).add(rule.getParent());
            } else {
                IntSet statesHere = new IntOpenHashSet();
                statesHere.add(rule.getParent());
                labelSetIDToParentStateSet.put(labelSetID, statesHere);
            }
            
            if (labelSetIDToRules.containsKey(labelSetID)) {
                labelSetIDToRules.get(labelSetID).add(rule);
            } else {
                Set<CondensedRule> internalSet = new HashSet<>();
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
     */
    public Set<CondensedRule> get(int[] childstates, int labelSetID) {
        return get(childstates, labelSetID, 0);
    }
    
    private Set<CondensedRule> get(int[] childstates, int labelSetID, int index) {
        if( index == childstates.length) {
            if (labelSetIDToRules.containsKey(labelSetID)) {
                return labelSetIDToRules.get(labelSetID);
            } else return new HashSet<>();
        } else {
            int keyHere = childstates[index];
            CondensedRuleTrie nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                return new HashSet<>();
            } else {
                return nextTrie.get(childstates, labelSetID, index+1);
            }
        }
    }
    
    
    /**
     * Returns the Trie at the final state of the given childstates.
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
     */
    public CondensedRuleTrie getSubtrie(int id) {
        return map.get(id);
    }
    
    public IntSet getParents(int labelSetID) {
        assert labelSetIDToParentStateSet.containsKey(labelSetID);
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
     */
    public IntSet getBranches() {
        return map.keySet();
    }
    
    public Iterable<CondensedRule> get(int[] childStates) {
        CondensedRuleTrie t = getFinalTrie(childStates);
        return Iterables.concat(t.labelSetIDToRules.values());
    }
}
