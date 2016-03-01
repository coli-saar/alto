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
import de.up.ling.tree.ParseException;
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
    public static Iterable<InputStream>[] getStringStringInputData(String data1, String data2,
                                    String alignment)
            throws IOException, ClassNotFoundException, InstantiationException,
                                       IllegalAccessException, ParserException {
        ArrayList<Iterable<InputStream>> result = new ArrayList<>();
        
        constructAutomata(result, data1, data2);

        makeStringToStringAlignments(alignment, result);
        
        return result.toArray(new Iterable[result.size()]);
    }

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
     * @throws ParseException 
     */
    public static Iterable<InputStream>[] getStringTreeInputData(String data1, String data2,
                                    String alignment)
            throws IOException, ClassNotFoundException, InstantiationException,
                                       IllegalAccessException, ParserException, ParseException {
        ArrayList<Iterable<InputStream>> result = new ArrayList<>();
        
        constructAutomata(result, data1, data2);

        makeStringToTreeAlignments(alignment, data2, result);
        
        return result.toArray(new Iterable[result.size()]);
    }
    
    /**
     * 
     * @param data1
     * @param data2
     * @param alignment
     * @param p1
     * @param p2
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParserException
     * @throws ParseException 
     */
    public static Iterable<String> getTreeIRTGs(String data1, String data2,
                                    String alignment, Pruner p1, Pruner p2)
            throws IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, ParserException, ParseException {
        Iterable<InputStream>[] data = getStringTreeInputData(data1, data2, alignment);
        
        CorpusCreator corCre = new CorpusCreator(p1, p2, 2);
        
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
    
    
    /**
     * 
     * @param alignment
     * @param result
     * @throws IOException 
     */
    private static void makeStringToStringAlignments(String alignment, ArrayList<Iterable<InputStream>> result) throws IOException {
        List<ByteArrayOutputStream> intermediateAlignments = new ArrayList<>();
        
        intermediateAlignments.clear();
        InputStream align = new ByteArrayInputStream(alignment.getBytes());
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream done = new ByteArrayOutputStream();

            intermediateAlignments.add(done);

            return done;
        };
        
        MakeAlignments.makeStringFromStandardAlign(align, supp, false);

        List<InputStream> align1 = new ArrayList<>();
        intermediateAlignments.stream().forEach((baos) -> {
            align1.add(new ByteArrayInputStream(baos.toByteArray()));
        });

        result.add(align1);
        
        align = new ByteArrayInputStream(alignment.getBytes());

        intermediateAlignments.clear();
        MakeAlignments.makeStringFromStandardAlign(align, supp, true);

        List<InputStream> align2 = new ArrayList<>();
        intermediateAlignments.stream().forEach((baos) -> {
            align2.add(new ByteArrayInputStream(baos.toByteArray()));
        });
        
        result.add(align2);
    }
    
    /**
     * 
     * @param alignment
     * @param trees
     * @param result
     * @throws IOException
     * @throws ParseException 
     */
    private static void makeStringToTreeAlignments(String alignment, String trees,
            ArrayList<Iterable<InputStream>> result) throws IOException, ParseException, ParserException {
        List<ByteArrayOutputStream> intermediateAlignments = new ArrayList<>();
        
        intermediateAlignments.clear();
        InputStream align = new ByteArrayInputStream(alignment.getBytes());
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream done = new ByteArrayOutputStream();

            intermediateAlignments.add(done);

            return done;
        };
        
        MakeAlignments.makeStringFromStandardAlign(align, supp, false);

        List<InputStream> align1 = new ArrayList<>();
        intermediateAlignments.stream().forEach((baos) -> {
            align1.add(new ByteArrayInputStream(baos.toByteArray()));
        });

        result.add(align1);
        
        align = new ByteArrayInputStream(alignment.getBytes());
        InputStream origTrees = new ByteArrayInputStream(trees.getBytes());
        
        intermediateAlignments.clear();
        MakeAlignments.makePreorderTreeFromStandard(align, origTrees, supp, true, 1);

        List<InputStream> align2 = new ArrayList<>();
        intermediateAlignments.stream().forEach((baos) -> {
            align2.add(new ByteArrayInputStream(baos.toByteArray()));
        });
        
        result.add(align2);
    }
    
    /**
     * 
     * @param result
     * @param data1
     * @param data2
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParserException 
     */
    public static void constructAutomata(ArrayList<Iterable<InputStream>> result,
                                    String data1, String data2) throws IOException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        List<ByteArrayOutputStream> treeOutputs = new ArrayList<>();
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream done = new ByteArrayOutputStream();

            treeOutputs.add(done);

            return done;
        };
        
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
    }
    
    
    /**
     * 
     * @param data1
     * @param data2
     * @param alignment
     * @param p1
     * @param p2
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParserException 
     */
    public static Iterable<String> getStringIRTGs(String data1, String data2,
                                    String alignment, Pruner p1, Pruner p2) throws IOException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        Iterable<InputStream>[] data = getStringStringInputData(data1, data2, alignment);
        
        CorpusCreator corCre = new CorpusCreator(p1, p2, 2);
        
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
