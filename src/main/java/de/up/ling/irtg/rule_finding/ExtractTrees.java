/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractTrees {
       
    /**
     * 
     */
    private final CorpusCreator cc;

    /**
     * 
     * @param cc 
     */
    public ExtractTrees(CorpusCreator cc) {
        this.cc = cc;
    }
    
    /**
     * 
     * @param in
     * @param outs
     * @return 
     * @throws java.io.IOException
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    public double[] getAutomataAndMakeStatistics(InputStream in, Supplier<OutputStream> outs) 
                                                throws IOException, ParserException {
        ArrayList<String> firstInputs = new ArrayList<>();
        ArrayList<String> secondInputs = new ArrayList<>();
        
        ArrayList<String> firstAlignments = new ArrayList<>();
        ArrayList<String> secondAlignments = new ArrayList<>();
        
        try(BufferedReader input = new BufferedReader(new InputStreamReader(in))) {
            String line;
            
            while((line = input.readLine()) != null){
                line = line.trim();
                if(!line.equals("")){
                    firstInputs.add(line);
                    secondInputs.add(input.readLine().trim());
                
                    firstAlignments.add(input.readLine().trim());
                    secondAlignments.add(input.readLine().trim());
                }
            }
        }
        
        List<TreeAutomaton> results
                = cc.makeRuleTrees(firstInputs, secondInputs, firstAlignments, secondAlignments);

        double sumOfSizes = 0;
        double length = results.size();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        
        
        for (TreeAutomaton ta : results) {
            InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(ta);

            double size = (ta.countTrees());
            sumOfSizes += size;
            min = Math.min(min, size);
            max = Math.max(size, max);
            
            TreeAlgebra algebra1 = new TreeAlgebra();
            TreeAlgebra algebra2 = new TreeAlgebra();

            Homomorphism hm1 = cc.getHomomorphismManager().getHomomorphism1();
            Homomorphism hm2 = cc.getHomomorphismManager().getHomomorphism2();

            Interpretation inpre1 = new Interpretation(algebra1, hm1);
            Interpretation inpre2 = new Interpretation(algebra2, hm2);

            ita.addInterpretation("FirstInput", inpre1);
            ita.addInterpretation("SecondInput", inpre2);

            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outs.get()))) {
                out.write(ita.toString());
            }
        }
        
        return new double[] {sumOfSizes / length, min, max};
    }
}
