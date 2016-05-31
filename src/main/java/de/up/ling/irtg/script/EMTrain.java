/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.corpus.ChartAttacher;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class EMTrain {
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     * @throws de.up.ling.irtg.corpus.CorpusReadingException 
     */
    public static void main(String... args) throws IOException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String corpusFile = props.getProperty("corpusFile");
        String irtgFile = props.getProperty("grammarFile");
        String vb = props.getProperty("useVariational");
        String iterations = props.getProperty("iterations");
        String result = props.getProperty("resultFile");
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita;
        try(FileInputStream fis = new FileInputStream(irtgFile)) {
            ita = iic.read(fis);
        }
        
        Corpus c;
        try(FileReader fr = new FileReader(corpusFile)) {
            c = Corpus.readCorpusLenient(fr, ita);
        }
        
        int iterate = Integer.parseInt(iterations);
        ProgressListener pol = (int currentValue, int maxValue, String string) -> {
            System.err.println(currentValue + " / "+maxValue);
            System.err.println(string);
        };
        
        c.attachCharts((Iterator<Instance> source) -> new Iterator<Instance>() {
            @Override
            public boolean hasNext() {
                return source.hasNext();
            }
            
            @Override
            public Instance next() {
                Instance ins = source.next();
                ins.setChart(ita.parseInputObjects(ins.getInputObjects()));
                
                return ins;
            }
        });
        
        if(Boolean.parseBoolean(vb)) {
            ita.trainVB(c, iterate, 0.0, pol);
        } else {
            ita.trainEM(c, iterate, 0.0, pol);
        }
        
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(result))) {
            bw.write(ita.toString());
        }
    }
}
