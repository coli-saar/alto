package de.up.ling.irtg.codec;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.pcfg_as_irtg.PcfgAsIrtgParser;
import de.up.ling.irtg.codec.scfg.SynchronousContextFreeGrammarLexer;
import de.up.ling.irtg.codec.scfg.SynchronousContextFreeGrammarParser;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@CodecMetadata(name = "scfg", description = "Synchronous context-free grammars", extension = "scfg", type = InterpretedTreeAutomaton.class)
public class SynchronousCfgInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    private int nextGensym = 1;

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        SynchronousContextFreeGrammarLexer l = new SynchronousContextFreeGrammarLexer(CharStreams.fromStream(is));
        l.removeErrorListeners();
        l.addErrorListener(ThrowingErrorListener.INSTANCE);

        SynchronousContextFreeGrammarParser p = new SynchronousContextFreeGrammarParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(ThrowingErrorListener.INSTANCE);
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        try {
            SynchronousContextFreeGrammarParser.ScfgContext result = p.scfg();

            ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();

            StringAlgebra leftStringAlgebra = new StringAlgebra();
            Homomorphism leftStringHom = new Homomorphism(auto.getSignature(), leftStringAlgebra.getSignature());

            TreeWithAritiesAlgebra leftTreeAlgebra = new TreeWithAritiesAlgebra();
            Homomorphism leftTreeHom = new Homomorphism(auto.getSignature(), leftTreeAlgebra.getSignature());

            StringAlgebra rightStringAlgebra = new StringAlgebra();
            Homomorphism rightStringHom = new Homomorphism(auto.getSignature(), rightStringAlgebra.getSignature());

            TreeWithAritiesAlgebra rightTreeAlgebra = new TreeWithAritiesAlgebra();
            Homomorphism rightTreeHom = new Homomorphism(auto.getSignature(), rightTreeAlgebra.getSignature());

            scfg(result, auto, leftStringHom, leftTreeHom, rightStringHom, rightTreeHom);

            InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);
            irtg.addInterpretation(new Interpretation(leftStringAlgebra, leftStringHom, "left"));
            irtg.addInterpretation(new Interpretation(leftTreeAlgebra, leftTreeHom, "left_tree"));
            irtg.addInterpretation(new Interpretation(rightStringAlgebra, rightStringHom, "right"));
            irtg.addInterpretation(new Interpretation(rightTreeAlgebra, rightTreeHom, "right_tree"));
            return irtg;
        } catch (RecognitionException e) {
            throw new CodecParseException(e.getMessage());
        }
    }

    private void scfg(SynchronousContextFreeGrammarParser.ScfgContext scfg, ConcreteTreeAutomaton<String> auto, Homomorphism leftStringHom, Homomorphism leftTreeHom, Homomorphism rightStringHom, Homomorphism rightTreeHom) {
        String startsym = name(scfg.startsymbol().name());
        Set<String> nonterminals = new HashSet<>();
        List<Pair<RawRule,RawRule>> rawRulePairs = new ArrayList<>();

        // collect all rules and record their LHS symbols as nonterminals
        int numRules = scfg.rulepair().size();
        int ruleIndex = 1;
        for (SynchronousContextFreeGrammarParser.RulepairContext rulePairContext : scfg.rulepair()) {
            notifyProgressListener(ruleIndex, 2*numRules, "Processing rules: " + (ruleIndex++) + "/" + numRules);
            RawRule leftRule = rule(rulePairContext.cfg_rule(0), nonterminals, false);
            RawRule rightRule = rule(rulePairContext.cfg_rule(1), nonterminals, true);
            rawRulePairs.add(new Pair(leftRule, rightRule));
        }

        // create rules and add them to automaton and homomorphism
        numRules = rawRulePairs.size();
        ruleIndex = 1;
        for (Pair<RawRule,RawRule> rulePair : rawRulePairs) {
            notifyProgressListener(numRules+ruleIndex, 2*numRules, "Creating rules: " + (ruleIndex++) + "/" + numRules);

            // process left rule
            RawRule leftRule = rulePair.left;
            RawRule rightRule = rulePair.right;

            if( ! leftRule.lhs.equals(rightRule.lhs)) {
                throw new CodecParseException("Invalid rule pair with different left-hand sides: '" + leftRule.lhs + "', '" + rightRule.lhs + "'");
            }

            List<String> leftHomLeaves = new ArrayList<>();
            List<String> rhsNonterminals = new ArrayList();
            Map<String,Integer> nonterminalToIndex = new HashMap<>(); // nonterminal name to variable name in the encoding of the left rule
            int i = 1;

            for (String rhsSymbol : leftRule.rhs) {
                if (nonterminals.contains(rhsSymbol)) {
                    if( nonterminalToIndex.containsKey(rhsSymbol)) {
                        nonterminalToIndex.put(rhsSymbol, -1); // nonterminal occurs twice => enforce indexing with coindexation on right rule
                    } else {
                        nonterminalToIndex.put(rhsSymbol, i);
                    }

                    leftHomLeaves.add("?" + (i++));
                    rhsNonterminals.add(rhsSymbol);
                } else {
                    leftHomLeaves.add(rhsSymbol);
                }
            }

            // create IRTG rule with left hom
            String terminal = gensym("r");
            auto.addRule(auto.createRule(leftRule.lhs, terminal, rhsNonterminals));
            leftStringHom.add(terminal, Util.makeBinaryTree("*", leftHomLeaves));
            leftTreeHom.add(terminal, Util.makeTreeWithArities(Tree.create(leftRule.lhs, Util.mapToList(leftHomLeaves, Tree::create))));

            // process right rule
            List<String> rightHomLeaves = new ArrayList<>();

            for( int k = 0; k < rightRule.rhs.size(); k++ ) {
                String rhsSymbol = rightRule.rhs.get(k);
                int rhsIndex = rightRule.rhsIndices.get(k);

                if (nonterminals.contains(rhsSymbol)) {
                    Integer leftVariableIndex = nonterminalToIndex.get(rhsSymbol);

                    if( leftVariableIndex == null ) {
                        throw new CodecParseException("RHS of rule pair contains a nonterminal that is not mentioned in left rule: " + rhsSymbol);
                    } else if( leftVariableIndex > 0 ) {
                        rightHomLeaves.add("?" + leftVariableIndex);
                    } else if( rhsIndex == 0 ) {
                        throw new CodecParseException("Ambiguous nonterminal " + rhsSymbol + " in right rule has no index.");
                    } else {
                        rightHomLeaves.add("?" + rhsIndex);
                    }
                } else {
                    rightHomLeaves.add(rhsSymbol);
                }
            }

            rightStringHom.add(terminal, Util.makeBinaryTree("*", rightHomLeaves));
            rightTreeHom.add(terminal, Util.makeTreeWithArities(Tree.create(leftRule.lhs, Util.mapToList(rightHomLeaves, Tree::create))));
        }

        auto.addFinalState(auto.addState(startsym));
    }

    private RawRule rule(SynchronousContextFreeGrammarParser.Cfg_ruleContext ruleContext, Set<String> nonterminals, boolean numInBracketsAllowed) {
        String lhs = name(ruleContext.name());
        List<String> rhs = new ArrayList<>();
        IntList rhsIndices = new IntArrayList();

        for(SynchronousContextFreeGrammarParser.Name_with_optional_bracketContext rhsName : ruleContext.name_with_optional_bracket()) {
            rhs.add(name(rhsName.name()));

            if( rhsName.NUMBER_IN_BRACKETS() != null ) {
                if( ! numInBracketsAllowed ) {
                    throw new CodecParseException("Nonterminal coindexation is only allowed in right rules.");
                }
                rhsIndices.add(Integer.parseInt(stripOuterChars(rhsName.NUMBER_IN_BRACKETS().getText())));
            } else {
                rhsIndices.add(0);
            }
        }

        nonterminals.add(lhs);
        return new RawRule(lhs, rhs, rhsIndices);
    }

    private String gensym(String prefix) {
        return prefix + (nextGensym++);
    }

    private static String name(SynchronousContextFreeGrammarParser.NameContext nc) {
        return CodecUtilities.extractName(nc, false);
    }


    private static String stripOuterChars(String s) {
        assert s.length() >= 2 : "string -" + s + "- should have length at least 2";
        return s.substring(1, s.length() - 1);
    }

    private static class RawRule {
        String lhs;
        List<String> rhs;
        IntList rhsIndices;

        public RawRule(String lhs, List<String> rhs, IntList rhsIndices) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.rhsIndices = rhsIndices;
        }
    }


    public static void main(String[] args) throws Exception {
        InterpretedTreeAutomaton irtg = new PcfgIrtgInputCodec().read(new FileInputStream("grammar.txt"));
        System.out.println(irtg);
    }
}
