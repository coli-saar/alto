/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

import de.saar.basic.StringOrVariable;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 */
public class Homomorphism {
    private static int gensymNext = 1;
    private static Pattern HOM_NON_QUOTING_PATTERN = Pattern.compile("([a-zA-Z*+_]([a-zA-Z0-9_*+-]*))|([?]([0-9]+))");
    private Map<String, Tree<StringOrVariable>> mappings;
    private Signature srcSignature, tgtSignature;
    private boolean debug = false;

    public Homomorphism(Signature src, Signature tgt) {
        mappings = new HashMap<String, Tree<StringOrVariable>>();
        srcSignature = src;
        tgtSignature = tgt;
    }

    public void add(String label, Tree<StringOrVariable> mapping) {
        mappings.put(label, mapping);
        
        if( tgtSignature.isWritable() ) {
            tgtSignature.addAllSymbolsWithoutVariables(mapping);
        }
    }

    public Tree<StringOrVariable> get(String label) {
        return mappings.get(label);
    }
    
    /*
     * Applies the homomorphism to the given tree. Returns the homomorphic image
     * of the tree under this homomorphism.
     * 
     */
    public Tree<String> apply(Tree<String> tree) {        
        final Map<String,String> knownGensyms = new HashMap<String, String>();
        
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                Tree<String> ret = construct(mappings.get(node.getLabel()), childrenValues, knownGensyms);
                if( debug ) {
                    System.err.println("\n" + node + ":");
                    System.err.println("  " + rhsAsString(mappings.get(node.getLabel())));
                    for( Tree<String> child : childrenValues ) {
                        System.err.println("   + " + child);
                    }
                    System.err.println("  => " + ret);
                }
                return ret;
            }            
        });        
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
    
    public Tree<String> construct(final Tree<StringOrVariable> tree, final List<Tree<String>> subtrees, final Map<String,String> knownGensyms) {
        final Tree<String> ret = tree.dfs(new TreeVisitor<StringOrVariable, String, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<StringOrVariable> node, List<Tree<String>> childrenValues) {
                StringOrVariable label = node.getLabel();
                
                if( label.isVariable() ) {
                    return subtrees.get(getIndexForVariable(label));
                } else if( label.getValue().contains("+") ) {
                    return Tree.create(gensym(label.getValue(), knownGensyms), childrenValues);
                } else {
                    return Tree.create(label.getValue(), childrenValues);
                }
            }
        });

        return ret;
    }
    
    private String gensym(String gensymString, Map<String,String> knownGensyms) {
        int start = gensymString.indexOf("+");
        String prefix = gensymString.substring(0, start);
        String gensymKey = gensymString.substring(start);        
        
        if( ! knownGensyms.containsKey(gensymKey) ) {
            knownGensyms.put(gensymKey, "_" + (gensymNext++));
        }
        
        return prefix + knownGensyms.get(gensymKey);
    }

    public static int getIndexForVariable(StringOrVariable varname) {
        int indexStartPos = 0;
        String val = varname.getValue();
        
        while( indexStartPos < val.length() && ! Character.isDigit(val.charAt(indexStartPos)) ) {
            indexStartPos++;
        }
        
        if( indexStartPos < val.length() ) {
            return Integer.parseInt(varname.getValue().substring(indexStartPos)) - 1;
        } else {
            return -1;
        }
        
        
    }

    public Set<String> getDomain() {
        return mappings.keySet();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        for( String key : mappings.keySet() ) {
            buf.append(key + " -> " + rhsAsString(mappings.get(key)) + "\n");
        }
        
        return buf.toString();
    }
    
    public static String rhsAsString(Tree<StringOrVariable> t) {
        t.setCachingPolicy(false);
        return t.toString(HOM_NON_QUOTING_PATTERN);
    }

    public Signature getSourceSignature() {
        return srcSignature;
    }

    public Signature getTargetSignature() {
        return tgtSignature;
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof Homomorphism ) {
            return mappings.equals(((Homomorphism) obj).mappings);
        }
        
        return false;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    

    
    
    public boolean isNonDeleting() {
        for( String label : mappings.keySet() ) {
            Tree<StringOrVariable> rhs = mappings.get(label);
            Set<StringOrVariable> variables = new HashSet<StringOrVariable>();
            for( StringOrVariable l : rhs.getLeafLabels() ) {
                if( l.isVariable() ) {
                    variables.add(l);
                }
            }
            
            if( variables.size() < srcSignature.getArity(label)) {
//                System.err.println("hom is nondeleting: " + label + "/" + srcSignature.getArity(label) +  " -> " + rhs);
                return false;
            }
        }
        
        return true;        
    }
    
}
