/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.sampling.models.IndependentTrees;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 * @param <Type1>
 * @param <Type2>
 */
public class ExtractGrammar<Type1, Type2> {

    /**
     *
     */
    private final Algebra<Type1> algebra1;

    /**
     *
     */
    private final Algebra<Type2> algebra2;

    /**
     *
     */
    private final String interpretation1ID;

    /**
     *
     */
    private final String interpretation2ID;

    /**
     *
     */
    private final TreeExtractor trex;

    /**
     *
     */
    private final StringSubtreeIterator.VariableMapping nonterminals;

    /**
     *
     * @param algebra1
     * @param algebra2
     * @param nonterminals
     * @param interpretation1ID
     * @param interpretation2ID
     * @param trex
     */
    public ExtractGrammar(Algebra<Type1> algebra1, Algebra<Type2> algebra2,
            StringSubtreeIterator.VariableMapping nonterminals,
            String interpretation1ID, String interpretation2ID,
            TreeExtractor trex) {
        this.algebra1 = algebra1;
        this.algebra2 = algebra2;
        this.nonterminals = nonterminals;
        this.interpretation1ID = interpretation1ID;
        this.interpretation2ID = interpretation2ID;
        this.trex = trex;
    }

    /**
     *
     * @param inputs
     * @param trees
     * @param irtg
     * @throws java.io.IOException
     */
    public void extract(Iterable<InputStream> inputs, OutputStream trees, OutputStream irtg)
            throws IOException {
        IrtgInputCodec iic = new IrtgInputCodec();

        Iterable<InterpretedTreeAutomaton> analyses = new FunctionIterable<>(inputs, (InputStream in) -> {
            InterpretedTreeAutomaton ita;
            try {
                ita = iic.read(in);
                in.close();
            } catch (IOException | CodecParseException ex) {
                Logger.getLogger(ExtractGrammar.class.getName()).log(Level.SEVERE, null, ex);
                ita = null;
            }

            return ita;
        });

        Iterable<TreeAutomaton> automata = new FunctionIterable<>(analyses, (InterpretedTreeAutomaton ita) -> {
            return ita.getAutomaton();
        });

        MostFrequentVariables mfv = new MostFrequentVariables();
        Iterable<Tree<String>> solutions = mfv.getChoices(automata);

        RulePostProcessing<Type1, Type2> rpp = new RulePostProcessing<>(algebra1, algebra2);

        Iterator<Tree<String>> itSol = solutions.iterator();
        Iterator<InterpretedTreeAutomaton> itAnalysis = analyses.iterator();

        try (BufferedWriter solutionWriter = new BufferedWriter(new OutputStreamWriter(trees))) {
            while (itSol.hasNext() && itAnalysis.hasNext()) {
                Tree<String> solution = itSol.next();
                InterpretedTreeAutomaton ita = itAnalysis.next();

                solutionWriter.write(solution.toString());
                solutionWriter.newLine();
                Map<String, Object> m = ita.interpret(solution);
                for (Map.Entry<String, Object> ent : m.entrySet()) {
                    solutionWriter.write(ent.getKey());
                    solutionWriter.write('\t');
                    solutionWriter.write(ent.getValue().toString());
                    solutionWriter.newLine();
                }
                solutionWriter.newLine();

                final Homomorphism hom1 = ita.getInterpretation(interpretation1ID).getHomomorphism();
                final Homomorphism hom2 = ita.getInterpretation(interpretation2ID).getHomomorphism();

                Iterator<Tree<String>> subtrees = StringSubtreeIterator.getSubtrees(solution, nonterminals);
                if (subtrees.hasNext()) {
                    rpp.addRule(subtrees.next(), hom1, hom2, true);
                }
                subtrees.forEachRemaining((Tree<String> t) -> {
                    rpp.addRule(t, hom1, hom2, false);
                });
            }
        }

        InterpretedTreeAutomaton ita = rpp.getIRTG(interpretation1ID, interpretation2ID);
        ita.getAutomaton().normalizeRuleWeights();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(irtg))) {
            bw.write(ita.toString());
        }
    }

    /**
     *
     * @param inputs
     * @param trees
     * @param irtg
     * @throws IOException
     */
    public void extractBySampling(Iterable<InputStream> inputs, OutputStream trees, OutputStream grammar)
            throws IOException {
        IrtgInputCodec iic = new IrtgInputCodec();

        Iterable<InterpretedTreeAutomaton> analyses = new FunctionIterable<>(inputs, (InputStream in) -> {
            InterpretedTreeAutomaton ita;
            try {
                ita = iic.read(in);
                in.close();
            } catch (IOException | CodecParseException ex) {
                Logger.getLogger(ExtractGrammar.class.getName()).log(Level.SEVERE, null, ex);
                ita = null;
            }

            return ita;
        });

        SampleOnlineEM soe = new SampleOnlineEM();

        Iterable<Signature> sigIt
                = new FunctionIterable<>(analyses, (InterpretedTreeAutomaton ita) -> ita.getAutomaton().getSignature());

        IndependentTrees it = new IndependentTrees(1.0, sigIt, "'X(x1) / X(x1) | 1'");
        Iterable<Iterable<Tree<String>>> solutions = soe.getChoices(analyses, it, new Date().getTime());

        RulePostProcessing<Type1, Type2> rpp = new RulePostProcessing<>(algebra1, algebra2);

        Iterator<Iterable<Tree<String>>> itSol = solutions.iterator();
        Iterator<InterpretedTreeAutomaton> itAnalysis = analyses.iterator();

        System.out.println("constructing grammar:");
        int pos = 0;
        try (BufferedWriter solutionWriter = new BufferedWriter(new OutputStreamWriter(trees))) {
            while (itSol.hasNext() && itAnalysis.hasNext()) {
                Iterable<Tree<String>> instanceSolutions = itSol.next();
                InterpretedTreeAutomaton ita = itAnalysis.next();

                for (Tree<String> solution : instanceSolutions) {

                    solutionWriter.write(solution.toString());
                    solutionWriter.newLine();

                    Map<String, Object> m = ita.interpret(solution);
                    for (Map.Entry<String, Object> ent : m.entrySet()) {
                        solutionWriter.write(ent.getKey());
                        solutionWriter.write('\t');
                        solutionWriter.write(ent.getValue().toString());
                        solutionWriter.newLine();
                    }
                    solutionWriter.newLine();

                    final Homomorphism hom1 = ita.getInterpretation(interpretation1ID).getHomomorphism();
                    final Homomorphism hom2 = ita.getInterpretation(interpretation2ID).getHomomorphism();

                    Iterator<Tree<String>> subtrees = StringSubtreeIterator.getSubtrees(solution, nonterminals);
                    if (subtrees.hasNext()) {
                        rpp.addRule(subtrees.next(), hom1, hom2, true);
                    }
                    subtrees.forEachRemaining((Tree<String> t) -> {
                        rpp.addRule(t, hom1, hom2, false);
                    });
                }

                if ((pos + 1) % 10 == 0) {
                    System.out.println("Processed " + (pos + 1) + " data points.");
                }

                ++pos;
            }
        }

        InterpretedTreeAutomaton ita = rpp.getIRTG(interpretation1ID, interpretation2ID);
        ita.getAutomaton().normalizeRuleWeights();
        
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(grammar))) {
            bw.write(ita.toString());
        }
    }
}
