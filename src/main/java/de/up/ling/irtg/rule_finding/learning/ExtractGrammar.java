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
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.io.BufferedReader;
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
public class ExtractGrammar<Type1,Type2> {
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
    private final StringSubtreeIterator.VariableMapping nonterminals;

    /**
     * 
     * @param algebra1
     * @param algebra2
     * @param nonterminals 
     */
    public ExtractGrammar(Algebra<Type1> algebra1, Algebra<Type2> algebra2, StringSubtreeIterator.VariableMapping nonterminals) {
        this.algebra1 = algebra1;
        this.algebra2 = algebra2;
        this.nonterminals = nonterminals;
    }
    
    /**
     * 
     * @param inputs
     * @param trees
     * @param irtg 
     */
    public void extract(Iterable<InputStream> inputs, OutputStream trees, OutputStream irtg) throws IOException {
        IrtgInputCodec iic = new IrtgInputCodec();
        
        Iterable<InterpretedTreeAutomaton> analyses = new FunctionIterable<>(inputs, (InputStream) -> {
            InterpretedTreeAutomaton ita;
            try {
                ita = iic.read(InputStream);
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
        Iterable<Tree<String>> solutions = mfv.getOptimalChoices(automata);
        
        RulePostProcessing<Type1,Type2> rpp = new RulePostProcessing<>(algebra1, algebra2);
        
        Iterator<Tree<String>> itSol = solutions.iterator();
        Iterator<InterpretedTreeAutomaton> itAnalysis = analyses.iterator();
        
        try (BufferedWriter solutionWriter = new BufferedWriter(new OutputStreamWriter(trees))) {
            while(itSol.hasNext() && itAnalysis.hasNext()){
                Tree<String> solution = itSol.next();
                InterpretedTreeAutomaton ita = itAnalysis.next();
                
                solutionWriter.write(solution.toString());
                solutionWriter.newLine();
                Map<String,Object> m = ita.interpret(solution);
                for(Map.Entry<String,Object> ent : m.entrySet()){
                    solutionWriter.write(ent.getKey());
                    solutionWriter.write('\t');
                    solutionWriter.write(ent.getValue().toString());
                }
                
                Iterator<Tree<String>> subtrees = StringSubtreeIterator.getSubtrees(solution, nonterminals);
                subtrees.forEachRemaining((Tree<String> t) -> {
                    //TODO, identify interpretation.
                });
            }
        }
        
        //TODO
    }
}
