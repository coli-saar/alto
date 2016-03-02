/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateJointTrees {
    /**
     *
     * @param args
     * @throws java.io.IOException
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    public static void main(String... args) throws IOException, ParserException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String firstAutomatonFolder = props.getProperty("firstAutomatonFolder");
        String secondAutomatonFolder = props.getProperty("secondAutomatonFolder");
        String firstAlignmentFolder = props.getProperty("firstAlignmentFolder");
        String secondAlignmentFolder = props.getProperty("secondAlignmentFolder");
        String firstPrunerSpecification = props.getProperty("firstPrunerSpecification");
        String secondPrunerSpecification = props.getProperty("secondPrunerSpecification");
        String threads = props.getProperty("threads");
        String outputFolder = props.getProperty("outputFolder");

        File fAutFolder = new File(firstAutomatonFolder);
        File sAutFolder = new File(secondAutomatonFolder);
        File fAlFolder = new File(firstAlignmentFolder);
        File sAlFolder = new File(secondAlignmentFolder);

        File[] children = fAutFolder.listFiles();

        List<String> files = new ArrayList<>();
        for (File child : children) {
            String name = child.getName();

            boolean isAdmissible = true;
            File test = new File(sAlFolder.getAbsolutePath() + File.separator + name);
            isAdmissible &= test.isFile() && !test.isDirectory();

            test = new File(fAlFolder.getAbsolutePath() + File.separator + name);
            isAdmissible &= test.isFile() && !test.isDirectory();

            test = new File(sAutFolder.getAbsolutePath() + File.separator + name);
            isAdmissible &= test.isFile() && !test.isDirectory();

            if (isAdmissible) {
                files.add(name);
            }
        }

        Function<String, Iterable<InputStream>> createIt = (String prefix) -> {
            return (Iterable<InputStream>) () -> new Iterator<InputStream>() {
                private int pos = 0;
                
                @Override
                public boolean hasNext() {
                    return pos < files.size();
                }
                
                @Override
                public InputStream next() {
                    String name = files.get(pos++);
                    
                    File source = new File(prefix + File.separator + name);
                    
                    try {
                        return new FileInputStream(source);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(CreateJointTrees.class.getName()).log(Level.SEVERE, null, ex);
                        
                        System.out.println("-------");
                        System.out.println(ex);
                        System.out.println("filename: "+name);
                        System.out.println("-------");
                        
                        throw new RuntimeException(ex);
                    }
                }
            };
        };

        Iterable<InputStream> autIt1 = createIt.apply(fAutFolder.getAbsolutePath());
        Iterable<InputStream> autIt2 = createIt.apply(sAutFolder.getAbsolutePath());
        Iterable<InputStream> alIt1 = createIt.apply(fAlFolder.getAbsolutePath());
        Iterable<InputStream> alIt2 = createIt.apply(sAlFolder.getAbsolutePath());
        
        File outFolder = new File(outputFolder);
        outFolder.mkdirs();
        
        Supplier<OutputStream> supp = new Supplier<OutputStream>() {
            int pos = 0;
            
            
            @Override
            public OutputStream get() {
                String name = files.get(pos++);
                
                File f = new File(outFolder.getAbsolutePath() + File.separator + name+".irtg");
                
                try {
                    return new FileOutputStream(f);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CreateJointTrees.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
        };
        
        CorpusCreator corpCreat = createCreator(firstPrunerSpecification, secondPrunerSpecification, threads);
        ExtractJointTrees ajt = new ExtractJointTrees(corpCreat);

        ajt.getAutomataAndMakeStatistics(autIt1, autIt2, alIt1, alIt2, supp);
    }

    /**
     *
     * @param firstPrunerSpecification
     * @param secondPrunerSpecification
     * @param threads
     * @return
     * @throws IOException
     * @throws NumberFormatException
     */
    private static CorpusCreator createCreator(String firstPrunerSpecification, String secondPrunerSpecification, String threads) throws IOException, NumberFormatException {
        List<String> prunName = new ArrayList<>();
        List<String> prunOption = new ArrayList<>();
        try (BufferedReader pReader = new BufferedReader(new FileReader(firstPrunerSpecification))) {
            String line;

            while ((line = pReader.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    continue;
                }
                
                prunName.add(line);
                prunOption.add(pReader.readLine().trim());
            }
        }
        int size = prunName.size();
        Pruner first = new IntersectionPruner(prunName.toArray(new String[size]), prunOption.toArray(new String[size]));
        
        prunName = new ArrayList<>();
        prunOption = new ArrayList<>();
        try (BufferedReader pReader = new BufferedReader(new FileReader(secondPrunerSpecification))) {
            String line;

            while ((line = pReader.readLine()) != null) {
                prunName.add(line.trim());
                prunOption.add(pReader.readLine().trim());
            }
        }
        size = prunName.size();
        Pruner second = new IntersectionPruner(prunName.toArray(new String[size]), prunOption.toArray(new String[size]));
        CorpusCreator corpCreat = new CorpusCreator(first, second, Integer.parseInt(threads.trim()));
        return corpCreat;
    }
}
