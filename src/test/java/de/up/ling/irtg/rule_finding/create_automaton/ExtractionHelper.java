/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import de.up.ling.irtg.rule_finding.data_creation.MakeAutomata;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractionHelper {
    /**
     * 
     * @param data1
     * @param data2
     * @param alignment
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParserException 
     */
    public static Iterable<InputStream>[] getInputData(String data1, String data2,
                                    String alignment)
            throws IOException, ClassNotFoundException, InstantiationException,
                                       IllegalAccessException, ParserException {
        List<ByteArrayOutputStream> treeOutputs = new ArrayList<>();
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            treeOutputs.add(result);

            return result;
        };

        ArrayList<Iterable<InputStream>> result = new ArrayList<>();
        
        InputStream inTrees = new ByteArrayInputStream(data1.getBytes());
        MakeAutomata.create(inTrees, supp);
        List<InputStream> treeInputs = new ArrayList<>();
        for (ByteArrayOutputStream baos : treeOutputs) {
            treeInputs.add(new ByteArrayInputStream(baos.toByteArray()));
        }
        
        result.add(treeInputs);

        treeOutputs.clear();

        inTrees = new ByteArrayInputStream(data2.getBytes());

        MakeAutomata.create(inTrees, supp);
        treeInputs = new ArrayList<>();
        for (ByteArrayOutputStream baos : treeOutputs) {
            treeInputs.add(new ByteArrayInputStream(baos.toByteArray()));
        }

        result.add(treeInputs);

        treeOutputs.clear();
        InputStream align = new ByteArrayInputStream(alignment.getBytes());

        MakeAlignments.makeStringFromStandardAlign(align, supp, false);

        List<InputStream> align1 = new ArrayList<>();
        treeOutputs.stream().forEach((baos) -> {
            align1.add(new ByteArrayInputStream(baos.toByteArray()));
        });

        result.add(align1);
        
        align = new ByteArrayInputStream(alignment.getBytes());

        MakeAlignments.makeStringFromStandardAlign(align, supp, true);

        List<InputStream> align2 = new ArrayList<>();
        treeOutputs.stream().forEach((baos) -> {
            align2.add(new ByteArrayInputStream(baos.toByteArray()));
        });
        
        result.add(align2);
        
        return result.toArray(new Iterable[result.size()]);
    }
    
    public static Iterable<String> makeIRTGs(String data1, String data2,
                                    String alignment, Pruner p1, Pruner p2) throws IOException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        Iterable<InputStream>[] data = getInputData(data1, data2, alignment);
        
        CorpusCreator corCre = new CorpusCreator(p2, p2, 2);
        
        ExtractJointTrees ejt = new ExtractJointTrees(corCre);
        ArrayList<ByteArrayOutputStream> results = new ArrayList<>();
        
        Supplier<OutputStream> outs = () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            results.add(baos);
            
            return baos;
        };
        
        double[] sizes = ejt.getAutomataAndMakeStatistics(data[0], data[1], data[2], data[3], outs);
        
        List<String> solutions = new ArrayList<>();
        for(ByteArrayOutputStream outStr : results) {
            solutions.add(outStr.toString());
        }
        
        System.out.println(Arrays.toString(sizes));
        return solutions;
    }
}
