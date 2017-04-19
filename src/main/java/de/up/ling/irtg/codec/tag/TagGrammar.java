/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.tag;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.FeatureStructure;
import de.saar.coli.featstruct.PlaceholderFeatureStructure;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.FeatureStructureAlgebra;
import de.up.ling.irtg.algebra.TagStringAlgebra;
import de.up.ling.irtg.algebra.TagTreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author koller
 */
public class TagGrammar {

    private static final String NO_ADJUNCTION = "*NOP*";
    private static final char SUBST_VARTYPE = 'S';
    private static final char ADJ_VARTYPE = 'A';
    private static final String SUBST_SUFFIX = "_" + SUBST_VARTYPE;

    private final Map<String, ElementaryTree> trees;   // tree-name -> elementary-tree
    private final SetMultimap<String, LexiconEntry> lexicon; // word -> set(tree-name)

    public TagGrammar() {
        trees = new HashMap<>();
        lexicon = HashMultimap.create();
    }

    public void addElementaryTree(String name, ElementaryTree tree) {
        trees.put(name, tree);
    }

    public ElementaryTree getElementaryTree(String name) {
        return trees.get(name);
    }

    public void addLexiconEntry(String word, LexiconEntry lex) {
        lexicon.put(word, lex);
    }

    public Collection<String> getWords() {
        return lexicon.keySet();
    }

    // makes Chen-specific assumptions
    public Collection<ElementaryTree> lexicalizeElementaryTrees(String word) {
        List<ElementaryTree> ret = new ArrayList<>();

        if (lexicon.containsKey(word)) {
            for (LexiconEntry lex : lexicon.get(word)) {
                ElementaryTree et = trees.get(lex.getElementaryTreeName());

                if (et == null) {
                    System.err.println("*** UNK ET: " + lex + " for word " + word + "***");
                } else {
                    ret.add(et.lexicalize(word, (String) lex.getFeatureStructure().get("pos").getValue(), lex.getSecondaryLex()));
                }
            }
        }

        return ret;
    }

    public InterpretedTreeAutomaton toIrtg() {
        return toIrtg("S");
    }

    public InterpretedTreeAutomaton toIrtg(String startSymbol) {
        // check whether this is a TAG with feature structures
        boolean hasFeatureStructures = trees.values().iterator().next().hasFeatureStructures();

        for (Map.Entry<String, ElementaryTree> et : trees.entrySet()) {
            if (et.getValue().hasFeatureStructures() != hasFeatureStructures) {
                throw new RuntimeException("Either all or elementary trees in the grammar must have features; tree " + et.getKey() + " is different.");
            }
        }

        // set up IRTG
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);

        TagStringAlgebra tsa = new TagStringAlgebra();
        Homomorphism sh = new Homomorphism(auto.getSignature(), tsa.getSignature());
        irtg.addInterpretation("string", new Interpretation(tsa, sh));

        TagTreeAlgebra tta = new TagTreeAlgebra();
        Homomorphism th = new Homomorphism(auto.getSignature(), tta.getSignature());
        irtg.addInterpretation("tree", new Interpretation(tta, th));

        FeatureStructureAlgebra fsa = null;
        Homomorphism fh = null;

        if (hasFeatureStructures) {
            fsa = new FeatureStructureAlgebra();
            fh = new Homomorphism(auto.getSignature(), fsa.getSignature());
            irtg.addInterpretation("ft", new Interpretation(fsa, fh));
        }

        auto.addFinalState(auto.addState(makeS(startSymbol)));

        // convert elementary trees
        Set<String> adjunctionNonterminals = new HashSet<>();
        for (String word : getWords()) {
            for (LexiconEntry lex : lexicon.get(word)) {
                convertElementaryTree(lex, auto, th, sh, tsa, fh, adjunctionNonterminals);
            }
        }

