/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * A specialized interner for IntSets used for example to keep track of sets
 * of symbol ids.
 * 
 * This adds a special method used to extend a set which has been interned.
 * 
 * @author gontrum
 */
public class IntSetInterner extends Interner<it.unimi.dsi.fastutil.ints.IntSet>{
    
    /**
     * This method adds the given newValue to the set which is associated with
     * the given index.
     * 
     * The method returns true if such a set exists and false otherwise. In the
     * latter case nothing is changed within the interner.
     * 
     * @param index
     * @param newValue
     * @return 
     */
    public boolean addValueToSetByID(int index, int newValue) {
        IntSet toChange = resolveId(index);
        
        if (toChange != null) {
            // remove set from the map where it is the key
            objectToInt.remove(toChange); 
            
            // change the set by adding the new value
            toChange.add(newValue);
            
            // Put it back in the map that it was removed from
            
            objectToInt.put(toChange, index);
        } else return false;
        return true;
    }
    
}
