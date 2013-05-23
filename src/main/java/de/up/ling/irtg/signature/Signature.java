/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author koller
 */
public abstract class Signature {
    public abstract Collection<String> getSymbols();  // try to avoid using this, it may be hard to compute
    
    public abstract int getArity(String symbol);
    public abstract boolean contains(String symbol);
    
    public void addSymbol(String symbol, int arity) {
        throw new UnsupportedOperationException("Adding symbols to this signature is not allowed.");
    }
    
    public boolean isWritable() {
        return false;
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
    
    public void addAllSymbols(List<String> words) {
        for( String word : words ) {
            addSymbol(word, 0);
        }
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