        // add rules for empty adjunctions
        for (String nt : adjunctionNonterminals) {
            String sym = makeNop(nt);
            auto.addRule(auto.createRule(nt, sym, Collections.EMPTY_LIST));

            th.add(sym, Tree.create(TagTreeAlgebra.P1));
            sh.add(sym, Tree.create(TagStringAlgebra.EE()));

            if (hasFeatureStructures) {
                fh.add(sym, Tree.create("[foot: #1, root: #1]"));
            }
        }

        return irtg;
    }

    public static String makeNop(String nt) {
        return NO_ADJUNCTION + "_" + nt;
    }

    public static String makeTerminalSymbol(LexiconEntry lex) {
        FeatureStructure fs = lex.getFeatureStructure();
        String ret = lex.getElementaryTreeName() + "-" + lex.getWord();

        if (fs == null) {
            return ret;
        } else {
            return ret + safe(fs.toString());
        }
    }

    private static String safe(String s) {
        return s.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private static String makeA(String nonterminal) {
        return nonterminal + "_" + ADJ_VARTYPE;
    }

    private static String makeS(String nonterminal) {
        return nonterminal + "_" + SUBST_VARTYPE;
    }

    private static HomomorphismSymbol lwa(String label, int arity, Homomorphism th) {
        return th.c(label + "_" + arity, arity);
    }

    /**
     * Generates the list of child states, in the same order as
     * {@link #convertElementaryTree(de.up.ling.irtg.codec.tag.LexiconEntry, de.up.ling.irtg.automata.ConcreteTreeAutomaton, de.up.ling.irtg.hom.Homomorphism, de.up.ling.irtg.hom.Homomorphism, de.up.ling.irtg.algebra.TagStringAlgebra, java.util.Set) }.
     *
     * @param etree
     * @return
     */
    List<String> getChildStates(String etreeName) { // ElementaryTree etree
        final List<String> childStates = new ArrayList<>();
        LexiconEntry lex = new LexiconEntry(null, etreeName);

        dfsEtree(lex, null, childStates, null, new ElementaryTreeVisitor<Void>() {
             @Override
             public Void makeAdjTree(Node node, List<Void> children, MutableInteger nextVar, Homomorphism th, List<String> childStates, Set<String> adjunctionNonterminals) {
                 childStates.add(makeA(node.getLabel()));
                 return null;
             }

             @Override
             public Void makeSubstTree(Node node, MutableInteger nextVar, Homomorphism th, List<String> childStates) {
                 childStates.add(makeS(node.getLabel()));
                 return null;
             }

             @Override
             public Void makeDummyTree(Node node, Homomorphism th) {
                 return null;
             }

             @Override
             public Void makeAtom(String s, Homomorphism th) {
                 return null;
             }
         });

        return childStates;
    }

    /**
     * Returns a tree of the same shape as the elementary tree, with each node
     * replaced by its post-order visit index, starting at nextPosition for the
     * leftmost leaf. This is the same order in which {@link #convertElementaryTree(de.up.ling.irtg.codec.tag.LexiconEntry, de.up.ling.irtg.automata.ConcreteTreeAutomaton, de.up.ling.irtg.hom.Homomorphism, de.up.ling.irtg.hom.Homomorphism, de.up.ling.irtg.algebra.TagStringAlgebra, java.util.Set)
     * }
     * and {@link #getChildStates(de.up.ling.irtg.codec.tag.ElementaryTree) }
     * generate the lists of child states.
     *
     * The node label -1 indicates that this node does not generate a child
     * state.
     *
     * @param tree
     * @param nextPosition
     * @return
     */
    Tree<Integer> makeDfsNodePositions(String etreeName, MutableInteger nextPosition) {
        LexiconEntry lex = new LexiconEntry(null, etreeName);

        System.err.printf("node pos for %s:\n", etreeName);

        return dfsEtree(lex, null, null, null, new ElementaryTreeVisitor<Tree<Integer>>() {
                    @Override
                    public Tree<Integer> makeAdjTree(Node node, List<Tree<Integer>> children, MutableInteger nextVar, Homomorphism th, List<String> childStates, Set<String> adjunctionNonterminals) {
                        int ret = nextPosition.incValue();
                        System.err.printf("[a] %s -> %s\n", node, Tree.create(ret, children));
                        return Tree.create(ret, children);
                    }

                    @Override
                    public Tree<Integer> makeSubstTree(Node node, MutableInteger nextVar, Homomorphism th, List<String> childStates) {
                        int ret = nextPosition.incValue();
                        System.err.printf("[s] %s -> %s\n", node, Tree.create(ret));

                        return Tree.create(ret); // , children
                    }

                    @Override
                    public Tree<Integer> makeDummyTree(Node node, Homomorphism th) {
                        System.err.printf("[d] %s -> -1\n", node);
                        return Tree.create(-1);
                    }

                    @Override
                    public Tree<Integer> makeAtom(String s, Homomorphism th) {
                        System.err.printf("[atom] %s -> -1\n", s);
                        return Tree.create(-1);
                    }
                });

        /*
        return tree.getTree().dfs((node, children) -> {
            switch (node.getLabel().getType()) {
                case HEAD:
                case SECONDARY_LEX:
                case SUBSTITUTION:
                    return Tree.create(nextPosition.incValue(), children);

                case DEFAULT:
                    if (isTrace(node.getLabel().getLabel())) {
                        // do not allow adjunction around traces
                        return Tree.create(-1);
                    } else {
                        return Tree.create(nextPosition.incValue(), children);
                    }

                default:
                    return Tree.create(-1, children);
            }
        });
         */
    }

    private static final String ADJ_PREFIX = "?" + ADJ_VARTYPE;
    private static final String SUBST_PREFIX = "?" + SUBST_VARTYPE;

    private static interface ElementaryTreeVisitor<E> {
        public E makeAdjTree(Node node, List<E> children, MutableInteger nextVar, Homomorphism th, List<String> childStates, Set<String> adjunctionNonterminals);

        public E makeSubstTree(Node node, MutableInteger nextVar, Homomorphism th, List<String> childStates);

        public E makeDummyTree(Node node, Homomorphism th);

        public E makeAtom(String s, Homomorphism th);
    }

    private static class HomConstructingEtreeVisitor implements ElementaryTreeVisitor<Tree<HomomorphismSymbol>> {
        @Override
        public Tree<HomomorphismSymbol> makeAdjTree(Node node, List<Tree<HomomorphismSymbol>> children, MutableInteger nextVar, Homomorphism th, List<String> childStates, Set<String> adjunctionNonterminals) {
            String label = node.getLabel();

            childStates.add(makeA(label));
            adjunctionNonterminals.add(makeA(label));
            return Tree.create(th.c(TagTreeAlgebra.C, 2),
                               Tree.create(th.v(nextVar.gensym(ADJ_PREFIX))),
                               Tree.create(lwa(label, children.size(), th), children));
        }

        @Override
        public Tree<HomomorphismSymbol> makeSubstTree(Node node, MutableInteger nextVar, Homomorphism th, List<String> childStates) {
            String label = node.getLabel();
            childStates.add(makeS(label));
            return Tree.create(th.v(nextVar.gensym(SUBST_PREFIX)));
        }

        @Override
        public Tree<HomomorphismSymbol> makeDummyTree(Node node, Homomorphism th) {
            return Tree.create(lwa(node.getLabel(), 0, th));
        }

        @Override
        public Tree<HomomorphismSymbol> makeAtom(String s, Homomorphism th) {
            return Tree.create(th.c(s));
        }
    }

    private <E> E dfsEtree(LexiconEntry lex, Homomorphism th, List<String> childStates, final Set<String> adjunctionNonterminals, ElementaryTreeVisitor<E> visitor) {
        ElementaryTree etree = trees.get(lex.getElementaryTreeName());
        MutableInteger nextVar = new MutableInteger(1);

        if (etree != null) {
            return etree.getTree().dfs((node, children) -> {
                switch (node.getLabel().getType()) {
                    case HEAD:
                        return visitor.makeAdjTree(node.getLabel(), Collections.singletonList(visitor.makeAtom(lex.getWord(), th)), nextVar, th, childStates, adjunctionNonterminals);

                    case SECONDARY_LEX:
                        return visitor.makeAdjTree(node.getLabel(), Collections.singletonList(visitor.makeAtom(lex.getSecondaryLex(), th)), nextVar, th, childStates, adjunctionNonterminals);

                    case FOOT:
                        return visitor.makeAtom(TagTreeAlgebra.P1, th);

                    case SUBSTITUTION:
                        return visitor.makeSubstTree(node.getLabel(), nextVar, th, childStates);

                    case DEFAULT:
                        if (isTrace(node.getLabel().getLabel())) {
                            // do not allow adjunction around traces
                            return visitor.makeDummyTree(node.getLabel(), th);
                        } else {
                            return visitor.makeAdjTree(node.getLabel(), children, nextVar, th, childStates, adjunctionNonterminals);
                        }

                    default:
                        throw new CodecParseException("Illegal node type in " + lex + ": " + etree.getTree().getLabel());
                }
            });
        } else {
            return null;
        }
    }

    /**
     * NB: Keep this consistent with {@link #makeDfsNodePositions(de.up.ling.irtg.codec.tag.ElementaryTree, de.up.ling.irtg.util.MutableInteger)
     * }
     * and {@link #getChildStates(de.up.ling.irtg.codec.tag.ElementaryTree) }.
     *
     * @param lex
     * @param auto
     * @param th
     * @param sh
     * @param tsa
     * @param adjunctionNonterminals
     */
    private void convertElementaryTree(LexiconEntry lex, ConcreteTreeAutomaton<String> auto, Homomorphism th, Homomorphism sh, TagStringAlgebra tsa, Homomorphism fh, final Set<String> adjunctionNonterminals) {
        final List<String> childStates = new ArrayList<>();
        ElementaryTree etree = trees.get(lex.getElementaryTreeName());
        String terminalSym = makeTerminalSymbol(lex);

        // null etree means that no elementary tree of that name was defined
        // in the grammar. An example is the dummy "tCO" tree from the Chen
        // PTB-TAG. We ignore these lexicon entries.
        if (etree != null) {
            Tree<HomomorphismSymbol> treeHomTerm = dfsEtree(lex, th, childStates, adjunctionNonterminals, new HomConstructingEtreeVisitor());
            int terminalSymId = auto.getSignature().addSymbol(terminalSym, childStates.size()); //nextVar.getValue() - 1);
            String parentState = (etree.getType() == ElementaryTreeType.INITIAL) ? makeS(etree.getRootLabel()) : makeA(etree.getRootLabel());
            auto.addRule(auto.createRule(parentState, terminalSym, childStates));
            th.add(terminalSymId, treeHomTerm);
            sh.add(terminalSymId, makeStringHom(treeHomTerm, th, sh, tsa, childStates));

            // convert feature structures
            if (fh != null) {
                AvmFeatureStructure rootMaker = new AvmFeatureStructure();
                PlaceholderFeatureStructure rootMakerPlaceholder = new PlaceholderFeatureStructure("root");
                rootMaker.put("root", rootMakerPlaceholder);

                AvmFeatureStructure fsForEtree = new AvmFeatureStructure();
                List<String> nodeIdsForChildren = new ArrayList<>();
                SameIndexMerger mergeSameIndices = new SameIndexMerger();

                // collect top and bottom feature structures for the etree nodes
                MutableInteger nextNodename = new MutableInteger(1);
                etree.getTree().dfs((nodeInTree, children) -> {
                    Node node = nodeInTree.getLabel();

                    // create nodeId -> attribute in AVM
                    String nodeId = null;
                    if (node.getType() == NodeType.FOOT) {
                        nodeId = "foot";
                    } else {
                        nodeId = "n" + nextNodename.incValue();
                    }

                    // if root, coindex with root attribute
                    if (nodeInTree == etree.getTree()) {
                        rootMaker.put(nodeId + "t", rootMakerPlaceholder);
                    }

                    switch (node.getType()) {
                        case HEAD:
                        case SECONDARY_LEX:
                        case DEFAULT:
                            // nodes that generate adjunction childen
                            if (node.getType() != NodeType.DEFAULT || !isTrace(node.getLabel())) {
                                nodeIdsForChildren.add(nodeId);

                                FeatureStructure bottom = fsn(node.getBottom());
                                if (node.getType() == NodeType.HEAD) {
                                    // put lexical feature structure here
                                    bottom = bottom.unify(lex.getFeatureStructure());
                                }

                                fsForEtree.put(nodeId + "t", fsn(node.getTop()));
                                fsForEtree.put(nodeId + "b", bottom);

                                mergeSameIndices.collect(nodeId + "t", node.getTop());
                                mergeSameIndices.collect(nodeId + "b", node.getBottom());
                            }
                            break;

                        case FOOT:
                            fsForEtree.put(nodeId, fsn(node.getTop()));
                            mergeSameIndices.collect(nodeId, node.getTop());
                            break;

                        case SUBSTITUTION:
                            nodeIdsForChildren.add(nodeId);
                            fsForEtree.put(nodeId, fsn(node.getTop()));
                            mergeSameIndices.collect(nodeId, node.getTop());
                            break;

                        default:

                    }

                    return null;
                });

                // make sure root attribute points to correct value
                // and enforce coindexation across different nodes in same etree
                final FeatureStructure coreFs = mergeSameIndices.unify(fsForEtree.unify(rootMaker));

                // construct homomorphism term, ensuring that it has the same
                // structure as the other terms so the rule can be binarized
                int footNodeId = th.getTargetSignature().getIdForSymbol(TagTreeAlgebra.P1);

                Tree<String> h = treeHomTerm.dfs((nodeInTree, children) -> {
                    if (children.isEmpty()) {
                        // leaf
                        if (nodeInTree.getLabel().isVariable()) {
                            // variable
                            int index = nodeInTree.getLabel().getValue();
                            String nodeId = nodeIdsForChildren.get(index);
                            boolean isAuxChild = !isSubstitutionVariable(childStates.get(index));

                            if (isAuxChild) {
                                return Tree.create("emba_" + nodeId + "t_" + nodeId + "b", Tree.create("?" + (index + 1)));
                            } else {
                                return Tree.create("emb_" + nodeId, Tree.create("proj_root", Tree.create("?" + (index + 1))));
                            }
                        } else {
                            HomomorphismSymbol label = nodeInTree.getLabel();

                            if (label.getValue() == footNodeId) {
                                // foot node => return dummy FS
                                return Tree.create("[]");
                            } else {
                                // constant for the lexical anchor
                                // NB this may not be entirely accurate if the TAG grammar is not
                                // strongly lexicalized, i.e. can have multiple words per e-tree

                                return Tree.create(coreFs.toString());
                            }
                        }
                    } else {
                        // Unification is commutative, so the order of children
                        // should not matter. We'll try to push the coreFs as far
                        // to the left as we can, to make the string representation
                        // more readable.                        
                        Tree<String> ret = children.get(children.size() - 1);

                        for (int i = children.size() - 2; i >= 0; i--) {
                            ret = Tree.create("unify", ret, children.get(i));
                        }

                        return ret;
                    }
                });

                fh.add(terminalSym, h);
            }
        }
    }

    private static class SameIndexMerger {

        private Map<String, FeatureStructure> placeholderForIndex = new HashMap<>();  // a unique placeholder for each index

        // This is successively built up to contain [n27_b: [foo: [bar: ... <PH #ix>]]],
        // where <PH #ix> is a placeholder FS for the index #ix which is unique
        // across the different nodes of the etree. In this way, uses of the same index
        // in different nodes in the etree will be unified.
        AvmFeatureStructure merger = new AvmFeatureStructure();

        public void collect(String attribute, FeatureStructure fs) {
            if (fs != null) {
                Set<String> alreadyCollectedIndices = new HashSet<>();

                for (List<String> path : fs.getAllPaths()) {
                    FeatureStructure endpoint = fs.get(path);
                    String index = endpoint.getIndex();

                    if (index != null) {
                        if (!alreadyCollectedIndices.contains(index)) {
                            FeatureStructure placeholder = placeholderForIndex.get(index);

                            if (placeholder == null) {
                                placeholder = new PlaceholderFeatureStructure(index);
                                placeholderForIndex.put(index, placeholder);
                            }

                            merger.put(attribute, fsWithPath(path, 0, placeholder));
                        }
                    }
                }
            }
        }

        public FeatureStructure unify(FeatureStructure fs) {
            return fs.unify(merger);
        }

        private static FeatureStructure fsWithPath(List<String> path, int pos, FeatureStructure placeholder) {
            if (pos == path.size()) {
                return placeholder;
            } else {
                AvmFeatureStructure ret = new AvmFeatureStructure();
                ret.put(path.get(pos), fsWithPath(path, pos + 1, placeholder));
                return ret;
            }
        }
    }

    private static FeatureStructure fsn(FeatureStructure fs) {
        if (fs == null) {
            return new AvmFeatureStructure();
        } else {
            return fs;
        }
    }

    private static class SortedTree {

        public Tree<HomomorphismSymbol> tree;
        public int sort;

        public SortedTree(Tree<HomomorphismSymbol> tree, int sort) {
            this.tree = tree;
            this.sort = sort;
        }
    }

    private static SortedTree cs(String label, List<SortedTree> children, Homomorphism sh, TagStringAlgebra tsa) {
        List<Tree<HomomorphismSymbol>> childTrees = Util.mapToList(children, st -> st.tree);
        HomomorphismSymbol labelHS = sh.c(label, childTrees.size());
        return new SortedTree(Tree.create(labelHS, childTrees), tsa.getSort(labelHS.getValue()));
    }

    private static boolean isSubstitutionVariable(String nonterminal) {
        return nonterminal.endsWith(SUBST_SUFFIX);
    }

    private Predicate<String> traceP = null;

    /**
     * Set a predicate which checks whether a leaf is a trace. Leaves whose
     * labels match this condition are replaced by *E* when constructing the
     * string homomorphism in {@link #toIrtg() }.
     *
     * @param traceP
     */
    public void setTracePredicate(Predicate<String> traceP) {
        this.traceP = traceP;
    }

    /**
     * Checks whether the given nodeLabel is a trace, according to the trace
     * predicate set by {@link #setTracePredicate(java.util.function.Predicate)
     * }.
     *
     * @param nodeLabel
     * @return
     */
    public boolean isTrace(String nodeLabel) {
        return (traceP != null) && traceP.test(nodeLabel);
    }

    private Tree<HomomorphismSymbol> makeStringHom(Tree<HomomorphismSymbol> treeForTreeHom, Homomorphism th, Homomorphism sh, TagStringAlgebra tsa, List<String> childStates) {
        SortedTree t = treeForTreeHom.dfs((node, children) -> {
            if (node.getLabel().isVariable()) {
                assert children.isEmpty();

//                System.err.printf("msh @ %s\n", HomomorphismSymbol.toStringTree(node, th.getTargetSignature()));
//                System.err.printf("  - childStates = %s\n", childStates);
                if (isSubstitutionVariable(childStates.get(node.getLabel().getValue()))) {
                    return new SortedTree(Tree.create(node.getLabel()), 1);
                } else {
                    return new SortedTree(Tree.create(node.getLabel()), 2);
                }
            } else {
                String label = th.getTargetSignature().resolveSymbolId(node.getLabel().getValue());
                assert label != null;

                if (TagTreeAlgebra.C.equals(label)) {
                    assert children.size() == 2;
                    return cs(TagStringAlgebra.WRAP(children.get(0).sort, children.get(1).sort), children, sh, tsa);
                } else if (TagTreeAlgebra.P1.equals(label)) {
                    assert children.isEmpty();
                    return cs(TagStringAlgebra.EE(), children, sh, tsa);
                } else if (isTrace(label)) {
                    return cs(TagStringAlgebra.E(), children, sh, tsa);
                } else {
                    switch (children.size()) {
                        case 0:
                            return new SortedTree(Tree.create(sh.c(label)), 1);

                        case 1:
                            return children.get(0);

                        default:
                            return concatenateMany(children, 0, sh, tsa);
                    }
                }
            }
        });

        return t.tree;
    }

    private static SortedTree concatenateMany(List<SortedTree> children, int pos, Homomorphism sh, TagStringAlgebra tsa) {
        SortedTree left = children.get(pos);
        SortedTree right = null;

        if (pos == children.size() - 2) {
            right = children.get(pos + 1);
        } else {
            right = concatenateMany(children, pos + 1, sh, tsa);
        }

        List<SortedTree> processedChildren = Lists.newArrayList(left, right);

        SortedTree ret = cs(TagStringAlgebra.CONCAT(left.sort, right.sort), processedChildren, sh, tsa);

        int symid = ret.tree.getLabel().getValue();
        assert symid != 0;
        assert sh.getTargetSignature().resolveSymbolId(symid) != null : "could not resolve symid " + symid + " in signature " + sh.getTargetSignature();

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("ELEMENTARY TREES:\n");
        buf.append(Joiner.on("\n").withKeyValueSeparator(" = ").join(trees));

        buf.append("\nLEXICON ENTRIES:\n");
        buf.append(Joiner.on("\n").withKeyValueSeparator(" = ").join(lexicon.asMap()));

        return buf.toString();
    }
}

