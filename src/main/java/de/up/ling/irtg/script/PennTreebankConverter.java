/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.corpus.CorpusConverter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.algebra.WideStringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.codec.PtbTreeInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts a treebank in Penn Treebank format into an Alto corpus. Specify the
 * files which make up the treebank as command-line arguments. The converted
 * corpus is written to an Alto corpus file.<p>
 *
 * The conversion uses an implicit IRTG with an interpretation "string" (using a
 * {@link StringAlgebra}) and an interpretation "tree" representing the
 * phrase-structure trees (using a {@link TreeWithAritiesAlgebra})
 * .<p>
 *
 * The strings and trees are taken literally from the treebank. No
 * postprocessing or normalization is performed.
 *
 * @author koller
 */
public class PennTreebankConverter {

    public static void main(String[] args) throws Exception {
        PtbTreeInputCodec codec = new PtbTreeInputCodec();
        Writer w = new FileWriter("out.txt");
        Map<String, Algebra> algebras = ImmutableMap.of("string", new WideStringAlgebra(), "tree", new TreeWithAritiesAlgebra());

        CorpusConverter<Tree<String>> converter = new CorpusConverter<Tree<String>>(Joiner.on(" ").join(args),
                algebras,
                ImmutableMap.of("string", (Tree<String> tree) -> tree.getLeafLabels(), "tree", x -> x),
                w);

        DerivationTreeMaker dtm = new DerivationTreeMaker(algebras);
        converter.setDerivationTreeMaker(dtm);

        for (String filename : args) {
            System.err.println("Processing " + filename + " ...");
            InputStream corpus = new FileInputStream(filename);
            codec.readCorpus(corpus).forEach(converter);
        }

        w.flush();
        w.close();

        System.err.println("Done.");

        // write IRTG
        w = new FileWriter("out.irtg");
        w.write(dtm.irtg.toString());
        w.flush();
        w.close();
    }

    private static class DerivationTreeMaker implements Function<Tree<String>, Tree<String>> {

        Interner<PtbRule> seenRules = new Interner<>();
        InterpretedTreeAutomaton irtg;
        private ConcreteTreeAutomaton<String> auto;

        public DerivationTreeMaker(Map<String, Algebra> algebras) {
            irtg = InterpretedTreeAutomaton.forAlgebras(algebras);
            auto = (ConcreteTreeAutomaton) irtg.getAutomaton();
        }

        @Override
        public Tree<String> apply(Tree<String> derivedTree) {
            return derivedTree.dfs((node, children) -> {
//                System.err.println("call: " + node);
//                System.err.println("    " + children);

                if (node.getChildren().isEmpty()) {
                    // leaf -- nothing to be done here
                    return null;
                } else {
                    PtbRule ruleHere = new PtbRule(node.getLabel(), Util.mapToList(node.getChildren(), ch -> ch.getLabel()));
                    int index = seenRules.addObject(ruleHere);
                    String label = "r" + index;

                    List<String> ruleChildren = new ArrayList<>();           // for the RTG rule
                    List<Tree<String>> derivTreeChildren = new ArrayList<>();      // for the deriv tree
                    List<Tree<String>> homChildren = new ArrayList<>();            // for the homomorphisms
                    int nextVar = 1;

                    for (int i = 0; i < children.size(); i++) {
                        Tree<String> ch = children.get(i);
                        Tree<String> originalChild = node.getChildren().get(i);

                        Tree<String> homChild = (ch == null) ? originalChild : Tree.create("?" + (nextVar++));
                        homChildren.add(homChild);

                        if (ch != null) {
                            ruleChildren.add(originalChild.getLabel());      // root nonterminal
                            derivTreeChildren.add(ch);
                        }
                    }

                    Rule rule = auto.createRule(node.getLabel(), label, ruleChildren);
                    auto.addRule(rule);

                    Tree st = Tree.create("conc" + homChildren.size(), homChildren);
//                    System.err.println("hs(" + label + ") = " + st);
                    irtg.getInterpretation("string").getHomomorphism().add(label, st);
                    irtg.getInterpretation("tree").getHomomorphism().add(label, Tree.create(node.getLabel() + "_" + homChildren.size(), homChildren));

                    System.err.println(irtg.getInterpretation("string").getHomomorphism().get(label));

                    Homomorphism hom = irtg.getInterpretation("string").getHomomorphism();
                    Tree<HomomorphismSymbol> rhs = hom.get(rule.getLabel());
                    System.err.println(hom.rhsAsString(rhs));

                    if (node == derivedTree) {
                        auto.addFinalState(rule.getParent());
                    }

                    return Tree.create(label, derivTreeChildren);
                }
            });

        }

    }

    private static class PtbRule {

        String lhs;
        List<String> rhs;

        public PtbRule(String lhs, List<String> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + Objects.hashCode(this.lhs);
            hash = 47 * hash + Objects.hashCode(this.rhs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PtbRule other = (PtbRule) obj;
            if (!Objects.equals(this.lhs, other.lhs)) {
                return false;
            }
            if (!Objects.equals(this.rhs, other.rhs)) {
                return false;
            }
            return true;
        }

    }
}
