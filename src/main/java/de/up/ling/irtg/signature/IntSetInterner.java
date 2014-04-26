/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author gontrum
 */
public class IntSetInterner extends Interner<it.unimi.dsi.fastutil.ints.IntSet>{

    public IntSetInterner() {
        super();
    }
    
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
