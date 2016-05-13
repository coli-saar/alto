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
import de.up.ling.irtg.rule_finding.sampling.RuleWeighters.SubtreeCounting;
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
public class CreateGrammarByEM {

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
        String secondAlgebra = props.getProperty("secondAlgebra");

        String adaptionSampleSize = props.getProperty("adaptionSampleSize");
        String learnSampleSize = props.getProperty("learnSampleSize");
        String resultSampleSize = props.getProperty("resultSampleSize");
        String sgdStepSize = props.getProperty("sgdStepSize");
        String adaptionRounds = props.getProperty("adaptionRounds");
        String trainIterations = props.getProperty("trainIterations");
        String normalizationExponent = props.getProperty("adaptionNormalizationExponent");
        String normalizationDivisor = props.getProperty("adaptionNormalizationDivisor");

        String smooth = props.getProperty("smooth");

        String outInterpretation1 = props.getProperty("firstAlgebraName");
        String outInterpretation2 = props.getProperty("secondAlgebraName");

        String seed = props.getProperty("seed");

        Algebra a1 = (Algebra) Class.forName(firstAlgebra).newInstance();
        Algebra a2 = (Algebra) Class.forName(secondAlgebra).newInstance();

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
                        Logger.getLogger(CreateGrammarByEM.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                });

        SubtreeCounting.CentralCounter counter = new SubtreeCounting.CentralCounter(Double.parseDouble(smooth), sigs);

        ProgressListener pog = (int done, int target, String line) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(done);
            sb.append(" / ");
            sb.append(target);
            sb.append("  ");
            sb.append(line);
        };

        SampleEM trex = new SampleEM();
        trex.setAdaptionRounds(Integer.parseInt(adaptionRounds));
        trex.setIterationProgress(pog);
        trex.setLearningSize(Integer.parseInt(learnSampleSize));
        trex.setNormalizationDivisor(Double.parseDouble(normalizationDivisor));
        trex.setNormalizationExponent(Integer.parseInt(normalizationExponent));
        trex.setResultSize(Integer.parseInt(resultSampleSize));
        trex.setSampleSize(Integer.parseInt(adaptionSampleSize));
        trex.setSamplerLearningRate(Double.parseDouble(sgdStepSize));
        trex.setSeed(Long.parseLong(seed));
        trex.setSmooth(Double.parseDouble(smooth));
        trex.setTrainIterations(Integer.parseInt(trainIterations));

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
                        bw.write(Integer.toString(ai.incrementAndGet()));
                        bw.write(";");
                        bw.write(d.toString());
                        bw.newLine();
                    } catch (IOException ioe) {
                        System.out.println("problem writing out nll.");
                    }
                }
            };
            trex.setnLLTracking(cons);
            ExtractGrammar eg = new ExtractGrammar(a1, a2, ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID, trex,
                    outInterpretation1, outInterpretation2);

            try (OutputStream trees = new FileOutputStream(treeOutput);
                    OutputStream grammar = new FileOutputStream(grammarOutput)) {
                eg.extract(instream, trees, grammar);
            }
        }
    }
}
