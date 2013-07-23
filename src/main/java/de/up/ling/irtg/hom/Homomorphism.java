/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

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
//    private static int gensymNext = 1;
    private static Pattern HOM_NON_QUOTING_PATTERN = Pattern.compile("([a-zA-Z*+_]([a-zA-Z0-9_*+-]*))|([?]([0-9]+))");
    private Map<Integer, Tree<HomomorphismSymbol>> mappings;
    private Signature srcSignature, tgtSignature;
    private boolean debug = false;

    public Homomorphism(Signature src, Signature tgt) {
        mappings = new HashMap<Integer, Tree<HomomorphismSymbol>>();
        srcSignature = src;
        tgtSignature = tgt;
    }

    public final Map<Integer, Tree<HomomorphismSymbol>> getMappings() {
        return mappings;
    }

    public void add(int label, Tree<HomomorphismSymbol> mapping) {
        mappings.put(label, mapping);

        if (tgtSignature.isWritable()) {
            tgtSignature.addAllConstants(mapping);
        }
    }

    public Tree<HomomorphismSymbol> get(int label) {
        return mappings.get(label);
    }

    /*
     * Applies the homomorphism to the given tree. Returns the homomorphic image
     * of the tree under this homomorphism.
     * 
     */
    public Tree<Integer> apply(Tree<Integer> tree) {
//        final Map<String, String> knownGensyms = new HashMap<String, String>();

        return tree.dfs(new TreeVisitor<Integer, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<Integer> node, List<Tree<Integer>> childrenValues) {
                Tree<Integer> ret = construct(mappings.get(node.getLabel()), childrenValues);
                if (debug) {
                    System.err.println("\n" + node + ":");
                    System.err.println("  " + rhsAsString(mappings.get(node.getLabel())));
                    for (Tree<Integer> child : childrenValues) {
                        System.err.println("   + " + child);
                    }
                    System.err.println("  => " + ret);
                }
                return ret;
            }
        });
    }
    
    
    
    /**
     * Applies the homomorphism to a given input tree. Variables are substituted according to the "subtrees"
     * parameter: ?1, ?x1 etc. refer to the first entry in the list, and so on.
     * 
     * @param tree
     * @param subtrees
     * @param knownGensyms
     * @return 
     */
    public Tree<Integer> construct(final Tree<HomomorphismSymbol> tree, final List<Tree<Integer>> subtrees) {
        final Tree<Integer> ret = tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<HomomorphismSymbol> node, List<Tree<Integer>> childrenValues) {
                HomomorphismSymbol label = node.getLabel();

                switch (label.getType()) {
                    case VARIABLE:
                        return subtrees.get(label.getValue());
                    case CONSTANT:
                        return Tree.create(label.getValue(), childrenValues);
                    default:
                        throw new RuntimeException("undefined homomorphism symbol type");
                }
            }
        });

        return ret;
    }

    /*
    private String gensym(String gensymString, Map<String, String> knownGensyms) {
        int start = gensymString.indexOf("+");
        String prefix = gensymString.substring(0, start);
        String gensymKey = gensymString.substring(start);

        if (!knownGensyms.containsKey(gensymKey)) {
            knownGensyms.put(gensymKey, "_" + (gensymNext++));
        }

        return prefix + knownGensyms.get(gensymKey);
    }
    */

    public Set<Integer> getDomain() {
        return mappings.keySet();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        for (Integer key : mappings.keySet()) {
            buf.append(srcSignature.resolveSymbolId(key) + " -> " + rhsAsString(mappings.get(key)) + "\n");
        }

        return buf.toString();
    }

    public String rhsAsString(Tree<HomomorphismSymbol> t) {
        Tree<String> resolvedTree = t.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<HomomorphismSymbol> node, List<Tree<String>> childrenValues) {
                switch(node.getLabel().getType()) {
                    case CONSTANT:
                        return Tree.create(tgtSignature.resolveSymbolId(node.getLabel().getValue()), childrenValues);
                    case VARIABLE:
                        return Tree.create("?" + (node.getLabel().getValue()+1));
                    default:
                        return Tree.create("***");
                }
            }
        });
        
        resolvedTree.setCachingPolicy(false);
        return resolvedTree.toString(HOM_NON_QUOTING_PATTERN);
    }

    public Signature getSourceSignature() {
        return srcSignature;
    }

    public Signature getTargetSignature() {
        return tgtSignature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Homomorphism) {
            return mappings.equals(((Homomorphism) obj).mappings);
        }

        return false;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isNonDeleting() {
        for (Integer label : mappings.keySet()) {
            Tree<HomomorphismSymbol> rhs = mappings.get(label);
            Set<HomomorphismSymbol> variables = new HashSet<HomomorphismSymbol>();
            for (HomomorphismSymbol l : rhs.getLeafLabels()) {
                if (l.isVariable()) {
                    variables.add(l);
                }
            }

            if (variables.size() < srcSignature.getArity((int) label)) {
                return false;
            }
        }

        return true;
    }
}
