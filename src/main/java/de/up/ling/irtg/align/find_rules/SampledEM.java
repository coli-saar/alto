/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.find_rules.sampling.Model;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author christoph_teichmann
 */
public class SampledEM {
 
    /**
     * 
     */
    private final int useThreads;

    /**
     * 
     */
    private final int batchSize;
    
    /**
     * 
     */
    private final double scaling;

    /**
     * 
     * @param useThreads
     * @param smooth
     * @param batchSize
     * @param scaling
     * @param emptynessConstraint
     * @param delexicalizationConstraint 
     */
    public SampledEM(int useThreads,int batchSize, double scaling) {
        this.useThreads = useThreads;
        this.batchSize = batchSize;
        this.scaling = scaling;
    }
    
    /**
     * 
     * 
     * @param cc
     * @param learningRounds
     * @param instances
     * @param model
     * @return
     * @throws ParserException
     * @throws ExecutionException
     * @throws InterruptedException 
     */
    public List<List<Tree<Rule>>> makeGrammar(CreateCorpus cc, int learningRounds,
            List<LearningInstance> instances, Model model)
            throws ParserException, ExecutionException, InterruptedException{
        List<List<Tree<Rule>>> corpus = runTraining(learningRounds, instances, model);
        
        return corpus;
    }

    /**
     * 
     * @param threads
     * @param cc
     * @param variableSmooth
     * @param mainSmooth
     * @param vi
     * @param learningRounds
     * @param jobs
     * @return
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private List<List<Tree<Rule>>> runTraining(int learningRounds, List<LearningInstance> jobs,
            Model mod) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(this.useThreads);
        
        List<LearningInstance> intermediate = new ObjectArrayList<>();
        
        pushModel(jobs, mod);
        
        train(learningRounds, jobs, intermediate, es, mod);
        
        List<List<Tree<Rule>>> result = new ArrayList<>();
        List<Future<List<Tree<Rule>>>> l = es.invokeAll(jobs);
        es.shutdown();
        
        for(int i=0;i<l.size();++i){
            result.add(l.get(i).get());   
        }
        
        return result;
    }

    /**
     * 
     * @param learningRounds
     * @param jobs
     * @param intermediate
     * @param es
     * @param mod
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void train(int learningRounds, List<LearningInstance> jobs,
            List<LearningInstance> intermediate, ExecutorService es, Model mod)
                                    throws InterruptedException, ExecutionException {
        for (int round = 0; round < learningRounds; ++round) {
            for (int i = 0; i < jobs.size();) {
                int k = 0;

                intermediate.clear();
                for (; i + k < jobs.size() && k < this.batchSize; ++k) {
                    intermediate.add(jobs.get(i + k));
                }
                
                i += k;

                List<Future<List<Tree<Rule>>>> results = es.invokeAll(intermediate);
                
                for (int pos=0;pos<results.size();++pos) {
                    List<Tree<Rule>> choices = results.get(pos).get();
                    double amount = scaling / ((double) choices.size());
                    for (int h = 0; h < choices.size(); ++h) {
                        mod.add(choices.get(h), amount);
                    }
                }
            }
            
            System.out.println("finished one round of training");
        }
    }

    /**
     * 
     * @param jobs
     * @param mod 
     */
    private void pushModel(List<LearningInstance> jobs, Model mod) {
        for(int i=0;i<jobs.size();++i){
            jobs.get(i).setModel(mod);
        }
    }
    
    /**
     * 
     */
    public static class SampledEMFactory{
        /**
         * 
         */
        private int numberOfThreads;
        
        /**
         * 
         */
        private int batchSize;
        
        /**
         * 
         */
        private double scaling;
        
        /**
         * 
         */
        public SampledEMFactory(){
            numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            batchSize = numberOfThreads*10;
            this.scaling = 1.0;
        }

        /**
         * 
         * @param numberOfThreads
         * @return 
         */
        public SampledEMFactory setNumberOfThreads(int numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        /**
         * 
         * @param batchSize
         * @return 
         */
        public SampledEMFactory setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * 
         * @param scaling
         * @return 
         */
        public SampledEMFactory setScaling(double scaling) {
            this.scaling = scaling;
            return this;
        }
        
        /**
         * 
         * @return 
         */
        public SampledEM getInstance(){
            return new SampledEM(this.numberOfThreads, this.batchSize,
                    this.scaling);
        }
    }
}
