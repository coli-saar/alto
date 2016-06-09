/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.learning.ExtractGrammar;
import de.up.ling.irtg.rule_finding.learning.SampleEM;
import de.up.ling.irtg.rule_finding.sampling.rule_weighters.SubtreeCounting;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.irtg.util.ProgressListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateSingularGrammarByEM {

    /**
     *
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);

        String inputFolder = props.getProperty("inputFolder");
        String treeOutput = props.getProperty("treeOutput");
        String logLikelihoodFile = props.getProperty("negativeLogLikelihoodOutput");
        String grammarOutput = props.getProperty("grammar");

        String firstAlgebra = props.getProperty("firstAlgebra");
        String firstAlgebraName = props.getProperty("firstAlgebraName");

        String adaptionSampleSize = props.getProperty("adaptionSampleSize");
        String resultSampleSize = props.getProperty("resultSampleSize");
        String sgdStepSize = props.getProperty("sgdStepSize");
        String adaptionRounds = props.getProperty("adaptionRounds");
        String trainIterations = props.getProperty("trainIterations");
        String normalizationExponent = props.getProperty("adaptionNormalizationExponent");
        String normalizationDivisor = props.getProperty("adaptionNormalizationDivisor");
        String lexiconAdditionFactor = props.getProperty("lexiconAdditionFactor");
        String resetEveryRound = props.getProperty("resetSamplerAdaptionEveryRound");
        String threads = props.getProperty("threads");

        String smooth = props.getProperty("smooth");
        String seed = props.getProperty("seed");

        Algebra a1 = (Algebra) Class.forName(firstAlgebra).newInstance();

        File[] inputs = new File(inputFolder).listFiles();
        Iterable<InputStream> instream = () -> {
            return new Iterator<InputStream>() {
                private int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < inputs.length;
                }

                @Override
                public InputStream next() {
                    File f = inputs[pos++];

                    try {
                        return new FileInputStream(f);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(CreateGrammarByRandomDraws.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                }
            };
        };

        IrtgInputCodec iic = new IrtgInputCodec();

        FunctionIterable<Signature, InputStream> sigs
                = new FunctionIterable<>(instream, (InputStream ins) -> {
                    try {
                        return iic.read(ins).getAutomaton().getSignature();
                    } catch (IOException | CodecParseException ex) {
                        Logger.getLogger(CreateSingularGrammarByEM.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                });

        //SubtreeCounting.CentralCounter counter = new SubtreeCounting.CentralCounter(Double.parseDouble(smooth), sigs);
        
        ProgressListener pog = (int done, int target, String line) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(done);
            sb.append(" / ");
            sb.append(target);
            sb.append("  ");
            sb.append(line);
            
            System.out.println(sb.toString());
        };

        SampleEM trex = new SampleEM();
        trex.setAdaptionRounds(Integer.parseInt(adaptionRounds));
        trex.setIterationProgress(pog);
        //trex.setLearningSize(Integer.parseInt(learnSampleSize));
        trex.setNormalizationDivisor(Double.parseDouble(normalizationDivisor));
        trex.setNormalizationExponent(Integer.parseInt(normalizationExponent));
        trex.setResultSize(Integer.parseInt(resultSampleSize));
        trex.setSampleSize(Integer.parseInt(adaptionSampleSize));
        trex.setSamplerLearningRate(Double.parseDouble(sgdStepSize));
        trex.setSeed(Long.parseLong(seed));
        trex.setSmooth(Double.parseDouble(smooth));
        trex.setTrainIterations(Integer.parseInt(trainIterations));

        trex.setLexiconAdditionFactor(Double.parseDouble(lexiconAdditionFactor));
        trex.setReset(Boolean.parseBoolean(resetEveryRound));
        trex.setThreads(Integer.parseInt(threads));
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logLikelihoodFile))) {
            Consumer<Double> cons = new Consumer<Double>() {
                /**
                 * 
                 */
                private final AtomicInteger ai = new AtomicInteger(0);
                {
                    bw.write ("Round;NegativeLogLikelihood");
                }
                
                @Override
                public void accept(Double d) {
                    System.out.println("NLL in this iteration: " + d);
                    try {
                        bw.newLine();
                        bw.write(Integer.toString(ai.incrementAndGet()));
                        bw.write(";");
                        bw.write(d.toString());
                    } catch (IOException ioe) {
                        System.out.println("problem writing out nll.");
                    }
                }
            };
            
            trex.setnLLTracking(cons);
            ExtractGrammar eg = new ExtractGrammar(a1, null, firstAlgebraName, null, trex,
                    firstAlgebraName, null);

            try (OutputStream trees = new FileOutputStream(treeOutput);
                    OutputStream grammar = new FileOutputStream(grammarOutput)) {
                eg.extractForFirstAlgebra(instream, trees, grammar);
            }
        }
    }
}
