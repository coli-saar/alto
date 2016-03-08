/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.tag;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.StringOrVariable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class TagGrammar {

    private static final String NO_ADJUNCTION = "*NOP*";
    private static final char SUBST_VARTYPE = 'S';
    private static final char ADJ_VARTYPE = 'A';

    private Map<String, ElementaryTree> trees;   // tree-name -> elementary-tree
    private SetMultimap<String, LexiconEntry> lexicon; // word -> set(tree-name)

    public TagGrammar() {
        trees = new HashMap<>();
        lexicon = HashMultimap.create();
    }

    public void addElementaryTree(String name, ElementaryTree tree) {
        trees.put(name, tree);
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

                if (et == null) {
                    System.err.println("*** UNK ET: " + lex + " for word " + word + "***");
                } else {
                    ret.add(et.lexicalize(word, lex.getFeature("pos"), lex.getSecondaryLex()));
                }
            }
        }

        return ret;
    }

    public InterpretedTreeAutomaton toIrtg() {
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);

        return irtg;
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

    private static StringOrVariable s(String str) {
        return new StringOrVariable(str, false);
    }

    private static StringOrVariable v(String str) {
        return new StringOrVariable(str, true);
    }

    private void convertElementaryTree(LexiconEntry lex, ConcreteTreeAutomaton<String> auto, Homomorphism th, Homomorphism sh, TagStringAlgebra tsa, final Set<String> adjunctionNonterminals) {
        final List<String> childStates = new ArrayList<String>();
        ElementaryTree etree = trees.get(lex.getElementaryTreeName());
        MutableInteger nextVar = new MutableInteger(1);
        String adjPrefix = "?" + ADJ_VARTYPE;
        String substPrefix = "?" + SUBST_VARTYPE;
        String terminalSym = makeTerminalSymbol(lex);
        int terminalSymId = auto.getSignature().getIdForSymbol(terminalSym);

        Tree<HomomorphismSymbol> treeHomTerm = etree.getTree().dfs((node, children) -> {
            String label = etree.getTree().getLabel().getLeft(); // use these as states
            String labelWithArity = label + children.size();     // use these as labels in the homomorphism terms

            switch (etree.getTree().getLabel().getRight()) {
                case HEAD:
                    childStates.add(makeA(label));
                    adjunctionNonterminals.add(makeA(label));
                    return Tree.create(th.c(TagTreeAlgebra.C, 2),
                            Tree.create(th.v(nextVar.gensym(adjPrefix))),
                            Tree.create(th.c(labelWithArity, 1), Tree.create(th.c(lex.getWord()))));

                case SECONDARY_LEX:
                    childStates.add(makeA(label));
                    adjunctionNonterminals.add(makeA(label));
                    return Tree.create(th.c(TagTreeAlgebra.C, 2),
                            Tree.create(th.v(nextVar.gensym(adjPrefix))),
                            Tree.create(th.c(labelWithArity, 1), Tree.create(s(lex.getSecondaryLex()))));
                // TODO - maybe XTAG allows multiple secondary lexes, one per POS-tag

                case FOOT:
                    return Tree.create(th.c(TagTreeAlgebra.P1));

                case SUBSTITUTION:
                    childStates.add(makeS(label));
                    return Tree.create(th.v(nextVar.gensym(substPrefix)));

                case DEFAULT:
                    childStates.add(makeA(label));
                    adjunctionNonterminals.add(makeA(label));
                    return Tree.create(th.c(TagTreeAlgebra.C, 2),
                            Tree.create(th.v(nextVar.gensym(adjPrefix))),
                            Tree.create(th.c(labelWithArity, 1), children));

                default:
                    throw new CodecParseException("Illegal node type in " + lex + ": " + etree.getTree().getLabel());
            }
        });

        th.add(terminalSymId, treeHomTerm);
        sh.add(terminalSymId, makeStringHom(treeHomTerm, th, sh, tsa));
        String parentState = (etree.getType() == ElementaryTreeType.INITIAL) ? makeS(etree.getRootLabel()) : makeA(etree.getRootLabel());
        auto.addRule(auto.createRule(parentState, terminalSym, childStates));
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

    private static Tree<HomomorphismSymbol> makeStringHom(Tree<HomomorphismSymbol> treeForTreeHom, Homomorphism th, Homomorphism sh, TagStringAlgebra tsa) {
        SortedTree t = treeForTreeHom.dfs((node, children) -> {
            String label = th.getTargetSignature().resolveSymbolId(node.getLabel().getValue());

            if (TagTreeAlgebra.C.equals(label)) {
                assert children.size() == 2;
                return cs(TagStringAlgebra.WRAP(children.get(0).sort, children.get(1).sort), children, sh, tsa);
            } else if(TagTreeAlgebra.P1.equals(label)) {
                assert children.isEmpty();
                return cs(TagStringAlgebra.EE(), children, sh, tsa);
            } else if(label != null && label.contains("*TRACE*")) {
                return cs(TagStringAlgebra.E(), children, sh, tsa);
            } else {
                switch(children.size()) {
                    case 0:
                        
                }
            }

            return null;
        });

        /*
                
         } else {
         switch (children.size()) {
         case 0:
         if (node.getLabel().isVariable()) {
         if (label.charAt(1) == ADJ_VARTYPE) {
         return new SortedTree(Tree.create(node.getLabel()), 2);
         } else {
         return new SortedTree(Tree.create(node.getLabel()), 1);
         }
         } else {
         return cs(node.getLabel().getValue());
         }
         case 1:
         return children.get(0);
         default:
         return concatenateMany(children, 0);
         }
         }
         }
         });
         */
        return t.tree;
    }

    private static SortedTree concatenateMany(List<SortedTree> children, int pos) {
        SortedTree left = children.get(pos);
        SortedTree right = null;

        if (pos == children.size() - 2) {
            right = children.get(pos + 1);
        } else {
            right = concatenateMany(children, pos + 1);
        }

        List<SortedTree> processedChildren = new ArrayList<SortedTree>();
        processedChildren.add(left);
        processedChildren.add(right);

        return null; // XXX
//        return cs(TagStringTupleAlgebra.CONCAT(left.sort, right.sort), processedChildren);
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
