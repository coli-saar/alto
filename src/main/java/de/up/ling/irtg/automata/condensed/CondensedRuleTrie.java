/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import java.util.HashSet;
import java.util.Set;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;


// TODO Adjust comments (still for original version of this class)
/**
 * A recursively defined Trie of <V>
 * @author koller, modified by gontrum to use primitive int as keys.
 * @param <V>
 */
public class CondensedRuleTrie<K,V> {
    private final Int2ObjectOpenHashMap<CondensedRuleTrie<K,V>> map;
    private final Object2ObjectMap<K,Set<V>> finalStateMap;

    public CondensedRuleTrie() {
        map = new Int2ObjectOpenHashMap<CondensedRuleTrie<K,V>>();
        finalStateMap = new Object2ObjectOpenHashMap<K,Set<V>>();
    }

    /**
     * Stores a sequence of ints (the Array) in the Trie 
     * and maps the final state to the given value.
     * @param keyList
     * @param value 
     */
    public void put(int[] keyList, K key, V value) {
        put(keyList, key, value, 0);
    }
    
    /**
     * Recursive version of put.
     * Go as deep as the length of the given array.
     * @param keyList
     * @param value
     * @param index 
     */
    private void put(int[] keyList, K key, V value, int index) {
        if( index == keyList.length) {
            if (finalStateMap.containsKey(key)) {
                finalStateMap.get(key).add(value);
            } else {
                Set<V> internalSet = new HashSet<V>();
                internalSet.add(value);
                finalStateMap.put(key, internalSet);
            }
        } else {
            int keyHere = keyList[index];
            CondensedRuleTrie<K,V> nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                nextTrie = new CondensedRuleTrie<K,V>();
                map.put(keyHere, nextTrie);
            }
            
            nextTrie.put(keyList, key, value, index+1);
        }
    }
    
    /**
     * Returns a set of values, that is mapped to the final state
     * we reach with the sequence of transitions in keyList.
     * @param keyList
     * @return 
     */
    public Set<V> get(int[] keyList, K storedKey) {
        return get(keyList, storedKey, 0);
    }
    
    private Set<V> get(int[] keyList, K storedKey, int index) {
        if( index == keyList.length) {
            if (finalStateMap.containsKey(storedKey)) {
                return finalStateMap.get(storedKey);
            } else return new HashSet<V>();
        } else {
            int keyHere = keyList[index];
            CondensedRuleTrie<K,V> nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                return new HashSet<V>();
            } else {
                return nextTrie.get(keyList, storedKey, index+1);
            }
        }
    }
    
    /**
     * Returns the subtrie that we reach with a transition with the given symbol.
     * @param id
     * @return 
     */
    public CondensedRuleTrie<K,V> getSubtrie(int id) {
        return map.get(id);
    }
    
    /**
     * Returns an IntSet with the symbols for all outgoing transitions.
     * @return 
     */
    public IntSet getBranches() {
        return map.keySet();
    }
    
//    /**
//     * Returns the stored values that we get from the state we reach 
//     * with the given symbol.
//     * @param id
//     * @return 
//     */
//    public Collection<V> getValuesForId(int id){
//        if (map.containsKey(id)) {
//            return map.get(id).values();
//        } else {
//            return new HashSet<V>();
//        }
//    }
    
    /**
     * Get all values that are stored in this trie including its subtries.
     * @return 
     */
//    public Collection<V> values() {
//    	Collection<V> ret = new ArrayList<V>();
//    	collectValues(ret);    	
//    	return ret;
//    }
//    
//    private void collectValues(Collection<V> ret) {
//    	ret.addAll(values);
//    	
//    	for( CondensedRuleTrie<K,V> child : map.values() ) {
//    		child.collectValues(ret);
//    	}
//    }
}
