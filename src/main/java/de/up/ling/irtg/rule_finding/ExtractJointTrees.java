/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.util.BiFunctionIterable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractJointTrees {
       
    /**
     * 
     */
    public static final String FIRST_ALGEBRA_ID = "FirstInput";
    
    /**
     * 
     */
    public static final String SECOND_ALGEBRA_ID = "SecondInput";
    
    
    /**
     * 
     */
    private final CorpusCreator cc;

    /**
     * 
     * @param cc 
     */
    public ExtractJointTrees(CorpusCreator cc) {
        this.cc = cc;
    }
    
    /**
     * 
     * @param firstAuts
     * @param secondAuts
     * @param firstAlign
     * @param secondAlign
     * @param outs
     * @return
     * @throws IOException
     * @throws ParserException 
     */
    public double[] getAutomataAndMakeStatistics(Iterable<InputStream> firstAuts,
            Iterable<InputStream> secondAuts, Iterable<InputStream> firstAlign,
            Iterable<InputStream> secondAlign, Supplier<OutputStream> outs) 
                                                throws IOException, ParserException {
        
        Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> pairs1 = makeTreeAutomata(firstAuts,firstAlign);
        Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> pairs2 = makeTreeAutomata(secondAuts,secondAlign);
             
        Iterable<Pair<TreeAutomaton<String>,TreeAutomaton<String>>> p = this.cc.pushAlignments(pairs1,pairs2);
        
        Iterable<Pair<TreeAutomaton,HomomorphismManager>> results = this.cc.getSharedAutomata(p);

        double sumOfSizes = 0;
        double length = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        
        int i = 0;
        for (Pair<TreeAutomaton,HomomorphismManager> pair : results) {
            ++length;
            
            TreeAutomaton ta = pair.getLeft();
            HomomorphismManager hm = pair.getRight();
            InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(ta);

            double size = (ta.countTrees());
            sumOfSizes += size;
            min = Math.min(min, size);
            max = Math.max(size, max);
            
            TreeAlgebra algebra1 = new TreeAlgebra();
            TreeAlgebra algebra2 = new TreeAlgebra();

            Homomorphism hm1 = hm.getHomomorphism1();
            Homomorphism hm2 = hm.getHomomorphism2();

            Interpretation inpre1 = new Interpretation(algebra1, hm1);
            Interpretation inpre2 = new Interpretation(algebra2, hm2);

            ita.addInterpretation("FirstInput", inpre1);
            ita.addInterpretation("SecondInput", inpre2);

            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outs.get()))) {
                out.write(ita.toString());
                out.flush();
            }
            
            System.out.println("handeled input pair: "+(++i));
        }
        
        return new double[] {sumOfSizes / length, min, max};
    }

    /**
     * 
     * @param firstAuts
     * @return 
     */
    private Iterable<Pair<TreeAutomaton, StateAlignmentMarking>> makeTreeAutomata(Iterable<InputStream> firstAuts, Iterable<InputStream> align) {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> auIt = new BiFunctionIterable<>(firstAuts, align, (InputStream au, InputStream al) -> {
            try {
                TreeAutomaton ta = taic.read(au);
                StateAlignmentMarking sam = new SpecifiedAligner(ta, al);
                
                return new Pair<>(ta,sam);
            } catch (CodecParseException | IOException ex) {
                throw new RuntimeException("Could not parse automaton.");
            }
        });
        
        return auIt;
    }
}
