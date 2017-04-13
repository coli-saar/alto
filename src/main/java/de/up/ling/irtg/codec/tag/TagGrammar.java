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
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
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

    public Collection<ElementaryTree> lexicalizeElementaryTrees(String word) {
        List<ElementaryTree> ret = new ArrayList<>();

        if (lexicon.containsKey(word)) {
            for (LexiconEntry lex : lexicon.get(word)) {
                ElementaryTree et = trees.get(lex.getElementaryTreeName());

                if (et == null) { /* && ! "tCO".equals(lex.getElementaryTreeName())) { 
                        adding the above to the if clause leads to NullPointerExceptions whenever
                        (et==null && "tCO".equals(lex.getElementaryTreeName())
                    */
                    System.err.println("*** UNK ET: " + lex + " for word " + word + "***");
                } else {
                    ret.add(et.lexicalize(word, lex.getFeature("pos"), lex.getSecondaryLex()));
                }
            }
        }

        return ret;
    }

    public InterpretedTreeAutomaton toIrtg() {
        // set up IRTG
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);

        TagStringAlgebra tsa = new TagStringAlgebra();
        Homomorphism sh = new Homomorphism(auto.getSignature(), tsa.getSignature());
        irtg.addInterpretation("string", new Interpretation(tsa, sh));

        TagTreeAlgebra tta = new TagTreeAlgebra();
        Homomorphism th = new Homomorphism(auto.getSignature(), tta.getSignature());
        irtg.addInterpretation("tree", new Interpretation(tta, th));

        auto.addFinalState(auto.addState(makeS("S")));

        // convert elementary trees
        Set<String> adjunctionNonterminals = new HashSet<>();
        for (String word : getWords()) {
            for (LexiconEntry lex : lexicon.get(word)) {
                convertElementaryTree(lex, auto, th, sh, tsa, adjunctionNonterminals);
            }
        }

        // add rules for empty adjunctions
        for (String nt : adjunctionNonterminals) {
            String sym = makeNop(nt);
            auto.addRule(auto.createRule(nt, sym, Collections.EMPTY_LIST));
            
            th.add(sym, Tree.create(TagTreeAlgebra.P1));
            sh.add(sym, Tree.create(TagStringAlgebra.EE()));
        }

        return irtg;
    }
    
    public static String makeNop(String nt) {
        return NO_ADJUNCTION + "_" + nt;
    }

    private static String makeTerminalSymbol(LexiconEntry lex) {
        return lex.getElementaryTreeName() + "-" + lex.getWord();
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
    List<String> getChildStates(ElementaryTree etree) {
        final List<String> childStates = new ArrayList<>();

        etree.getTree().dfs((node, children) -> {
            String label = node.getLabel().getLeft(); // use these as states

            switch (node.getLabel().getRight()) {
                case HEAD:
                case SECONDARY_LEX:
                    childStates.add(makeA(label));
                    break;

                case SUBSTITUTION:
                    childStates.add(makeS(label));
                    break;

                case DEFAULT:
                    if (isTrace(label)) { //  traceP != null && traceP.test(label)) {
                        // do not allow adjunction around traces
                    } else {
                        childStates.add(makeA(label));
                    }
                    break;
                    
                default:
                    // NOP
            }

            return null;
        });

        return childStates;
    }
    
    /**
     * Returns a tree of the same shape as the elementary tree, with each
     * node replaced by its post-order visit index, starting at nextPosition for the
     * leftmost leaf. This is the same order in which {@link #convertElementaryTree(de.up.ling.irtg.codec.tag.LexiconEntry, de.up.ling.irtg.automata.ConcreteTreeAutomaton, de.up.ling.irtg.hom.Homomorphism, de.up.ling.irtg.hom.Homomorphism, de.up.ling.irtg.algebra.TagStringAlgebra, java.util.Set) }
     * and {@link #getChildStates(de.up.ling.irtg.codec.tag.ElementaryTree) } generate
     * the lists of child states.
     * 
     * The node label -1 indicates that this node does not generate a child
     * state.
     * 
     * @param tree
     * @param nextPosition
     * @return 
     */
    Tree<Integer> makeDfsNodePositions(ElementaryTree tree, MutableInteger nextPosition) {
            return tree.getTree().dfs((node, children) -> {
                switch (node.getLabel().getRight()) {
                    case HEAD:
                    case SECONDARY_LEX:
                    case SUBSTITUTION:
                        return Tree.create(nextPosition.incValue(), children);
                        
                    case DEFAULT:
                        if (isTrace(node.getLabel().getLeft())) {
                            // do not allow adjunction around traces
                            return Tree.create(-1);
                        } else {
                            return Tree.create(nextPosition.incValue(), children);
                        }
                    
                    default:
                        return Tree.create(-1, children);
                }
            });
        }

    /**
     * NB: Keep this consistent with {@link #makeDfsNodePositions(de.up.ling.irtg.codec.tag.ElementaryTree, de.up.ling.irtg.util.MutableInteger) }
     * and {@link #getChildStates(de.up.ling.irtg.codec.tag.ElementaryTree) }.
     * 
     * @param lex
     * @param auto
     * @param th
     * @param sh
     * @param tsa
     * @param adjunctionNonterminals 
     */
    private void convertElementaryTree(LexiconEntry lex, ConcreteTreeAutomaton<String> auto, Homomorphism th, Homomorphism sh, TagStringAlgebra tsa, final Set<String> adjunctionNonterminals) {
        final List<String> childStates = new ArrayList<>();
        ElementaryTree etree = trees.get(lex.getElementaryTreeName());
        MutableInteger nextVar = new MutableInteger(1);
        String adjPrefix = "?" + ADJ_VARTYPE;
        String substPrefix = "?" + SUBST_VARTYPE;
        String terminalSym = makeTerminalSymbol(lex);

        // null etree means that no elementary tree of that name was defined
        // in the grammar. An example is the dummy "tCO" tree from the Chen
        // PTB-TAG. We ignore these lexicon entries.
        if (etree != null) {
            Tree<HomomorphismSymbol> treeHomTerm = etree.getTree().dfs((node, children) -> {
                String label = node.getLabel().getLeft(); // use these as states
//                String labelWithArity = label + "_" + children.size();     // use these as labels in the homomorphism terms // suitable for interpretation by new TagTreeAlgebra
                Tree<HomomorphismSymbol> ret = null;

                switch (node.getLabel().getRight()) {
                    case HEAD:
                        childStates.add(makeA(label));
                        adjunctionNonterminals.add(makeA(label));
                        ret = Tree.create(th.c(TagTreeAlgebra.C, 2),
                                Tree.create(th.v(nextVar.gensym(adjPrefix))),
                                Tree.create(lwa(label, 1, th), Tree.create(th.c(lex.getWord())))); // th.c(labelWithArity + "**1", 1)
                        break;

                    case SECONDARY_LEX:
                        childStates.add(makeA(label));
                        adjunctionNonterminals.add(makeA(label));
                        ret = Tree.create(th.c(TagTreeAlgebra.C, 2),
                                Tree.create(th.v(nextVar.gensym(adjPrefix))),
                                Tree.create(lwa(label, 1, th), Tree.create(th.c(lex.getSecondaryLex())))); // th.c(labelWithArity + "**1", 1)
                        break;
                    // TODO - maybe XTAG allows multiple secondary lexes, one per POS-tag

                    case FOOT:
                        ret = Tree.create(th.c(TagTreeAlgebra.P1));
                        break;

                    case SUBSTITUTION:
                        childStates.add(makeS(label));
                        ret = Tree.create(th.v(nextVar.gensym(substPrefix)));
                        break;

                    case DEFAULT:
                        if (isTrace(label)) { //  traceP != null && traceP.test(label)) {
                            // do not allow adjunction around traces
                            ret = Tree.create(lwa(label, 0, th)); // th.c(labelWithArity, 0));
                        } else {
                            childStates.add(makeA(label));
                            adjunctionNonterminals.add(makeA(label));
                            int numChildren = children.size();

                            ret = Tree.create(th.c(TagTreeAlgebra.C, 2),
                                    Tree.create(th.v(nextVar.gensym(adjPrefix))),
                                    Tree.create(lwa(label, numChildren, th), children)); // th.c(labelWithArity, numChildren)
//                            Tree.create(th.c(labelWithArity, 1), children));
                        }
                        break;

                    default:
                        throw new CodecParseException("Illegal node type in " + lex + ": " + etree.getTree().getLabel());
                }

                return ret;
            });

            int terminalSymId = auto.getSignature().addSymbol(terminalSym, nextVar.getValue() - 1);
            String parentState = (etree.getType() == ElementaryTreeType.INITIAL) ? makeS(etree.getRootLabel()) : makeA(etree.getRootLabel());
            auto.addRule(auto.createRule(parentState, terminalSym, childStates));
            th.add(terminalSymId, treeHomTerm);
            sh.add(terminalSymId, makeStringHom(treeHomTerm, th, sh, tsa, childStates));
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
