/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.rule_finding.create_automaton.MakeIdIRTGForSingleSide;
import de.up.ling.irtg.rule_finding.create_automaton.MakeMonolingualAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.single_input_nonterminals.FromTreeWithBar;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class CreateSingleLanguageIRTGS {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String algebraTwo = props.getProperty("algebra2");
        String algebraThree = props.getProperty("algebra3");
        String targetAlgebraName = props.getProperty("treeAlgebraName");
        String algTwoName = props.getProperty("algebra2Name");
        String algThreeName = props.getProperty("algebra3Name");
        
        String corpusFile = props.getProperty("corpusFile");
        String automataFolder = props.getProperty("automataFolder");
        String rootState = props.getProperty("rootStateName");
        
        Algebra a1 = new MinimalTreeAlgebra();
        Algebra a2 = (Algebra) Class.forName(algebraTwo).newInstance();
        Algebra a3 = (Algebra) Class.forName(algebraThree).newInstance();
        
        Map<String,Algebra> map = new HashMap<>();
        map.put(targetAlgebraName, a1);
        map.put(algTwoName, a2);
        map.put(algThreeName, a2);
        
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(map);
        
        Reader corpus = new FileReader(corpusFile);
        
        Corpus corp = Corpus.readCorpus(corpus, ita);
        
        File f1 = new File(automataFolder);
        f1.mkdirs();
        
        int i = 0;
        for(Instance instance : corp) {
            int num = ++i;
            
            Object o = instance.getInputObjects().get(targetAlgebraName);
            
            TreeAutomaton ta = a1.decompose(o);
            
            String fileName = makeStandardName(f1, num);
            
            FromTreeWithBar ftwb = new FromTreeWithBar(ta);
            MakeMonolingualAutomaton mma = new MakeMonolingualAutomaton();
            
            ta = mma.introduce(ta, ftwb, rootState);
            InterpretedTreeAutomaton result = MakeIdIRTGForSingleSide.makeIRTG(ta, targetAlgebraName, new TreeAlgebra());
            try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
                out.write(result.toString());
            }
        }
    }
    
    /**
     * 
     * @param folder
     * @param num
     * @return 
     */
    public static String makeStandardName(File folder, int num) {
        return folder.getAbsolutePath()+File.separator+"input_"+num+".irtg";
    }
}
