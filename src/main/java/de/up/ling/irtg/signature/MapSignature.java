/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class MapSignature extends Signature implements Serializable {
    private Map<String,Integer> symbols;

    public MapSignature() {
        symbols = new HashMap<String, Integer>();
    }

    @Override
    public Collection<String> getSymbols() {
        return symbols.keySet();
    }

    @Override
    public int getArity(String symbol) {
        return symbols.get(symbol);
    }
    
    @Override
    public void addSymbol(String symbol, int arity) {
        symbols.put(symbol, arity);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean contains(String symbol) {
        return symbols.containsKey(symbol);
    }

    @Override
    public String toString() {
        return symbols.toString();
    }
    
    
    
}
