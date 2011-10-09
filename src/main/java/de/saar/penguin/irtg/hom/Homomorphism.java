/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.hom;

import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class Homomorphism {
    private Map<String, Tree<StringOrVariable>> mappings;

    public Homomorphism() {
        mappings = new HashMap<String, Tree<StringOrVariable>>();
    }

    public void add(String label, Tree<StringOrVariable> mapping) {
        mappings.put(label, mapping);
    }

    public Tree<StringOrVariable> get(String label) {
        return mappings.get(label);
    }

    public Tree<String> apply(Tree<String> tree) {
        List<String> children = tree.getChildren(tree.getRoot());
        List<Tree<String>> subtrees = new ArrayList<Tree<String>>();

        for (String child : children) {
            subtrees.add(apply(tree.subtree(child)));
        }

        return construct(mappings.get(tree.getLabel(tree.getRoot())), subtrees);
    }

    private Tree<String> construct(final Tree<StringOrVariable> tree, final List<Tree<String>> subtrees) {
        final Tree<String> ret = new Tree<String>();

        tree.dfs(new TreeVisitor<String, Void>() {
            @Override
            public String visit(String node, String parent) {
                StringOrVariable label = tree.getLabel(node);

                if (label.isVariable()) {
                    ret.insert(subtrees.get(getIndexForVariable(label)), parent);
                    return null; // never used
                } else {
                    String newNode = ret.addNode(label.getValue(), parent);
                    return newNode;
                }
            }

            @Override
            public String getRootValue() {
                return null;
            }
        });

        return ret;
    }

    /**
     * Returns a map that maps node names in hom(f) to variables,
     * for all variables that occur in hom(f).
     * 
     * @param f
     * @return 
     */
    public Map<String,StringOrVariable> getVariableMap(final String f) {
        final Map<String,StringOrVariable> ret = new HashMap<String,StringOrVariable>();
        final Tree<StringOrVariable> tree = mappings.get(f);

        tree.dfs(new TreeVisitor<Void, Void>() {
            @Override
            public Void combine(String node, List<Void> childrenValues) {
                StringOrVariable sv = tree.getLabel(node);
                
                if( sv.isVariable() ) {
                    ret.put(node, sv);
                }
                
                return null;
            }
        });

        return ret;
    }

    public static int getIndexForVariable(StringOrVariable varname) {
        return Integer.parseInt(varname.getValue().substring(1)) - 1;
    }

    public int getArity(String label) {
        Integer ar = mappings.get(label).dfs(new TreeVisitor<Void, Integer>() {
            @Override
            public Integer combine(String node, List<Integer> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return 0;
                } else {
                    int childrenMax = Collections.max(childrenValues);
                    return Math.max(childrenMax, childrenValues.size());
                }
            }
        });

        return ar;
    }

    public Set<String> getDomain() {
        return mappings.keySet();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        for( String key : mappings.keySet() ) {
            buf.append(key + " -> " + mappings.get(key) + "\n");
        }
        
        return buf.toString();
    }
}
