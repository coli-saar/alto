/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.find_rules.sampling.InterpretingModel;
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
    private final double smooth;

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
     */
    private final double emptynessConstraint;
    
    /**
     * 
     */
    private final double delexicalizationConstraint;

    public SampledEM(int useThreads, double smooth, int batchSize, double scaling, double emptynessConstraint, double delexicalizationConstraint) {
        this.useThreads = useThreads;
        this.smooth = smooth;
        this.batchSize = batchSize;
        this.scaling = scaling;
        this.emptynessConstraint = emptynessConstraint;
        this.delexicalizationConstraint = delexicalizationConstraint;
    }

    
    
    
    /**
     * 
     * 
     * @param cc
     * @param learningRounds
     * @param instances
     * @return
     * @throws ParserException
     * @throws ExecutionException
     * @throws InterruptedException 
     */
    public List<List<Tree<Rule>>> makeGrammar(CreateCorpus cc, int learningRounds,
            List<LearningInstance> instances) throws ParserException, ExecutionException, InterruptedException{
        
        VariableIndication vi = new VariableIndicationByLookUp(cc.getMainManager());
        
        List<List<Tree<Rule>>> corpus = runTraining(cc, vi, learningRounds, instances);
        
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
    private List<List<Tree<Rule>>> runTraining(CreateCorpus cc, VariableIndication vi,
            int learningRounds, List<LearningInstance> jobs) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(this.useThreads);
        
        List<LearningInstance> intermediate = new ObjectArrayList<>();
        
        Model mod = new InterpretingModel(cc.getMainManager(), this.smooth, this.emptynessConstraint,
                                                                        this.delexicalizationConstraint);
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
    public class SampledEMFactory{
        /**
         * 
         */
        private int numberOfThreads;
        
        /**
         * 
         */
        private double smooth;
        
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
        private double emptynessConstraint;
        
        /**
         * 
         */
        private double delexicalizationConstraint;
        
        /**
         * 
         */
        public SampledEMFactory(){
            numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            smooth = 10.0;
            batchSize = numberOfThreads*10;
            this.scaling = 1.0;
            this.emptynessConstraint = Math.log(1E-5);
            this.delexicalizationConstraint = Math.log(1E-5);
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
         * @param smooth
         * @return 
         */
        public SampledEMFactory setSmooth(double smooth) {
            this.smooth = smooth;
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
         * @param emptynessConstraint
         * @return 
         */
        public SampledEMFactory setEmptynessConstraint(double emptynessConstraint) {
            this.emptynessConstraint = emptynessConstraint;
            return this;
        }

        /**
         * 
         * @param delexicalizationConstraint 
         * @return  
         */
        public SampledEMFactory setDelexicalizationConstraint(double delexicalizationConstraint) {
            this.delexicalizationConstraint = delexicalizationConstraint;
            return this;
        }
        
        /**
         * 
         * @return 
         */
        public SampledEM getInstance(){
            return new SampledEM(this.numberOfThreads, this.smooth, this.batchSize,
                    this.scaling, this.emptynessConstraint, this.delexicalizationConstraint);
        }
    }
}
