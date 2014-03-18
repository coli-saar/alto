/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

import static de.up.ling.irtg.hom.HomomorphismSymbol.Type.CONSTANT;
import static de.up.ling.irtg.hom.HomomorphismSymbol.Type.VARIABLE;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
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
    private Map<Integer, Tree<HomomorphismSymbol>> mappings; // TODO remove
    private Signature srcSignature, tgtSignature;
    private boolean debug = false;
    
    private Interner<Tree<HomomorphismSymbol>> terms;   // Maps an ID to each term
    private Interner<IntSet> labelSetInterner;
    private Int2IntMap termsToLabelSet; // Lookup for existing terms
    private Int2IntMap labelToLabelSet; // Find the labelSet for a given label
    private Int2IntMap labelSetToTerm;  // Lookup the term that has been maped to a labelSet
    
    private Int2IntMap tgtIDToSrcID;    // maps an int that resolves to an string in the tgt signature 
                                        // to an int, that resolves to the coresponding string in the src signature
    
    public Homomorphism(Signature src, Signature tgt) {
//        if (debug) System.err.println("New Homomorphism created...\n");
        mappings = new HashMap<Integer, Tree<HomomorphismSymbol>>();
        srcSignature = src;
        tgtSignature = tgt;

        terms = new Interner<Tree<HomomorphismSymbol>>();
        termsToLabelSet = new Int2IntOpenHashMap();
        labelToLabelSet = new Int2IntOpenHashMap();
        labelSetToTerm = new Int2IntOpenHashMap();
        labelSetInterner = new Interner<IntSet>();
        
        tgtIDToSrcID = new Int2IntOpenHashMap();
    }

    
    public void add(String label, Tree<String> mapping) {
        add(srcSignature.getIdForSymbol(label), HomomorphismSymbol.treeFromNames(mapping, tgtSignature));
    }

    public void add(int label, Tree<HomomorphismSymbol> mapping) {        
        mappings.put(label, mapping); // To be removed later?
        if (debug) System.err.println("Adding " + label + " - " + mapping);
        int labelSetID;
        if (terms.isKnownObject(mapping)) { // Term is already processed. We only need to add the given label to the proper labelSet
            if (debug) System.err.println("-> " + mapping + " is already known.");
            int termID = terms.resolveObject(mapping); 
            assert termsToLabelSet.containsKey(termID);
            labelSetID = termsToLabelSet.get(termID);
            addToLabelSet(label, labelSetID);
            if (debug) System.err.println("termID = " + termID);
            if (debug) System.err.println("labelSetID = " + labelSetID);
            labelToLabelSet.put(label, labelSetID); // Add the mapping from the label to the labelSet
        } else {
            if (debug) System.err.println("-> " + mapping + " is new.");
            int termID = terms.addObject(mapping); 
            labelSetID = createNewLabelSet(label);
            if (debug) System.err.println("termID = " + termID);
            labelToLabelSet.put(label, labelSetID);
            labelSetToTerm.put(labelSetID, termID);
            termsToLabelSet.put(termID, labelSetID);
        }
        
        // Save a link between the label of the current Tree to the ID in the target signature
        int tgtID = HomomorphismSymbol.getHomSymbolToIntFunction().apply(mapping.getLabel());
        tgtIDToSrcID.put(tgtID, labelSetID);
    }

    
    private IntSet getLabelSet(int labelSetID) {
        IntSet ret = labelSetInterner.resolveId(labelSetID);
        if (ret != null) {
            return ret;
        } else return new IntOpenHashSet();
    }
    
    // Adds a label to an existing labelSet.
    private void addToLabelSet(int label, int labelSetID) {
        IntSet labelSet = getLabelSet(labelSetID);
        if (debug) System.err.println("labelSet = " + labelSet);
        labelSet.add(label);
        if (debug) System.err.println("labelSet\\ = " + labelSet);
    }
    
    // Creates a new labelSet for a new label and returns the labelSetID
    private int createNewLabelSet(int label) {
        IntSet labelSet = new IntOpenHashSet();
        int labelSetID = labelSetInterner.addObject(labelSet);
        labelSet.add(label);
        if (debug) System.err.println("labelSetID = " + labelSetID);
        return labelSetID;
    }
    /**
     * Returns the value h(label), using symbol IDs.
     * 
     * @param label
     * @return 
     */
    public Tree<HomomorphismSymbol> get(int label) {
        if (debug) System.err.println("Getting mapping for " + label);
//        assert mappings.get(label).equals(terms.resolveId(labelSetToTerm.get(labelToLabelSet.get(label))));
        return mappings.get(label);
//        if (!labelToLabelSet.containsKey(label)) {
//            return null;
//        }
//        int labelSetID = labelToLabelSet.get(label);
//        int termID = labelSetToTerm.get(labelSetID);
////        if (debug) System.err.println("New: " + terms.resolveId(termID));
////        if (debug) System.err.println("Old: " + mappings.get(label));
////        assert mappings.get(label).equals(terms.resolveId(labelSetToTerm.get(labelToLabelSet.get(label))));
//        return terms.resolveId(termID);
    }
    
    public Tree<HomomorphismSymbol> get(IntSet labelSet) {
        if (labelSetInterner.isKnownObject(labelSet)) {
            int labelSetID = labelSetInterner.resolveObject(labelSet);
            assert labelSetToTerm.containsKey(labelSetID);
            int termID = labelSetToTerm.get(labelSetID);
            return terms.resolveId(termID);
        } else return null;
    }
    
    public Tree<HomomorphismSymbol> getByLabelSetID(int labelSetID) {
        if (labelSetToTerm.containsKey(labelSetID)) {
            int termID = labelSetToTerm.get(labelSetID);
            return terms.resolveId(termID);
        } else {
            return null;
        }
    }
    
    public IntSet getLabelSetByLabelSetID(int labelSetID) {
        assert labelSetID < labelSetInterner.getNextIndex();
        return labelSetInterner.resolveId(labelSetID);
    }
    
    public int getTermID(int label) {
        int labelSetID = labelToLabelSet.get(label);
        return labelSetToTerm.get(labelSetID);
    }
    
    public int getLabelSetID(int label) {
        return labelToLabelSet.get(label);
    }
    
    public int getTermIDByLabelSet(int labelSetID) {
        return labelSetToTerm.get(labelSetID);
    }
    
    public IntSet getLabelSetForLabel(int label) {
        return getLabelSet(labelToLabelSet.get(label));
    }
    
    /**
     * Returns the value h(label). The label is resolved
     * according to the homomorphism's source signature, and
     * is expected to be known there. The labels in the returned
     * tree are elements of the homomorphism's target signature.
     * If necessary, the returned tree can be converted back to
     * a tree of HomomorphismSymbols using HomomorphismSymbo.treeFromNames
     * and the homomorphism's target signature.
     * 
     * @param label
     * @return 
     */
    public Tree<String> get(String label) {
        if (debug) System.err.println("Getting for " + label);
        return HomomorphismSymbol.toStringTree(get(srcSignature.getIdForSymbol(label)), tgtSignature);
    }

    
    public IntSet getLabelsetIDsForTgtSymbols(IntSet tgtSymbols) {
        IntSet ret = new IntOpenHashSet();
        for (int tgtSymbol : tgtSymbols) {
            ret.add(tgtIDToSrcID.get(tgtSymbol));
        }
        
        return ret;
    }
    
    
    /*
     * Applies the homomorphism to the given tree. Returns the homomorphic image
     * of the tree under this homomorphism.
     * 
     */
    public Tree<Integer> applyRaw(Tree<Integer> tree) {
//        final Map<String, String> knownGensyms = new HashMap<String, String>();

        return tree.dfs(new TreeVisitor<Integer, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<Integer> node, List<Tree<Integer>> childrenValues) {
                Tree<Integer> ret = constructRaw(get(node.getLabel()), childrenValues);
                if (debug) {
                    System.err.println("\n" + node + ":");
                    System.err.println("  " + rhsAsString(get(node.getLabel())));
                    for (Tree<Integer> child : childrenValues) {
                        System.err.println("   + " + child);
                    }
                    System.err.println("  => " + ret);
                }
                return ret;
            }
        });
    }
    
    public Tree<String> apply(Tree<String> tree) {
        return getTargetSignature().resolve(applyRaw(getSourceSignature().addAllSymbols(tree)));
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
    private Tree<Integer> constructRaw(final Tree<HomomorphismSymbol> tree, final List<Tree<Integer>> subtrees) {
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
    
//    public Tree<String> construct(Tree<String> tree, List<Tree<)

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
    
    public String toStringCondensed() {
        StringBuilder buf = new StringBuilder();
        buf.append("Labelsets mapped to terms in Homomorphism:\n");
        for (IntSet labels : labelSetInterner.getSymbolTable().keySet()) {
            StringBuilder labelSetStrings = new StringBuilder();
            labelSetStrings.append("{");
            for (int label : labels) {
                labelSetStrings.append(srcSignature.resolveSymbolId(label)).append(",");
            }
            labelSetStrings.setLength(labelSetStrings.length()-1);
            buf.append(labelSetStrings.toString()).append("} -> ").append(rhsAsString(get(labels))).append("\n");
        }
        return buf.toString();
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        for (int key : labelToLabelSet.keySet()) {
            buf.append(srcSignature.resolveSymbolId(key)).append(" -> ").append(rhsAsString(get(key))).append("\n");
        }

        return buf.toString();
    }

    public String rhsAsString(Tree<HomomorphismSymbol> t) {
        Tree<String> resolvedTree = HomomorphismSymbol.toStringTree(t, tgtSignature);
//        
//        
//         resolvedTree = t.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<String>>() {
//            @Override
//            public Tree<String> combine(Tree<HomomorphismSymbol> node, List<Tree<String>> childrenValues) {
//                switch(node.getLabel().getType()) {
//                    case CONSTANT:
//                        return Tree.create(tgtSignature.resolveSymbolId(node.getLabel().getValue()), childrenValues);
//                    case VARIABLE:
//                        return Tree.create("?" + (node.getLabel().getValue()+1));
//                    default:
//                        return Tree.create("***");
//                }
//            }
//        });
//        
        resolvedTree.setCachingPolicy(false);
        
        try {
        return resolvedTree.toString(HOM_NON_QUOTING_PATTERN);
        } catch(Exception e) {
            return null;
        }
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
            Homomorphism other = (Homomorphism) obj;
            
            int[] sourceRemap = srcSignature.remap(other.srcSignature);
            int[] targetRemap = tgtSignature.remap(other.tgtSignature);
            
            if( labelToLabelSet.size() != other.labelToLabelSet.size() ) {
                return false;
            }
            
            for( int srcSym : labelToLabelSet.keySet() ) {
                if( sourceRemap[srcSym] == 0 ) {
                    return false;
                }
                
                Tree<HomomorphismSymbol> thisRhs = get(srcSym);
                Tree<HomomorphismSymbol> otherRhs = other.get(sourceRemap[srcSym]);
                
                if( ! equalRhsTrees(thisRhs, otherRhs, targetRemap)) {
                    return false;
                }
            }
            
            return true;
        }

        return false;
    }
    
    private boolean equalRhsTrees(Tree<HomomorphismSymbol> thisRhs, Tree<HomomorphismSymbol> otherRhs, int[] targetRemap) {
        if( thisRhs.getLabel().getType() != otherRhs.getLabel().getType() ) {
            return false;
        }
        
        switch(thisRhs.getLabel().getType()) {
            case CONSTANT:
                if( targetRemap[thisRhs.getLabel().getValue()] != otherRhs.getLabel().getValue() ) {
                    return false;
                }
                break;
                
            case VARIABLE:
                if( thisRhs.getLabel().getValue() != otherRhs.getLabel().getValue() ) {
                    return false;
                }
        }
        
        if( thisRhs.getChildren().size() != otherRhs.getChildren().size() ) {
            return false;
        }
        
        for( int i = 0; i < thisRhs.getChildren().size(); i++ ) {
            if( ! equalRhsTrees(thisRhs.getChildren().get(i), otherRhs.getChildren().get(i), targetRemap)) {
                return false;
            }
        }
        
        return true;
    }
    
    

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isNonDeleting() {
        for (int label : labelToLabelSet.keySet()) {
            Tree<HomomorphismSymbol> rhs = get(label);
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
