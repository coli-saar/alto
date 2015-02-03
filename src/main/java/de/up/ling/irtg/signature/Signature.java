/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import de.saar.basic.StringTools;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public SignatureMapper getMapperTo(Signature other) {
        return interner.getMapperTo(other.interner);
    }
    
    public SignatureMapper getIdentityMapper() {
        return new IdentitySignatureMapper(interner);
    }
    
    public void clear() {
        interner.clear();
    }
    
    // deliberately only package-visible
//    Interner<String> getInterner() {
//        return interner;
//    }
    
    public Collection<String> getSymbols() {
        return interner.getKnownObjects();
    }
    
    public int getArityForLabel(String symbol) {
        return arities.get(interner.resolveObject(symbol));
    }
    
    public int getArity(int symbolId) {
        return arities.get(symbolId);
    }
    
    public String resolveSymbolId(int id) {
        return interner.resolveId(id);
    }
    
    public Collection<String> resolveSymbolIDs(IntCollection ids) {
        List<String> ret = new ArrayList<>();
        FastutilUtils.forEach(ids, id -> ret.add(resolveSymbolId(id)));
        return ret;
    }
    
    public int getIdForSymbol(String symbol) {
        return interner.resolveObject(symbol);
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
    
    public int getMaxSymbolId() {
        return interner.getNextIndex()-1;
    }
    
    public Map<String,Integer> getSymbolsWithArities() {
        Map<String,Integer> ret = new HashMap<String, Integer>();
        Map<String,Integer> symbolTable = interner.getSymbolTable();
        
        for( String sym : symbolTable.keySet() ) {
            ret.put(sym, arities.get(symbolTable.get(sym)));
        }
        
        
        return ret;
    }
    
    public Tree<String> resolve(Tree<Integer> tree) {
        return tree.dfs(new TreeVisitor<Integer, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<Integer> node, List<Tree<String>> childrenValues) {
                String label = resolveSymbolId(node.getLabel());
                
                if( label == null ) {
                    label = "null";
                }
                
                return Tree.create(label, childrenValues);
            }           
        });
    }
    
    public Tree<Integer> addAllSymbols(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<String> node, List<Tree<Integer>> childrenValues) {
                int sym = addSymbol(node.getLabel(), childrenValues.size());
                return Tree.create(sym, childrenValues);
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

    @Override
    public String toString() {
        List<String> syms = new ArrayList<String>();
        for( int i = 1; i <= getMaxSymbolId(); i++ ) {
            syms.add("" + i + ":" + resolveSymbolId(i) + "/" + getArity(i));
        }
        
        return "[" + StringTools.join(syms, ", ") + "]";
    }
    
    /**
     * Returns an arrary x such that the symbol with ID
     * i in this signature is the same as the symbol with ID
     * x[i] in the other signature. If the symbol
     * does not exist in the other signature, x[i]
     * will be 0.
     */
    public int[] remap(Signature other) {
        return interner.remap(other.interner);
    }    
}
