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
    private static int gensymNext = 1;
    private static Pattern HOM_NON_QUOTING_PATTERN = Pattern.compile("([a-zA-Z*+_]([a-zA-Z0-9_*+-]*))|([?]([0-9]+))");
    private Map<String, Tree<HomomorphismSymbol>> mappings;
    private Signature srcSignature, tgtSignature;
    private boolean debug = false;

    public Homomorphism(Signature src, Signature tgt) {
        mappings = new HashMap<String, Tree<HomomorphismSymbol>>();
        srcSignature = src;
        tgtSignature = tgt;
    }

    public final Map<String, Tree<HomomorphismSymbol>> getMappings() {
        return mappings;
    }

    public void add(String label, Tree<HomomorphismSymbol> mapping) {
        mappings.put(label, mapping);

        if (tgtSignature.isWritable()) {
            tgtSignature.addAllConstants(mapping);
        }
    }

    public Tree<HomomorphismSymbol> get(String label) {
        return mappings.get(label);
    }

    /*
     * Applies the homomorphism to the given tree. Returns the homomorphic image
     * of the tree under this homomorphism.
     * 
     */
    public Tree<String> apply(Tree<String> tree) {
        final Map<String, String> knownGensyms = new HashMap<String, String>();

        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                Tree<String> ret = construct(mappings.get(node.getLabel()), childrenValues, knownGensyms);
                if (debug) {
                    System.err.println("\n" + node + ":");
                    System.err.println("  " + rhsAsString(mappings.get(node.getLabel())));
                    for (Tree<String> child : childrenValues) {
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
     * parameter: ?1, ?x1 etc. refer to the first entry in the list, and so on. The "knownGensyms" parameter
     * specifies previously generated symbols for gensym labels in the homomorphic image.
     * 
     * @param tree
     * @param subtrees
     * @param knownGensyms
     * @return 
     */
    public Tree<String> construct(final Tree<HomomorphismSymbol> tree, final List<Tree<String>> subtrees, final Map<String, String> knownGensyms) {
        final Tree<String> ret = tree.dfs(new TreeVisitor<HomomorphismSymbol, String, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<HomomorphismSymbol> node, List<Tree<String>> childrenValues) {
                HomomorphismSymbol label = node.getLabel();

                switch (label.getType()) {
                    case VARIABLE:
                        return subtrees.get(label.getIndex());
                    case GENSYM:
                        return Tree.create(gensym(label.getValue(), knownGensyms), childrenValues);
                    case CONSTANT:
                        return Tree.create(label.getValue(), childrenValues);
                    default:
                        throw new RuntimeException("undefined homomorphism symbol type");
                }
            }
        });

        return ret;
    }

    private String gensym(String gensymString, Map<String, String> knownGensyms) {
        int start = gensymString.indexOf("+");
        String prefix = gensymString.substring(0, start);
        String gensymKey = gensymString.substring(start);

        if (!knownGensyms.containsKey(gensymKey)) {
            knownGensyms.put(gensymKey, "_" + (gensymNext++));
        }

        return prefix + knownGensyms.get(gensymKey);
    }

    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    public static int getIndexForVariable(HomomorphismSymbol varname) {
        int indexStartPos = 0;
        String val = varname.getValue();
        int ret = 0;
        boolean foundIndex = false;

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
//            return Integer.parseInt(varname.getValue().substring(indexStartPos)) - 1;
//            System.err.println(val + " -> " + ret);
            return ret - 1;
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

        for (String key : mappings.keySet()) {
            buf.append(key + " -> " + rhsAsString(mappings.get(key)) + "\n");
        }

        return buf.toString();
    }

    public static String rhsAsString(Tree<HomomorphismSymbol> t) {
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
        if (obj instanceof Homomorphism) {
            return mappings.equals(((Homomorphism) obj).mappings);
        }

        return false;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isNonDeleting() {
        for (String label : mappings.keySet()) {
            Tree<HomomorphismSymbol> rhs = mappings.get(label);
            Set<HomomorphismSymbol> variables = new HashSet<HomomorphismSymbol>();
            for (HomomorphismSymbol l : rhs.getLeafLabels()) {
                if (l.isVariable()) {
                    variables.add(l);
                }
            }

            if (variables.size() < srcSignature.getArity(label)) {
//                System.err.println("hom is nondeleting: " + label + "/" + srcSignature.getArity(label) +  " -> " + rhs);
                return false;
            }
        }

        return true;
    }
}
