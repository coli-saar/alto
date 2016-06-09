/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private final String outInterpretation1;

    /**
     *
     */
    private final String outInterpretation2;

    /**
     *
     */
    private final TreeExtractor trex;
    
    /**
     *
     * @param algebra1
     * @param algebra2
     * @param interpretation1ID
     * @param interpretation2ID
     * @param trex
     * @param outInterpretation1
     * @param outInterpretation2
     */
    public ExtractGrammar(Algebra<Type1> algebra1, Algebra<Type2> algebra2,
            String interpretation1ID, String interpretation2ID, TreeExtractor trex,
            String outInterpretation1, String outInterpretation2) {
        this.algebra1 = algebra1;
        this.algebra2 = algebra2;

        this.interpretation1ID = interpretation1ID;
        this.interpretation2ID = interpretation2ID;
        this.outInterpretation1 = outInterpretation1;
        this.outInterpretation2 = outInterpretation2;

        this.trex = trex;
    }

    /**
     * 
     * @param input
     * @param trees
     * @param irtg
     * @throws IOException 
     */
    public void extractForFirstAlgebra(Iterable<InputStream> input, OutputStream trees, OutputStream irtg) throws IOException {
        Iterable<InterpretedTreeAutomaton> analyses = makeIRTGIterable(input);
        
        Iterable<Iterable<Tree<String>>> solutions = this.trex.getAnalyses(analyses);
        RulePostProcessing<Type1, Type2> rpp = new RulePostProcessing<>(algebra1);
        
        Iterator<Iterable<Tree<String>>> itSol = solutions.iterator();
        Iterator<InterpretedTreeAutomaton> itAnalysis = analyses.iterator();
        
        try (BufferedWriter solutionWriter = new BufferedWriter(new OutputStreamWriter(trees))) {
            while (itSol.hasNext() && itAnalysis.hasNext()) {
                Iterable<Tree<String>> subPackage = itSol.next();
                InterpretedTreeAutomaton ita = itAnalysis.next();
                
                final Homomorphism hom1 = ita.getInterpretation(interpretation1ID).getHomomorphism();

                for (Tree<String> solution : subPackage) {
                    solutionWriter.write(solution.toString());
                    solutionWriter.newLine();

                    Iterator<Tree<String>> subtrees = StringSubtreeIterator.getSubtrees(solution);    
                    
                    if (subtrees.hasNext()) {
                        rpp.addRule(subtrees.next(), hom1, true);
                    }
                    subtrees.forEachRemaining((Tree<String> t) -> {
                        rpp.addRule(t, hom1, false);
                    });
                }
            }
        }
        
        InterpretedTreeAutomaton ita = rpp.getIRTG(outInterpretation1);
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
     * @throws java.io.IOException
     */
    public void extract(Iterable<InputStream> inputs, OutputStream trees, OutputStream irtg)
            throws IOException {
        Iterable<InterpretedTreeAutomaton> analyses = makeIRTGIterable(inputs);

        Iterable<Iterable<Tree<String>>> solutions = this.trex.getAnalyses(analyses);

        RulePostProcessing<Type1, Type2> rpp = new RulePostProcessing<>(algebra1, algebra2);

        Iterator<Iterable<Tree<String>>> itSol = solutions.iterator();
        Iterator<InterpretedTreeAutomaton> itAnalysis = analyses.iterator();
        
        try (BufferedWriter solutionWriter = new BufferedWriter(new OutputStreamWriter(trees))) {
            while (itSol.hasNext() && itAnalysis.hasNext()) {
                Iterable<Tree<String>> subPackage = itSol.next();
                InterpretedTreeAutomaton ita = itAnalysis.next();
                
                    final Homomorphism hom1 = ita.getInterpretation(interpretation1ID).getHomomorphism();
                    final Homomorphism hom2 = ita.getInterpretation(interpretation2ID).getHomomorphism();

                for (Tree<String> solution : subPackage) {
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

                    Iterator<Tree<String>> subtrees = StringSubtreeIterator.getSubtrees(solution);    
                    
                    if (subtrees.hasNext()) {
                        rpp.addRule(subtrees.next(), hom1, hom2, true);
                    }
                    subtrees.forEachRemaining((Tree<String> t) -> {
                        rpp.addRule(t, hom1, hom2, false);
                    });
                }
            }
        }

        InterpretedTreeAutomaton ita = rpp.getIRTG(outInterpretation1, outInterpretation2);
        ita.getAutomaton().normalizeRuleWeights();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(irtg))) {
            bw.write(ita.toString());
        }
    }

    /**
     * 
     * @param inputs
     * @return 
     */
    private Iterable<InterpretedTreeAutomaton> makeIRTGIterable(Iterable<InputStream> inputs) {
        IrtgInputCodec iic = new IrtgInputCodec();
        Signature sig = new Signature();
        
        Iterable<InterpretedTreeAutomaton> analyses = new FunctionIterable<>(inputs, (InputStream in) -> {
            InterpretedTreeAutomaton ita;
            try {
                ita = iic.read(in,sig);
                
                in.close();
            } catch (IOException | CodecParseException ex) {
                Logger.getLogger(ExtractGrammar.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
            
            return ita;
        });
        return analyses;
    }
    
    /**
     * 
     * @param inputs
     * @param irtg
     * @param ruler
     * @param startNonterminal
     * @throws IOException 
     */
    public void extract(Iterable<InputStream> inputs, OutputStream irtg, SubtreeExtractor ruler,
            String startNonterminal)
            throws IOException {
        Iterable<InterpretedTreeAutomaton> analyses = this.makeIRTGIterable(inputs);
        
        RulePostProcessing<Type1, Type2> rpp = new RulePostProcessing<>(algebra1, algebra2);
        rpp.addFinalState(startNonterminal);
        
        Iterable<Iterable<Tree<String>>> rules = ruler.getRuleTrees(analyses);
        
        Iterator<InterpretedTreeAutomaton> itAnalysis = analyses.iterator();
        for(Iterable<Tree<String>> inner : rules) {
            
            InterpretedTreeAutomaton ita = itAnalysis.next();
            final Homomorphism hom1 = ita.getInterpretation(interpretation1ID).getHomomorphism();
            final Homomorphism hom2 = ita.getInterpretation(interpretation2ID).getHomomorphism();
            
            for(Tree<String> rule : inner) {
                rpp.addRule(rule, hom1, hom2, false);
            }
        }
        
        InterpretedTreeAutomaton result = rpp.getIRTG(outInterpretation1, outInterpretation2);
        try(BufferedWriter output = new BufferedWriter(new OutputStreamWriter(irtg))) {
            output.write(result.toString());
        }
    }
}
