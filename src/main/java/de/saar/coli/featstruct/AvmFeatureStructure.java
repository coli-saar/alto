/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class AvmFeatureStructure implements FeatureStructure {
    private Map<String,FeatureStructure> avm;

    public AvmFeatureStructure() {
        avm = new HashMap<>();
    }
    
    public void add(String attribute, FeatureStructure value) {
        avm.put(attribute, value);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[");
        boolean first = true;
        
        // TODO - does not represent coindexation correctly yet
        
        for( String key : avm.keySet() ) {
            if(first) {
                first = false;
            } else {
                buf.append(", ");
            }
            
            buf.append(key);
            buf.append(": ");
            buf.append(avm.get(key).toString());
        }
        
        buf.append("]");
        return buf.toString();
    }
    
    
}
 