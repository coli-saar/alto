/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.List;

/**
 *
 * @author koller
 */
public class HomomorphismSymbol {
    public enum Type {
        CONSTANT, VARIABLE, GENSYM
    };
    private Type type;
    private String value;

    private HomomorphismSymbol(String value, Type type) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public static HomomorphismSymbol createVariable(String varname) {
        return new HomomorphismSymbol(varname, Type.VARIABLE);
    }

    public static HomomorphismSymbol createConstant(String name) {
        return new HomomorphismSymbol(name, Type.CONSTANT);
    }

    public static HomomorphismSymbol createGensym(String name) {
        return new HomomorphismSymbol(name, Type.GENSYM);
    }
    
    public static HomomorphismSymbol createFromName(String name) {
        if( name.startsWith("?")) {
            return createVariable(name);
        } else if( name.contains("+")) {
            return createGensym(name);
        } else {
            return createConstant(name);
        }
    }
    
    public static Tree<HomomorphismSymbol> treeFromNames(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<HomomorphismSymbol>>() {
            @Override
            public Tree<HomomorphismSymbol> combine(Tree<String> node, List<Tree<HomomorphismSymbol>> childrenValues) {
                return Tree.create(createFromName(node.getLabel()), childrenValues);
            }            
        });
    }

    public boolean isConstant() {
        return getType() == Type.CONSTANT;
    }

    public boolean isVariable() {
        return getType() == Type.VARIABLE;
    }

    public boolean isGensym() {
        return getType() == Type.GENSYM;
    }

    public int getIndex() {
        int indexStartPos = 0;
        String val = getValue();
        int ret = 0;
        boolean foundIndex = false;

        if (type != Type.VARIABLE) {
            return -1;
        } else {

            while (indexStartPos < val.length() && !isDigit(val.charAt(indexStartPos))) {
                indexStartPos++;
            }

            while (indexStartPos < val.length()) {
                char c = val.charAt(indexStartPos++);

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
    }

    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    @Override
    public String toString() {
        return value;
    }   

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 71 * hash + (this.value != null ? this.value.hashCode() : 0);
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
        if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
    
}
