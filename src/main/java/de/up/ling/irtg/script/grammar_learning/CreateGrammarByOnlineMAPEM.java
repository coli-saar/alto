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
import de.up.ling.irtg.rule_finding.sampling.models.IndependentTrees;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateGrammarByOnlineMAPEM {
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
        String grammarOutput = props.getProperty("grammar");
        String firstAlgebra = props.getProperty("firstAlgebra");
        String secondAlgebra = props.getProperty("secondAlgebra");
        String samplerSmooth = props.getProperty("samplerSmooth");
        String sampleSize = props.getProperty("sampleSize");
        String learnSize = props.getProperty("learnSize");
        String learnSampleSize = props.getProperty("learnSampleSize");
        String adaptionRounds = props.getProperty("adaptionRounds");
        String trainIterations = props.getProperty("trainIterations");
        String modelSmooth = props.getProperty("modelSmooth");
        String logInformatWrongess = props.getProperty("logInformantWrongness");
        String desiredAvergeSize = props.getProperty("desiredAverageSize");
        String outInterpretation1 = props.getProperty("firstAlgebraName");
        String outInterpretation2 = props.getProperty("secondAlgebraName");
        
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
        
        
        double mSmooth = Double.parseDouble(modelSmooth);
        double logWrong = Double.parseDouble(logInformatWrongess);
        int desiredSize = Integer.parseInt(desiredAvergeSize);
        IrtgInputCodec iic = new IrtgInputCodec();
        
        FunctionIterable<Signature, InputStream> sigs =
                new FunctionIterable<>(instream,(InputStream ins) -> {
            try {
                return iic.read(ins).getAutomaton().getSignature();
            } catch (IOException |CodecParseException  ex) {
                Logger.getLogger(CreateGrammarByOnlineMAPEM.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        });
        
        IndependentTrees indT = new IndependentTrees(mSmooth, logWrong, desiredSize, sigs);
        
        SampleEM trex = new SampleEM();
        //TODO TODO
        
        ExtractGrammar eg = new ExtractGrammar(a1, a2, ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID, trex,
        outInterpretation1,outInterpretation2);
        
        try(OutputStream trees = new FileOutputStream(treeOutput);
                OutputStream grammar = new FileOutputStream(grammarOutput)) {
            /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(grammar));
            bw.write("# "+BuildProperties.getBuild());
            bw.newLine();
            bw.write("# "+BuildProperties.getVersion());
            bw.newLine();
            bw.flush();*/
            
            eg.extract(instream, trees, grammar);
        }
    }
}
