/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author koller
 */
public class Signature implements Serializable {
    private Interner<String> interner;
    private Int2IntMap arities;

    public Signature() {
        interner = new Interner<String>();
        arities = new Int2IntOpenHashMap();
    }
    
    public Collection<String> getSymbols() {
        return interner.getKnownObjects();
    }
    
    public int getArity(String symbol) {
        return arities.get(interner.resolveObject(symbol));
    }
    
    public boolean contains(String symbol) {
        return interner.isKnownObject(symbol);
    }
    
    public int addSymbol(String symbol, int arity) {
        int ret = interner.addObject(symbol);
        arities.put(ret, arity);
        return ret;
    }
    
    public boolean isWritable() {
        return true;
    }
    
    public void addAllSymbols(Tree<String> tree) {
        tree.dfs(new TreeVisitor<String, Void, Void>() {
            @Override
            public Void combine(Tree<String> node, List<Void> childrenValues) {
                addSymbol(node.getLabel(), childrenValues.size());
                return null;
            }
        });
    }
    
    public void addAllConstants(Tree<HomomorphismSymbol> tree) {
        tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Void>() {
            @Override
            public Void combine(Tree<HomomorphismSymbol> node, List<Void> childrenValues) {
                if( node.getLabel().isConstant() ) {
                    addSymbol(node.getLabel().toString(), childrenValues.size());
                }
                
                return null;
            }
        });
    }
}
