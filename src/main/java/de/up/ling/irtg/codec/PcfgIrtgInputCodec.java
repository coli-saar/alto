/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.pcfg_as_irtg.PcfgAsIrtgLexer;
import de.up.ling.irtg.codec.pcfg_as_irtg.PcfgAsIrtgParser;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 * An input codec for context-free grammars, with or without rule probabilities.
 * The first line of a file that this codec will read contains the start symbol of the grammar.
 * Then one rule is specified on each line, in the following form:<p>
 * 
 * {@code A -> B c D [0.3]}
 * 
 * Rule probabilities may optionally be specified in square brackets for each rule.
 * Rules without explicit probabilities receive weight 1; this can be used
 * to specify an ordinary, non-probabilistic context-free grammar.<p>
 * 
 * The codec infers which symbols are nonterminals by taking all symbols
 * that occur on the left-hand side of a rule as nonterminals. This means
 * that the terminal and nonterminal symbols must be disjoint sets, otherwise
 * the grammar will not be equivalent to what you intended.<p>
 * 
 * The codec represents the context-free grammar as an IRTG with a single
 * interpretation called "string" over a {@link de.up.ling.irtg.algebra.StringAlgebra}.
 * This algebra has only a single concatenation operator, "*"; it has arity 2.
 * You may still use multiple symbols on the right-hand side of your CFG rules.
 * These will be combined together using binary concatenations in an unspecified
 * bracketing.
 * 
 * @author koller
 */
@CodecMetadata(name = "pcfg", description = "Probabilistic context-free grammars", extension = "cfg", type = InterpretedTreeAutomaton.class)
public class PcfgIrtgInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    private int nextGensym = 1;

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        PcfgAsIrtgLexer l = new PcfgAsIrtgLexer(CharStreams.fromStream(is));
        l.removeErrorListeners();
        l.addErrorListener(ThrowingErrorListener.INSTANCE);
        PcfgAsIrtgParser p = new PcfgAsIrtgParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(ThrowingErrorListener.INSTANCE);
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        try {
            PcfgAsIrtgParser.PcfgContext result = p.pcfg();

            ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
            StringAlgebra stringAlgebra = new StringAlgebra();
            Homomorphism stringHom = new Homomorphism(auto.getSignature(), stringAlgebra.getSignature());
            TreeWithAritiesAlgebra treeAlgebra = new TreeWithAritiesAlgebra();
            Homomorphism treeHom = new Homomorphism(auto.getSignature(), treeAlgebra.getSignature());

            pcfg(result, auto, stringHom, treeHom);

            InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);
            irtg.addInterpretation(new Interpretation(stringAlgebra, stringHom, "string"));
            irtg.addInterpretation(new Interpretation(treeAlgebra, treeHom, "tree"));
            return irtg;
        } catch (RecognitionException e) {
            throw new CodecParseException(e.getMessage());
        } 
    }

    private void pcfg(PcfgAsIrtgParser.PcfgContext pcfg, ConcreteTreeAutomaton<String> auto, Homomorphism stringHom, Homomorphism treeHom) {
        String startsym = name(pcfg.startsymbol().name());
        Set<String> nonterminals = new HashSet<>();
        List<RawRule> rawRules = new ArrayList<>();

        // collect all rules and record their LHS symbols as nonterminals
        int numRules = pcfg.pcfg_rule().size();
        int ruleIndex = 1;
        for (PcfgAsIrtgParser.Pcfg_ruleContext ruleContext : pcfg.pcfg_rule()) {
            notifyProgressListener(ruleIndex, 2*numRules, "Processing rules: " + (ruleIndex++) + "/" + numRules);
            rule(ruleContext, rawRules, nonterminals);
        }

        // create rules and add them to automaton and homomorphism
        numRules = rawRules.size();
        ruleIndex = 1;
        for (RawRule rule : rawRules) {
            notifyProgressListener(numRules+ruleIndex, 2*numRules, "Creating rules: " + (ruleIndex++) + "/" + numRules);
            List<String> homLeaves = new ArrayList<>();
            List<String> rhsNonterminals = new ArrayList();
            int i = 1;

            for (String rhsSymbol : rule.rhs) {
                if (nonterminals.contains(rhsSymbol)) {
                    homLeaves.add("?" + (i++));
                    rhsNonterminals.add(rhsSymbol);
                } else {
                    homLeaves.add(rhsSymbol);
                }
            }

            String terminal = gensym("r");
            auto.addRule(auto.createRule(rule.lhs, terminal, rhsNonterminals, rule.weight));
            stringHom.add(terminal, Util.makeBinaryTree("*", homLeaves));
            treeHom.add(terminal, Util.makeTreeWithArities(Tree.create(rule.lhs, Util.mapToList(homLeaves, Tree::create))));
        }

        auto.addFinalState(auto.addState(startsym));
    }

    private void rule(PcfgAsIrtgParser.Pcfg_ruleContext ruleContext, List<RawRule> rawRules, Set<String> nonterminals) {
        String lhs = null;
        List<String> rhs = new ArrayList<>();
        double weight = 1;
        int i = 0;

        for (PcfgAsIrtgParser.NameContext name : ruleContext.name()) {
            if (i == 0) {
                lhs = name(name);
            } else {
                rhs.add(name(name));
            }

            i++;
        }

        if (ruleContext.NUMBER_IN_BRACKETS() != null) {
            weight = Double.parseDouble(stripOuterChars(ruleContext.NUMBER_IN_BRACKETS().getText()));
        }

        rawRules.add(new RawRule(lhs, rhs, weight));
        nonterminals.add(lhs);
    }

    private String gensym(String prefix) {
        return prefix + (nextGensym++);
    }
    
    private static String name(PcfgAsIrtgParser.NameContext nc) {
        return CodecUtilities.extractName(nc, false);
    }
    

    private static String stripOuterChars(String s) {
        assert s.length() >= 2 : "string -" + s + "- should have length at least 2";
        return s.substring(1, s.length() - 1);
    }

    private static class RawRule {
        String lhs;
        List<String> rhs;
        double weight;

        public RawRule(String lhs, List<String> rhs, double weight) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.weight = weight;
        }
    }
    
    
    public static void main(String[] args) throws Exception {
        InterpretedTreeAutomaton irtg = new PcfgIrtgInputCodec().read(new FileInputStream("grammar.txt"));
        System.out.println(irtg);
    }

}
