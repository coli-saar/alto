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
 * A ranked signature of symbols. This class maps between the symbols and their
 * numeric symbol IDs, as an {@link Interner} would. In addition, it stores, for
 * each symbol f, its arity, i.e. the number of children that a node with label
 * f must have in a well-formed tree. Every symbol must have a unique arity.
 *
 * @author koller
 */
public class Signature implements Serializable {
    private final Interner<String> interner;
    private final Int2IntMap arities;

    /**
     * This creates a new, empty signature.
     * 
     * Each signature can contains as many symbols as there are positive integers.
     */
    public Signature() {
        interner = new Interner<>();
        arities = new Int2IntOpenHashMap();
        arities.defaultReturnValue(-1);      // arity for unknown symbols
    }
    
    private Signature(Interner<String> interner, Int2IntMap arities) {
        this.interner = interner;
        this.arities = arities;
    }

    /**
     * Obtains a signature mapper between this signature on the given other
     * signature.
     * 
     * @param other
     * @return 
     */
    public SignatureMapper getMapperTo(Signature other) {
        return interner.getMapperTo(other.interner);
    }

    /**
     * Obtains an identity mapper for this signature (every id is mapped to itself).
     * 
     * @return 
     */
    public SignatureMapper getIdentityMapper() {
        return new IdentitySignatureMapper(interner);
    }

    /**
     * Removes all symbols from this signature.
     */
    public void clear() {
        interner.clear();
        this.arities.clear();
    }

    // deliberately only package-visible
//    Interner<String> getInterner() {
//        return interner;
//    }
    
    /**
     * Obtains a collection of all the symbols known to the signature.
     * @return 
     */
    public Collection<String> getSymbols() {
        return interner.getKnownObjects();
    }

    /**
     * Returns the arity of the given symbol in this signature.
     *
     * The return value will be -1 if the symbol is not known.
     * 
     * @param symbol
     * @return 
     */
    public int getArityForLabel(String symbol) {
        return arities.get(interner.resolveObject(symbol));
    }

    /**
     * Returns the arity of the symbol associated with the given id.
     * 
     * The return value will be -1 if the id is not known.
     * 
     * @param symbolId
     * @return 
     */
    public int getArity(int symbolId) {
        return arities.get(symbolId);
    }

    /**
     * Returns the symbol associated with the given id, or null if no such symbol
     * exists.
     * 
     * @param id
     * @return 
     */
    public String resolveSymbolId(int id) {
        return interner.resolveId(id);
    }

    /**
     * Collects all the symbols for the given list of ints.
     * 
     * If an id is not known,  then null will be present in the collection.
     * 
     * @param ids
     * @return 
     */
    public Collection<String> resolveSymbolIDs(IntCollection ids) {
        List<String> ret = new ArrayList<>();
        FastutilUtils.forEach(ids, id -> ret.add(resolveSymbolId(id)));
        return ret;
    }

    /**
     * Obtains the id for the given symbol.
     * 
     * Returns 0 if the symbol is not known.
     * 
     * @param symbol
     * @return 
     */
    public int getIdForSymbol(String symbol) {
        return interner.resolveObject(symbol);
    }

    /**
     * Returns true if the symbol is known to this signature.
     * 
     * @param symbol
     * @return 
     */
    public boolean contains(String symbol) {
        return interner.isKnownObject(symbol);
    }

    /**
     * Adds this symbol to the signature with the given arity.
     * 
     * If the symbol is already assigned a different arity, then an UnsupportedOperationException
     * will be thrown.
     * 
     * @param symbol
     * @param arity
     * @return 
     */
    public int addSymbol(String symbol, int arity) {
        int ret = interner.addObject(symbol);

        int previousArity = arities.get(ret);

        if (previousArity >= 0) {
            if (previousArity != arity) {
                throw new UnsupportedOperationException("Cannot add symbol " + symbol + " with arity " + arity + ", because it was already defined with arity " + previousArity + ".");
            }
        } else {
            arities.put(ret, arity);
        }

        return ret;
    }

