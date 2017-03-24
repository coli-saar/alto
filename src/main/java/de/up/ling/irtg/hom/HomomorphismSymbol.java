/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.Serializable;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 */
public class HomomorphismSymbol implements Serializable {
    private static final Pattern VARNAME_PATTERN = Pattern.compile("\\?\\d+");

    public enum Type {
        CONSTANT, VARIABLE
        // GENSYM
    };
    private final Type type;
    private final int value;

    private HomomorphismSymbol(int value, Type type) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public static HomomorphismSymbol createVariable(String varname) {
        return new HomomorphismSymbol(getVariableIndex(varname), Type.VARIABLE);
    }
    
    public static HomomorphismSymbol createVariable(int variableId) {
        return new HomomorphismSymbol(variableId, Type.VARIABLE);
    }

    /**
     * Creates a constant HomomorphismSymbol for the given symbol.
     * The symbol is added to the given signature with the given
     * arity if needed.
     * 
     * @param name
     * @param signature
     * @param arity
     * @return 
     */
    public static HomomorphismSymbol createConstant(String name, Signature signature, int arity) {
        return new HomomorphismSymbol(signature.addSymbol(name, arity), Type.CONSTANT);
    }

    /**
     * Creates a constant HomomorphismSymbol for the given symbol ID.
     * 
     * @param symbolId
     * @return 
     */
    public static HomomorphismSymbol createConstant(int symbolId) {
        return new HomomorphismSymbol(symbolId, Type.CONSTANT);
    }

    public static boolean isVariableSymbol(String sym) {
        if( sym.length() < 2 || sym.charAt(0) != '?') {
            return false;
        }
        
        for( int i = 1; i < sym.length(); i++ ) {
            if( ! Character.isDigit(sym.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }

    private static HomomorphismSymbol createFromName(String name, Signature signature, int arity) {
        if (isVariableSymbol(name)) {
            return createVariable(name);
        } else {
            return createConstant(name, signature, arity);
        }
    }

    /**
     * Converts a tree of string labels into a tree of HomomorphismSymbols.
     * Nodes with the labels ?1, ?2, ... will be converted into variables.
     * Constants will be resolved to symbol IDs using the given signature.
     * Constants that are not known in the signature will be added to the
     * signature, with the number of children in the tree as the arity.
     *
     * @param tree
     * @param signature
     * @return
     */
    public static Tree<HomomorphismSymbol> treeFromNames(Tree<String> tree, final Signature signature) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<HomomorphismSymbol>>() {
            @Override
            public Tree<HomomorphismSymbol> combine(Tree<String> node, List<Tree<HomomorphismSymbol>> childrenValues) {
                return Tree.create(createFromName(node.getLabel(), signature, childrenValues.size()), childrenValues);
            }
        });
    }

    /**
     * Converts a tree of HomomorphismSymbols into a tree of string labels.
     * Symbol IDs in constant labels are resolved according to the given
     * signature. The tree returned by this method can be converted back into a
     * tree of HomomorphismSymbols using treeFromNames.
     *
     * @param tree
     * @param signature
     * @return
     */
    public static Tree<String> toStringTree(Tree<HomomorphismSymbol> tree, final Signature signature) {
        if (tree == null) {
            return Tree.create("<null>");
        } else {
            return tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<String>>() {
                @Override
                public Tree<String> combine(Tree<HomomorphismSymbol> node, List<Tree<String>> childrenValues) {
                    switch (node.getLabel().getType()) {
                        case VARIABLE:
                            return Tree.create("?" + (node.getLabel().getValue() + 1));
                        case CONSTANT:
                            return Tree.create(signature.resolveSymbolId(node.getLabel().getValue()), childrenValues);
                    }

                    return null;
                }
            });
        }
    }

    public boolean isConstant() {
        return getType() == Type.CONSTANT;
    }

    public boolean isVariable() {
        return getType() == Type.VARIABLE;
    }

    @Deprecated
    public int getIndex() {
        if (type != Type.VARIABLE) {
            return -1;
        } else {
            return value;
        }
    }

    public static int getVariableIndex(String varname) {
        int indexStartPos = 0;
        int ret = 0;
        boolean foundIndex = false;

        while (indexStartPos < varname.length() && !isDigit(varname.charAt(indexStartPos))) {
            indexStartPos++;
        }

        while (indexStartPos < varname.length()) {
            char c = varname.charAt(indexStartPos++);

            if (isDigit(c)) {
                foundIndex = true;
                ret = 10 * ret + (c - '0');
            }
        }

        if (foundIndex) {
            return ret - 1;
        } else {
            return -1;
        }
    }

    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    @Override
    public String toString() {
        if( isVariable() ) {
            return "?" + (value+1);
        } else {
            return "" + value;
        }
    }

    
    private static ToIntFunction<HomomorphismSymbol> HOM_SYMBOL_TO_INT = x -> x.getValue();

    public static ToIntFunction<HomomorphismSymbol> getHomSymbolToIntFunction() {
        return HOM_SYMBOL_TO_INT;
    }
    
    /**
     * This is for running an automaton on the RHS of a homomorphism. "mapper" maps
     * the hom target signature to the automaton signature. The function returns
     * the symbol ID of the automaton signature to which the symbol ID in the HomomorphismSymbol
     * corresponds.
    */
    public static ToIntFunction<HomomorphismSymbol> getRemappingSymbolToIntFunction(final SignatureMapper mapper) {
        return f -> mapper.remapForward(f.getValue());
    };
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 79 * hash + this.value;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HomomorphismSymbol other = (HomomorphismSymbol) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.value != other.value) {
            return false;
        }
        return true;
    }
}
