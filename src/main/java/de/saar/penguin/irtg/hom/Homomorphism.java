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
    private static int gensymNext = 1;
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
        return apply(tree, new HashMap<String, String>());
    }

    private Tree<String> apply(Tree<String> tree, final Map<String,String> knownGensyms) {
        List<String> children = tree.getChildren(tree.getRoot());
        List<Tree<String>> subtrees = new ArrayList<Tree<String>>();

        for (String child : children) {
            subtrees.add(apply(tree.subtree(child), knownGensyms));
        }

        return construct(mappings.get(tree.getLabel(tree.getRoot())), subtrees, knownGensyms);
    }

    
    /*
     * THIS IS A HACK THAT NEEDS TO BE FIXED SOON!!
     * 
     * In "construct", we assume that a gensym label is just a string that starts with a +. It makes no sense to distinguish
     * between variables and non-variables, but not between ordinary labels and gensym labels. Either we should make everything
     * just strings and interpret variables appropriately in #construct, or StringOrVariable needs a third type for gensyms.
     * 
     * Note that homomorphisms with gensyms are not, strictly speaking, homomorphisms in the theoretical sense of the word.
     * It will in general not be possible to compute e.g. pre-image of an automaton under such "homomorphisms".
     * 
     */
    
    private Tree<String> construct(final Tree<StringOrVariable> tree, final List<Tree<String>> subtrees, final Map<String,String> knownGensyms) {
        final Tree<String> ret = new Tree<String>();

        tree.dfs(new TreeVisitor<String, Void>() {
            @Override
            public String visit(String node, String parent) {
                StringOrVariable label = tree.getLabel(node);

                if (label.isVariable()) {
                    ret.insert(subtrees.get(getIndexForVariable(label)), parent);
                    return null; // never used
                } else if( label.getValue().startsWith("+")) {
                    String newNode = ret.addNode(gensym(label.getValue(), knownGensyms), parent);
                    return newNode;
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
    
    private String gensym(String gensymKey, Map<String,String> knownGensyms) {
        if( ! knownGensyms.containsKey(gensymKey) ) {
            knownGensyms.put(gensymKey, "_gen" + (gensymNext++));
        }
        
        return knownGensyms.get(gensymKey);
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
