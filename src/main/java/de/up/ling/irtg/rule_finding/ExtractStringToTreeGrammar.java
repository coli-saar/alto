/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.align.find_rules.SampledEM;
import de.up.ling.tree.Tree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractStringToTreeGrammar {
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     * @throws de.up.ling.irtg.algebra.ParserException 
     * @throws java.util.concurrent.ExecutionException 
     * @throws java.lang.InterruptedException 
     */
    public static void main(String... args) throws IOException, ParserException, ExecutionException, InterruptedException {
        /**
        StringAlgebra str = new StringAlgebra();
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        ArrayList<CreateCorpus.InputPackage> stringInputs = new ArrayList<>();
        ArrayList<CreateCorpus.InputPackage> treeInput = new ArrayList<>();
        
        CreateCorpus<List<String>,Tree<String>> cc = new CreateCorpus<>(str,mta);
        
        Propagator propDef = new Propagator();
        
        Propagator propLarge = new Propagator(new StringLeftOrRight());
        Propagator propSmall = new Propagator();
        
        SampledEM sem = new SampledEM(2, 1.0, 1.0, 10, 1.0);
        
        Function<List<String>,Propagator> funct = (List<String> in) -> {
            //TODO
            //if(in.size() > 15){
            //    return propLarge;
            //}
            
            return propLarge;
        };
        SpanAligner.Factory spanFactory = new SpanAligner.Factory();
        
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        String line;
        while((line = br.readLine()) != null){
            line = line.trim();
            if(line.equals("")){
                continue;
            }
            
            stringInputs.add(new CreateCorpus.InputPackage(line, "", funct, spanFactory));     
        }
        br.close();
        
        Function<Tree<String>,Propagator> f = (Tree<String> in) -> {
            return propDef;
        };
        AddressAligner.Factory addFactory = new AddressAligner.Factory();
        
        br = new BufferedReader(new FileReader(args[1]));
        while((line = br.readLine()) != null){
            line = line.trim();
            if(line.equals("")){
                continue;
            }
            
            treeInput.add(new CreateCorpus.InputPackage(line, "", f, addFactory));
        }
        br.close();
        
        List<TreeAutomaton> data = cc.makeDataSet(stringInputs, treeInput);
        
        List<SampledEM.LearningInstance> insts = sem.makeInstances(2, 500, cc, data, 0.01, 29919799895L, 5);
        InterpretedTreeAutomaton ita = sem.makeGrammar(cc, 20, insts);
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(args[2]));
        bw.write(ita.toString());
        bw.close();
        */
    }
}
