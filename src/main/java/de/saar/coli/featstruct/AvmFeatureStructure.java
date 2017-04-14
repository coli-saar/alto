/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class AvmFeatureStructure extends FeatureStructure {
    private Map<String,FeatureStructure> avm;

    public AvmFeatureStructure() {
        avm = new HashMap<>();
    }
    
    public void put(String attribute, FeatureStructure value) {
        avm.put(attribute, value);
    }
    
    public Set<String> getAttributes() {
        return avm.keySet();
    }
    
    public FeatureStructure get(String attribute) {
        return avm.get(attribute);
    }

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        boolean first = true;
        
        buf.append("[");
        
        for( String key : avm.keySet() ) {
            if(first) {
                first = false;
            } else {
                buf.append(", ");
            }
            
            buf.append(key);
            buf.append(": ");
            
            avm.get(key).appendWithIndex(visitedIndexedFs, buf);
        }
        
        buf.append("]");
    }

    @Override
    protected Object getValue(String[] path, int pos) {
//        System.err.printf("gv on %s [%d]\n", Arrays.toString(path), pos);
        
        if( pos < path.length ) {
            FeatureStructure ch = get(path[pos]);
            
            if( ch == null ) {
                return null;
            } else {
                return ch.getValue(path, pos+1);
            }
        } else {
            return null;
        }
    }
}
 