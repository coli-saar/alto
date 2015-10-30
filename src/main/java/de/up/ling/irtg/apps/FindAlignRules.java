/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.apps;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.creation.Propagator;
import de.up.ling.irtg.align.RuleFinder;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.automata.FromRuleTreesAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author christoph_teichmann
 */
public class FindAlignRules {

    public static void main(String... args) throws IOException, InterruptedException {
        int threads = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        String resultPrefix = args[1];

        int number = 1;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        Semaphore sm = new Semaphore(0);
        
        System.out.println("version 4");

        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {
            String line;

            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String line2 = in.readLine().trim();
                String align1 = in.readLine();
                String align2 = in.readLine();

                String[] parts1 = line.split("\\s+");
                String[] parts2 = line2.split("\\s+");

                Job j = new Job(parts1, parts2, resultPrefix, number++, sm, align1, align2);
                ex.submit(j);
            }

            sm.acquire(number-1);
            ex.shutdown();
        }

    }

    /**
     *
     */
    private static class Job implements Runnable {

        /**
         *
         */
        private final String s1;

        /**
         *
         */
        private final String s2;

        /**
         *
         */
        private final String fileName;
        
        /**
         * 
         */
        private final Semaphore sm;
        
        /**
         * 
         */
        private final String align1;
        
        /**
         * 
         */
        private final String align2;

        /**
         *
         * @param parts1
         * @param parts2
         * @param resultPrefix
         * @param i
         */
        private Job(String[] parts1, String[] parts2, String resultPrefix, int i, Semaphore sm, String align1,
                String align2) {
            s1 = Arrays.stream(parts1, 1, parts1.length).collect(Collectors.joining(" "));
            s2 = Arrays.stream(parts2, 1, parts2.length).collect(Collectors.joining(" "));

            this.fileName = resultPrefix + "_" + i + ".irtg";
            this.sm = sm;
            
            this.align1 = align1;
            this.align2 = align2;
        }

        @Override
        public void run() {
            StringAlgebra alg1 = new StringAlgebra();
            StringAlgebra alg2 = new StringAlgebra();

            HomomorphismManager homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
            
            List<String> list1 = alg1.parseString(this.s1);
            List<String> list2 = alg2.parseString(this.s2);

            TreeAutomaton t1 = alg1.decompose(list1);
            TreeAutomaton t2 = alg2.decompose(list2);

            SpanAligner spa1 = new SpanAligner(align1, t1);
            SpanAligner spa2 = new SpanAligner(align2, t2);

            if (list1.size() > 10 || list2.size() > 10) {
                int samps = list1.size() > 15 || list2.size() > 15 ? 2 : 5;
                FromRuleTreesAutomaton sample = new FromRuleTreesAutomaton(t1);

                for (int i = 0; i < samps; ++i) {
                    Tree<Rule> samp = t1.getRandomRuleTreeFromInside();
                    sample.addRules(samp);
                }

                t1 = sample;

                sample = new FromRuleTreesAutomaton(t2);
                for (int i = 0; i < samps; ++i) {
                    Tree<Rule> samp = t2.getRandomRuleTreeFromInside();
                    sample.addRules(samp);
                }
                t2 = sample;
            }
            
            Propagator prop = new Propagator();
        
            t1 = prop.convert(t1, spa1);
            t2 = prop.convert(t2, spa2);
            
            RuleFinder rf = new RuleFinder();

            TreeAutomaton t = rf.getRules(t1, t2, homa);
            
            InterpretedTreeAutomaton ita = rf.getInterpretation(t, homa, alg1, alg2);
            
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))){
                
                bw.write(ita.toString());
                
            } catch (IOException ex) {
                System.out.println(ex);
                Logger.getLogger(FindRules.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                sm.release(1);
                System.out.println("finished "+fileName);
            }
        }
    }
}