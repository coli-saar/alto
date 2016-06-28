/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Creates a set of files that contain string alignments, but are named according
 * to the current graph file naming schema (number_nrNodes.rtg).
 * @author groschwitz
 */
public class CreateStringAlignmentsWithGraphNames {

    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFile = props.getProperty("inputFile");
        String outputFolder = props.getProperty("outputFolder");
        String useRight = props.getProperty("useRight");
        String corpusFile = props.getProperty("corpusFile");
        
        
        //setup corpus form input
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(null);
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        Corpus graphCorpus = Corpus.readCorpus(new FileReader(corpusFile), irtg4Corpus);
        
        InputStream align = new FileInputStream(inputFile);
        boolean useR = Boolean.parseBoolean(useRight);
        File outputFile = new File(outputFolder);
        outputFile.mkdirs();
        
        Iterator<Instance> instIt = graphCorpus.iterator();
        
        Supplier<OutputStream> supp = new Supplier<OutputStream>() {
            /**
             * 
             */
            private int nameNum = 0;
            
            @Override
            public OutputStream get() {
                int num = nameNum++;
                
                int nrNodes = ((SGraph)instIt.next().getInputObjects().get("graph")).getAllNodeNames().size();
                
                String name = outputFolder+"/"+num+"_"+nrNodes+".rtg";
                
                try {
                    return new FileOutputStream(name);
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        
        
        MakeAlignments.makeStringFromStandardAlign(align, supp, useR);
    }   
}
