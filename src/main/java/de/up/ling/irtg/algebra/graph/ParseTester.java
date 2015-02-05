/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
//import java.sql.Date;
import java.util.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jonas
 */
public class ParseTester {

    public static AverageLogger averageLogger = new AverageLogger();

    public static void main(String[] args) throws Exception {

        Reader corpusReader = new FileReader("corpora-and-grammars/corpora/amr-bank-v1.3.txt");
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        inducer.getCorpus().sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));

        int start = 0;
        int stop = 1000;

        int warmupIterations = 10;
        int iterations = 1;
        int internalIterations = 1;

        IntList failed = new IntArrayList();

        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
        List<String> labels = new ArrayList<>();

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("corpora-and-grammars/grammars/LittlePrinceSubtreesTyped.txt"));

        //uncomment this to write a log of the pattern matching:
        //irtg.getInterpretation("int").setPmLogName("AfterMergingStartStatesInto_q");
        for (int j = 0; j < warmupIterations; j++) {
            sw = new CpuTimeStopwatch();
            sw.record(0);
            for (int i = start; i < stop; i++) {
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, internalIterations, internalSw);
                System.err.print(" i = " + i);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
            System.err.println();
            sw.record(1);
            sw.printMilliseconds("warmup run " + (j+1));
        }

        sw.record(0);
        Writer resultWriter = new FileWriter("logs/resultsParseTester" + (new Date()).toString() + ".txt");
        StringJoiner sj = new StringJoiner(",");
        sj.add("Original number");
        sj.add("Ordering number");
        sj.add("Node count");
        sj.add("Edge count");
        sj.add("Node + Edge count");
        sj.add("Time");
        sj.add("Language size");
        resultWriter.write(sj.toString() + "\n");

        for (int j = 0; j < iterations; j++) {
            averageLogger = new AverageLogger();
            averageLogger.activate();
            //averageLogger.deactivate();
            for (int i = start; i < stop; i++) {
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, internalIterations, internalSw);
                System.err.print("i = " + i);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
            System.err.println();
            averageLogger.setDefaultCount((stop - start) * internalIterations);
            averageLogger.printAveragesAsError();
        }
        resultWriter.close();

        sw.record(1);

        sw.printMilliseconds("parsing trees from " + start + " to " + stop + "(" + (iterations * internalIterations) + " iterations)");

        /*Writer logWriter = new FileWriter(dumpPath +"log.txt");
         sw.record(2 * iterations);
        
         logWriter.write("Total: " + String.valueOf(iterations)+"\n");
         logWriter.write("Failed: " + String.valueOf(failed)+"\n");
         logWriter.write(sw.toMilliseconds("\n", labels.toArray(new String[labels.size()])));
         logWriter.close();*/
    }

    public static void parseInstanceWithIrtg(List<IrtgInducer.TrainingInstance> corpus, InterpretedTreeAutomaton irtg, int i, Writer resultWriter, int internalIterations, CpuTimeStopwatch internalSw) {
        IrtgInducer.TrainingInstance ti = corpus.get(i);
        internalSw.record(0);
        TreeAutomaton chart = null;
//        System.err.println("\n" + ti.graph);
        for (int j = 0; j < internalIterations; j++) {
            Map<String, Object> input = new HashMap<>();
            input.put("int", ti.graph);
            chart = irtg.parseInputObjects(input);
        }
        internalSw.record(1);
        if (resultWriter != null) {
            StringJoiner sj = new StringJoiner(",");
            sj.add(String.valueOf(ti.id));
            sj.add(String.valueOf(i));
            sj.add(String.valueOf(ti.graph.getAllNodeNames().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()) + ti.graph.getAllNodeNames().size());
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(chart.language().size()));
            try {
                resultWriter.write(sj.toString() + "\n");
            } catch (IOException ex) {
                Logger.getLogger(ParseTester.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /*if (!chart.language().isEmpty()) {
         averageLogger.increaseCount("nonemptyLanguageSize");
         averageLogger.increaseValueBy("nonemptyLanguageSize", chart.language().size());
         }*/
//        System.err.println(chart.viterbi());
        //System.err.println(chart);
    }

}
