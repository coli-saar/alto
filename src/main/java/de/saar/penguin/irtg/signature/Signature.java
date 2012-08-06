/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.signature;

import de.saar.basic.StringOrVariable;
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
    
    public void addAllSymbolsWithoutVariables(Tree<StringOrVariable> tree) {
        tree.dfs(new TreeVisitor<StringOrVariable, Void, Void>() {
            @Override
            public Void combine(Tree<StringOrVariable> node, List<Void> childrenValues) {
                if( ! node.getLabel().isVariable() ) {
                    addSymbol(node.getLabel().toString(), childrenValues.size());
                }
                
                return null;
            }
        });
    }
}
