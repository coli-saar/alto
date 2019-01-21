/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.tag;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
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
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.TulipacInputCodec;
import de.up.ling.irtg.codec.tulipac.TulipacLexer;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
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
        boolean hasFeatureStructures = trees.values().stream().anyMatch(t -> t.hasFeatureStructures());

        // set up IRTG
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);

        TagStringAlgebra tsa = new TagStringAlgebra();
        Homomorphism sh = new Homomorphism(auto.getSignature(), tsa.getSignature());
        irtg.addInterpretation(new Interpretation(tsa, sh, "string"));

        TagTreeAlgebra tta = new TagTreeAlgebra();
        Homomorphism th = new Homomorphism(auto.getSignature(), tta.getSignature());
        irtg.addInterpretation(new Interpretation(tta, th, "tree"));

        FeatureStructureAlgebra fsa = null;
        Homomorphism fh = null;

        if (hasFeatureStructures) {
            fsa = new FeatureStructureAlgebra();
            fh = new Homomorphism(auto.getSignature(), fsa.getSignature());
            irtg.addInterpretation(new Interpretation(fsa, fh, "ft"));
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

    public static String makeA(String nonterminal) {
        return nonterminal + "_" + ADJ_VARTYPE;
    }

    public static String makeS(String nonterminal) {
        return nonterminal + "_" + SUBST_VARTYPE;
    }

    private static HomomorphismSymbol lwa(String label, int arity, Homomorphism th) {
        return th.c(label + "_" + arity, arity);
    }

    private static final String ADJ_PREFIX = "?" + ADJ_VARTYPE;
    private static final String SUBST_PREFIX = "?" + SUBST_VARTYPE;

    public interface ElementaryTreeVisitor<E> {
        E makeAdjTree(Node node, List<E> children, MutableInteger nextVar, Homomorphism th, List<String> childStates, Set<String> adjunctionNonterminals);

        E makeSubstTree(Node node, MutableInteger nextVar, Homomorphism th, List<String> childStates);

        E makeNoAdjTree(Node node, List<E> children, Homomorphism th);

        E makeWordTree(String s, Homomorphism th);

        E makeFootTree(Homomorphism th);
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
        public Tree<HomomorphismSymbol> makeNoAdjTree(Node node, List<Tree<HomomorphismSymbol>> children, Homomorphism th) {
            return Tree.create(lwa(node.getLabel(), children.size(), th), children);
        }

        @Override
        public Tree<HomomorphismSymbol> makeWordTree(String s, Homomorphism th) {
            return Tree.create(th.c(s));
        }

        @Override
        public Tree<HomomorphismSymbol> makeFootTree(Homomorphism th) {
            return Tree.create(th.c(TagTreeAlgebra.P1));
        }
    }

    public <E> E dfsEtree(LexiconEntry lex, Homomorphism th, List<String> childStates, final Set<String> adjunctionNonterminals, ElementaryTreeVisitor<E> visitor) {
        ElementaryTree etree = trees.get(lex.getElementaryTreeName());
        MutableInteger nextVar = new MutableInteger(1);

        if (etree != null) {
            return etree.getTree().dfs((node, children) -> {
                switch (node.getLabel().getType()) {
                    case HEAD:
                        children = Collections.singletonList(visitor.makeWordTree(lex.getWord(), th));
                        if (node.getLabel().getAnnotation() == NodeAnnotation.NO_ADJUNCTION) {
                            return visitor.makeNoAdjTree(node.getLabel(), children, th);
                        } else {
                            return visitor.makeAdjTree(node.getLabel(), children, nextVar, th, childStates, adjunctionNonterminals);
                        }

                    case SECONDARY_LEX:
                        children = Collections.singletonList(visitor.makeWordTree(lex.getSecondaryLex(), th));
                        if (node.getLabel().getAnnotation() == NodeAnnotation.NO_ADJUNCTION) {
                            return visitor.makeNoAdjTree(node.getLabel(), children, th);
                        } else {
                            return visitor.makeAdjTree(node.getLabel(), children, nextVar, th, childStates, adjunctionNonterminals);
                        }
//                        return visitor.makeAdjTree(node.getLabel(), , nextVar, th, childStates, adjunctionNonterminals);

                    case FOOT:
                        return visitor.makeFootTree(th);

                    case SUBSTITUTION:
                        return visitor.makeSubstTree(node.getLabel(), nextVar, th, childStates);

                    case DEFAULT:
                        if (isTrace(node.getLabel().getLabel())) {
                            // do not allow adjunction around traces
                            List<E> e = Collections.EMPTY_LIST;
                            return visitor.makeNoAdjTree(node.getLabel(), e, th);
                        } else if (node.getLabel().getAnnotation() == NodeAnnotation.NO_ADJUNCTION) {
                            // do not allow adjunction at nodes with no-adjunction annotation
                            return visitor.makeNoAdjTree(node.getLabel(), children, th);
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
//            System.err.printf("collect attr=%s, fs=%s\n", attribute, fs == null ? "<null>" : fs);
            
            if (fs != null) {
                Set<String> alreadyCollectedIndices = new HashSet<>();
                AvmFeatureStructure avmForAttribute = new AvmFeatureStructure();

                for (List<String> path : fs.getAllPaths()) {
                    FeatureStructure endpoint = fs.get(path);
                    String index = endpoint.getIndex();
//                    System.err.printf("-> path %s, endpoint %s, index %s\n", path, endpoint, index);

                    if (index != null) {
                        if (!alreadyCollectedIndices.contains(index)) {
                            FeatureStructure placeholder = placeholderForIndex.get(index);

                            if (placeholder == null) {
                                placeholder = new PlaceholderFeatureStructure(index);
                                placeholderForIndex.put(index, placeholder);
                            }
                            
                            assert path.size() == 1;
                            avmForAttribute.put(path.get(0), placeholder);
//                            System.err.printf("   -> avmForAttribute is now %s\n", avmForAttribute);

                        }
                    }
                }
                
                merger.put(attribute, avmForAttribute);
//                System.err.printf(" -> merger is now %s\n", merger);
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