    /**
     * Can be used to check whether this signature can be printed.
     * 
     * @return 
     */
    public boolean isWritable() {
        return true;
    }

    /**
     * Returns the highest symbol ID that has been used in this signature.
     * Iterate over all symbol IDs by iterating from 1 to getMaxSymbolId()
     * (inclusively).
     *
     * @return
     */
    public int getMaxSymbolId() {
        return interner.getNextIndex() - 1;
    }

    /**
     * Returns a map which contains the pairs of symbols and arities known
     * to this signature.
     * 
     * @return 
     */
    public Map<String, Integer> getSymbolsWithArities() {
        Map<String, Integer> ret = new HashMap<>();
        Map<String, Integer> symbolTable = interner.getSymbolTable();

        for (String sym : symbolTable.keySet()) {
            ret.put(sym, arities.get(symbolTable.get(sym)));
        }

        return ret;
    }

    /**
     * Returns a new tree with all the integers replaced by the symbols associated
     * with these integers in this signature.
     * 
     * null trees will result in a RuntimeException. If a symbol is unknown then
     * the string "null" will be inserted.
     * 
     * @param tree
     * @return 
     */
    public Tree<String> resolve(Tree<Integer> tree) {
        if (tree == null) {
            throw new RuntimeException("Cannot resolve null tree.");
        }

        return tree.dfs(new TreeVisitor<Integer, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<Integer> node, List<Tree<String>> childrenValues) {
                String label = resolveSymbolId(node.getLabel());

                if (label == null) {
                    label = "null";
                }

                return Tree.create(label, childrenValues);
            }
        });
    }

    /**
     * Adds all the symbols in the tree to the signature and returns a new tree
     * with all the symbols replaced their ids in the signature.
     * 
     * The arities for the symbols will be the number of children they have in
     * the tree.
     * 
     * @param tree
     * @return 
     */
    public Tree<Integer> addAllSymbols(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<String> node, List<Tree<Integer>> childrenValues) {
                int sym = addSymbol(node.getLabel(), childrenValues.size());
                return Tree.create(sym, childrenValues);
            }
        });
    }

    /**
     * Adds all the constants from the tree to the signature.
     * 
     * Note that this means that strings representing the int codes from the
     * HomomorphismSymbol will be added. The arities are given by the number
     * of children.
     * 
     * @param tree 
     */
    public void addAllConstants(Tree<HomomorphismSymbol> tree) {
        tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Void>() {
            @Override
            public Void combine(Tree<HomomorphismSymbol> node, List<Void> childrenValues) {
                if (node.getLabel().isConstant()) {
                    addSymbol(node.getLabel().toString(), childrenValues.size());
                }

                return null;
            }
        });
    }

    @Override
    public Object clone() {
        Int2IntMap aritiesClone = new Int2IntOpenHashMap();
        aritiesClone.putAll(arities);
        return new Signature((Interner<String>) interner.clone(), aritiesClone);
    }
    
    @Override
    public String toString() {
        List<String> syms = new ArrayList<>();
        for (int i = 1; i <= getMaxSymbolId(); i++) {
            syms.add("" + i + ":" + resolveSymbolId(i) + "/" + getArity(i));
        }

        return "[" + StringTools.join(syms, ", ") + "]";
    }

    /**
     * Returns an arrary x such that the symbol with ID i in this signature is
     * the same as the symbol with ID x[i] in the other signature. If the symbol
     * does not exist in the other signature, x[i] will be 0.
     * 
     * @param other
     * @return 
     */
    public int[] remap(Signature other) {
        return interner.remap(other.interner);
    }

    /**
     * Returns the maximum arity of any symbol in this signature.
     *
     * @return
     */
    public int getMaxArity() {
        int ret = 0;
        for (int arity : arities.values()) {
            ret = Math.max(ret, arity);
        }
        return ret;
    }

}
