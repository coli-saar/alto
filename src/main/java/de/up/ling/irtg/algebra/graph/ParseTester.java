/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jonas
 */
public class ParseTester {
    
    public static void main(String[] args) throws Exception{
        
        Reader corpusReader = new FileReader("corpora/amr-bank-v1.3.txt");
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        inducer.corpus.sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));
        
        int start = 0;
        int stop = 1000;
  
        int warmupIterations = 0;
        int iterations = 10;
        
        IntList failed = new IntArrayList();


        
        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        List<String> labels = new ArrayList<>();
        

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/LittlePrinceSubtrees.irtg"));
        
        
        
        
        
        //uncomment this to write a log of the pattern matching:
        //irtg.getInterpretation("int").setPmLogName("AfterMergingStartStatesInto_q");
        
        
        
        
        for (int j = 0; j<warmupIterations; j++) {
            for (int i = start; i < stop; i++) {
                parseInstanceWithIrtg(inducer.corpus, irtg, i);
                System.err.println("i = " + i);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
        }
        
        sw.record(0);
        
        
        for (int j = 0; j<iterations; j++) {
            for (int i = start; i < stop; i++) {
                parseInstanceWithIrtg(inducer.corpus, irtg, i);
                System.err.println("i = " + i);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
        }

        
        sw.record(1);
        
        sw.printMilliseconds("parsing trees from " + start + " to " + stop +"("+iterations + " iterations)");
        
        
        
        
        
        
        
        /*Writer logWriter = new FileWriter(dumpPath +"log.txt");
        sw.record(2 * iterations);
        
        logWriter.write("Total: " + String.valueOf(iterations)+"\n");
        logWriter.write("Failed: " + String.valueOf(failed)+"\n");
        logWriter.write(sw.toMilliseconds("\n", labels.toArray(new String[labels.size()])));
        logWriter.close();*/
        
        
    }
    
    public static void parseInstanceWithIrtg(List<IrtgInducer.TrainingInstance> corpus, InterpretedTreeAutomaton irtg, int i) {
        IrtgInducer.TrainingInstance ti = corpus.get(i);
        
//        System.err.println("\n" + ti.graph);
        
        Map<String, Object> input = new HashMap<>();
        input.put("int", ti.graph);
        TreeAutomaton chart = irtg.parseInputObjects(input);
        
//        System.err.println(chart.viterbi());
        
        //System.err.println(chart);
    }
    
    
}
