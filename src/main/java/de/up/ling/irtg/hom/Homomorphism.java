// Copyright 2012-2017 Alexander Koller
// Copyright 2013 baumgarten
// Copyright 2013 danilo
// Copyright 2014 Johannes Gontrum
// Copyright 2015-2016 Jonas Groschwitz
// Copyright 2017 Christoph Teichmann
// Copyright 2019 Arne Köhn
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package de.up.ling.irtg.hom;

import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A Homomorphism f defines a mapping from a signature A to terms over
 * a signature B. For a symbol y in A of arity k, the term f(y)
 * may contain variables x_1 to x_k. A homomorphism can map
 * a whole term t over A to a term f(t) over B recursively in
 * the following way: for each label
 * y in f(t), replace each variable x_i in f(y) with the result of the term at the i-th
 * position below y. I.e. if s is the i-th term below y in t, then replace
 * x_i in f(y) with f(s), and repeat this recursively.
 *
 * In an IRTG, A is the signature of the grammar and B is the signature
 * of the algebra (i.e. the algebra's operations). Thus, a homomorphism
 * maps a term of the grammar to a term over the algebra which then
 * can be evaluated. This way, the Homomorphism class is a central
 * part of the Interpretation class.
 */
public class Homomorphism implements Serializable {

    private static final long serialVersionUID = 8615572724774133330L;
	private static final Pattern HOM_NON_QUOTING_PATTERN = Pattern.compile("([a-zA-Z*+_]([a-zA-Z0-9_*+-]*))|([?]([0-9]+))");
    private final Signature srcSignature, tgtSignature;
    private static final boolean debug = false;

    private final List<Tree<HomomorphismSymbol>> terms;    // Maps an ID to each term
    private final Object2IntMap<Tree<HomomorphismSymbol>> termToId; // maps term to ID
    private final Int2IntMap labelToLabelSet; // Find the labelSet for a given label
    private final List<IntSet> labelSetList;  // List of labelSets. Their index is their labelSet OD

    private Object2IntMap<IntSet> labelSetToLabelID;  // maps a label set to its ID; only computed by need
    private boolean labelSetsDirty;                   // is current value of labelSetToLabelID valid?

    private final Int2ObjectMap<IntSet> srcSymbolToRhsSymbols;  // maps a labelset ID f to the set of symbols in hom(f)

    private SignatureMapper signatureMapper;

    public Homomorphism(Signature src, Signature tgt) {
        srcSignature = src;
        tgtSignature = tgt;

        terms = new ArrayList<>();
        termToId = new Object2IntOpenHashMap<>();
        labelToLabelSet = new Int2IntOpenHashMap();
        labelSetList = new ArrayList<>();

        srcSymbolToRhsSymbols = new Int2ObjectOpenHashMap<>();

        terms.add(null);
        labelSetList.add(null); // dummies to ensure that IDs start at 1 (so 0 can be used for "not found")

        labelSetsDirty = true;
        signatureMapper = null;
    }

    public void add(String label, Tree<String> mapping) {
        add(srcSignature.getIdForSymbol(label), HomomorphismSymbol.treeFromNames(mapping, tgtSignature));
    }

    public void add(int label, Tree<HomomorphismSymbol> mapping) {
        labelSetsDirty = true;

        if (termToId.containsKey(mapping)) {
            // Term is already processed. We only need to add the given label to the proper labelSet
            if (debug) {
                System.err.println("-> " + mapping + " is already known.");
            }

            int labelSetID = termToId.getInt(mapping);  // get existing termID
            addToLabelSet(label, labelSetID);           // put the current label in the labelSet for this term.
            labelToLabelSet.put(label, labelSetID);     // Add the mapping from the label to the corresponding labelSet
        } else {
            // This is the first time we see the term 'mapping'

            terms.add(mapping);      // Create an ID for the term from the term interner

            int labelSetID = createNewLabelSet(label);      // Create a new labelSet and the ID for it
            termToId.put(mapping, labelSetID);

            labelToLabelSet.put(label, labelSetID);     // Map the used label to its labelSetID

            // record the symbols in the term
            IntSet allSymbols = new IntOpenHashSet();
            collectAllSymbols(mapping, allSymbols);
            srcSymbolToRhsSymbols.put(labelSetID, allSymbols);
        }
    }

    private void collectAllSymbols(Tree<HomomorphismSymbol> tree, IntSet allSymbols) {
        if (tree.getLabel().isConstant()) {
            allSymbols.add(tree.getLabel().getValue());
        }

        for (Tree<HomomorphismSymbol> sub : tree.getChildren()) {
            collectAllSymbols(sub, allSymbols);
        }
    }

    private IntSet getLabelSet(int labelSetID) {
        IntSet ret = labelSetList.get(labelSetID);
        if (ret != null) {
            return ret;
        } else {
            return new IntOpenHashSet();
        }
    }

    // Adds a label to an existing labelSet.
    private void addToLabelSet(int label, int labelSetID) {
        IntSet labelSet = getLabelSet(labelSetID);  // Get the actual labelset
        labelSet.add(label);                        // Now change the content of the set

        if (debug) {
            System.err.println("labelSet = " + labelSet);
        }

        if (debug) {
            System.err.println("labelSet\\ = " + labelSet);
        }
    }

  /**
   *Creates a new labelSet for a new label and returns the labelSetID
   */
    private int createNewLabelSet(int label) {
        IntSet labelSet = new IntOpenHashSet();
        labelSet.add(label);            // put first element in set
        labelSetList.add(labelSet);     // add set to the list
        int labelSetID = labelSetList.size() - 1; // = the position in the list

        if (debug) {
            System.err.println("labelSetID = " + labelSetID);
        }

        return labelSetID;
    }

    /**
     * Returns the value h(label), using symbol IDs.
     */
    public Tree<HomomorphismSymbol> get(int label) {
        if (debug) {
            System.err.println("Getting mapping for " + label);
        }

        int termID = getTermID(label);

        if (termID == 0) {
            return null;
        } else {
            return terms.get(termID);
        }
    }

    public Tree<HomomorphismSymbol> getByLabelSetID(int labelSetID) {
        return terms.get(labelSetID);
    }

    public IntSet getLabelSetByLabelSetID(int labelSetID) {
        assert labelSetID < labelSetList.size();
        return labelSetList.get(labelSetID);
    }

    private void ensureCleanLabelSets() {
        if (labelSetsDirty) {
            labelSetToLabelID.clear();

            for (int i = 1; i < labelSetList.size(); i++) {
                labelSetToLabelID.put(labelSetList.get(i), i);
            }

            labelSetsDirty = false;
        }
    }

    public int getLabelSetIDByLabelSet(IntSet labelSet) {
        ensureCleanLabelSets();
        return labelSetToLabelID.getInt(labelSet);
    }

    public int getTermID(int label) {
        return labelToLabelSet.get(label);
    }

    /**
     * This method is deprecated. Use {@link #getTermID(int) } instead.
     * @deprecated
     */
    @Deprecated
    public int getLabelSetID(int label) {
        return labelToLabelSet.get(label);
    }

    public IntSet getLabelSetForLabel(int label) {
        return getLabelSet(labelToLabelSet.get(label));
    }

    // valid label set IDs: 1 .. maxLabelSetID (inclusive)
    public int getMaxLabelSetID() {
        return labelSetList.size() - 1;
    }

    /**
     * Returns the value h(label). The label is resolved according to the
     * homomorphism's source signature, and is expected to be known there. The
     * labels in the returned tree are elements of the homomorphism's target
     * signature. If necessary, the returned tree can be converted back to a
     * tree of HomomorphismSymbols using HomomorphismSymbol.treeFromNames and the
     * homomorphism's target signature.
     */
    public Tree<String> get(String label) {
        if (debug) {
            System.err.println("Getting for " + label);
        }
        return HomomorphismSymbol.toStringTree(get(srcSignature.getIdForSymbol(label)), tgtSignature);
    }

    public IntCollection getLabelsetIDsForTgtSymbols(IntSet tgtSymbols) {
        IntList ret = new IntArrayList();

        // TODO - this might be inefficient, use better data structure
//        Logging.get().fine(() -> "tgt sig: " + getTargetSignature());
//        Logging.get().fine("src sig: " + getSourceSignature());
//        Logging.get().fine("labelset IDs: " + labelSetList);
        for (int srcSymbol : srcSymbolToRhsSymbols.keySet()) {
            if (tgtSymbols.containsAll(srcSymbolToRhsSymbols.get(srcSymbol))) {
                ret.add(srcSymbol);
            }
        }

        return ret;
    }

    /**
     * Applies the homomorphism to the given tree. Returns the homomorphic image
     * of the tree under this homomorphism.
     */
    public Tree<Integer> applyRaw(Tree<Integer> tree) {
        Tree<HomomorphismSymbol> homSym = get(tree.getLabel());
        Tree<Integer> ti = constructRaw(homSym, tree.getChildren());

        return ti;
    }

    @OperationAnnotation(code = "apply")
    public Tree<String> apply(Tree<String> tree) {
        if (tree == null) {
            return null;
        } else {
            return getTargetSignature().resolve(applyRaw(getSourceSignature().addAllSymbols(tree)));
        }
    }

    /**
     * Applies the homomorphism to a given input tree. Variables are substituted
     * according to the "subtrees" parameter: ?1, ?x1 etc. refer to the first
     * entry in the list, and so on.
     */
    private Tree<Integer> constructRaw(final Tree<HomomorphismSymbol> tree, final List<Tree<Integer>> subtrees) {
        HomomorphismSymbol label = tree.getLabel();
        switch (label.getType()) {
            case VARIABLE:
                return applyRaw(subtrees.get(label.getValue()));
            case CONSTANT:
                int size = tree.getChildren().size();
				@SuppressWarnings("unchecked")
                Tree<Integer>[] children = new Tree[size];

                for (int i = 0; i < size; ++i) {
                    children[i] = constructRaw(tree.getChildren().get(i), subtrees);
                }

                return Tree.create(label.getValue(), children);
            default:
                throw new RuntimeException("undefined homomorphism symbol type");
        }
    }

    public String toStringCondensed() {
        StringBuilder buf = new StringBuilder();

        for (int labelSetID = 1; labelSetID < labelSetList.size(); labelSetID++) {
            StringBuilder labelSetStrings = new StringBuilder();
            labelSetStrings.append(labelSetID);
            labelSetStrings.append(":{");
            for (int label : getLabelSetByLabelSetID(labelSetID)) {
                labelSetStrings.append(srcSignature.resolveSymbolId(label)).append(",");
            }
            labelSetStrings.setLength(labelSetStrings.length() - 1);
            buf.append(labelSetStrings.toString()).append("} -> ").append(rhsAsString(getByLabelSetID(labelSetID))).append("\n");
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
        resolvedTree.setCachingPolicy(false);

        try {
            return resolvedTree.toString(HOM_NON_QUOTING_PATTERN);
        } catch (Exception e) {
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

            if (labelToLabelSet.size() != other.labelToLabelSet.size()) {
                return false;
            }

            for (int srcSym : labelToLabelSet.keySet()) {
                if (sourceRemap[srcSym] == 0) {
                    return false;
                }

                Tree<HomomorphismSymbol> thisRhs = get(srcSym);
                Tree<HomomorphismSymbol> otherRhs = other.get(sourceRemap[srcSym]);

                if (!equalRhsTrees(thisRhs, otherRhs, targetRemap)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private boolean equalRhsTrees(Tree<HomomorphismSymbol> thisRhs, Tree<HomomorphismSymbol> otherRhs, int[] targetRemap) {
        if (thisRhs.getLabel().getType() != otherRhs.getLabel().getType()) {
            return false;
        }

        switch (thisRhs.getLabel().getType()) {
            case CONSTANT:
                if (targetRemap[thisRhs.getLabel().getValue()] != otherRhs.getLabel().getValue()) {
                    return false;
                }
                break;

            case VARIABLE:
                if (thisRhs.getLabel().getValue() != otherRhs.getLabel().getValue()) {
                    return false;
                }
        }

        if (thisRhs.getChildren().size() != otherRhs.getChildren().size()) {
            return false;
        }

        for (int i = 0; i < thisRhs.getChildren().size(); i++) {
            if (!equalRhsTrees(thisRhs.getChildren().get(i), otherRhs.getChildren().get(i), targetRemap)) {
                return false;
            }
        }

        return true;
    }

    private transient boolean isNondeletingComputed = false;
    private transient boolean isNondeleting = false;

    /**
     * A homomorphism is nondeleting iff in the image of every
     * symbol y all k variables are present, where k is the arity of y.
     */
    public boolean isNonDeleting() {
        if (isNondeletingComputed) {
            return isNondeleting;
        }
        isNondeleting = true;
        for (int label : labelToLabelSet.keySet()) {
            Tree<HomomorphismSymbol> rhs = get(label);
            Set<HomomorphismSymbol> variables = new HashSet<>();
            for (HomomorphismSymbol l : rhs.getLeafLabels()) {
                if (l.isVariable()) {
                    variables.add(l);
                }
            }
            if (variables.size() < srcSignature.getArity(label)) {
                isNondeleting = false;
                break;
            }
        }
        isNondeletingComputed = true;
        return isNondeleting;
    }

    /**
     * Returns a mapper that translates symbols of the source signature into
     * symbols of the target signature (and back). The mapper is computed by
     * need, when this method is called for the first time, and then reused.
     * Note that if one of the underlying signatures changes, you need to call
     * {@link SignatureMapper#recompute() } to update the mapper.
     */
    public SignatureMapper getSignatureMapper() {
        if (signatureMapper == null) {
            signatureMapper = srcSignature.getMapperTo(tgtSignature);
        }

        return signatureMapper;
    }

    public HomomorphismSymbol c(String constant) {
        return c(constant, 0);
    }

    public HomomorphismSymbol c(String constant, int arity) {
        return HomomorphismSymbol.createConstant(constant, tgtSignature, arity);
    }

    public HomomorphismSymbol v(String variable) {
        return HomomorphismSymbol.createVariable(variable);
    }
   
    
    
}