/*
            Tree<HomomorphismSymbol> treeHomTerm = etree.getTree().dfs((node, children) -> {
                String label = node.getLabel().getLabel(); // use these as states
                Tree<HomomorphismSymbol> ret = null;

                switch (node.getLabel().getType()) {
                    case HEAD:
                        children = Collections.singletonList(Tree.create(th.c(lex.getWord())));
                        ret = makeAdjTree(node.getLabel(), children, nextVar, th, childStates, adjunctionNonterminals);
                        break;

                    case SECONDARY_LEX:
                        children = Collections.singletonList(Tree.create(th.c(lex.getSecondaryLex())));
                        ret = makeAdjTree(node.getLabel(), children, nextVar, th, childStates, adjunctionNonterminals);
                        break;
                    // TODO - maybe XTAG allows multiple secondary lexes, one per POS-tag

                    case FOOT:
                        ret = Tree.create(th.c(TagTreeAlgebra.P1));
                        break;

                    case SUBSTITUTION:
                        ret = makeSubstTree(node.getLabel(), nextVar, th, childStates);
//                        childStates.add(makeS(label));
//                        ret = Tree.create(th.v(nextVar.gensym(substPrefix)));
                        break;

                    case DEFAULT:
                        if (isTrace(label)) {
                            // do not allow adjunction around traces
                            ret = makeDummyTree(node.getLabel(), th);
//                            ret = Tree.create(lwa(label, 0, th));
                        } else {
                            ret = makeAdjTree(node.getLabel(), children, nextVar, th, childStates, adjunctionNonterminals);
                        }
                        break;

                    default:
                        throw new CodecParseException("Illegal node type in " + lex + ": " + etree.getTree().getLabel());
                }

                return ret;
            });
 */
//            System.err.printf("th: %s\n", HomomorphismSymbol.toStringTree(treeHomTerm, th.getTargetSignature()));
//            System.err.printf("childStates: %s\n", childStates);

