/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.RuleFinder;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.find_rules.sampling.InterpretingModel;
import de.up.ling.irtg.align.find_rules.sampling.Model;
import de.up.ling.irtg.align.find_rules.sampling.RuleCountBenign;
import de.up.ling.irtg.align.find_rules.sampling.SampleBenign;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.Callable;
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
    private double emptynessConstraint;
    
    /**
     * 
     * @param useThreads
     * @param smooth
     * @param batchSize 
     * @param scaling 
     */
    public SampledEM(int useThreads, double smooth, int batchSize,
            double scaling) {
        this.useThreads = Math.max(useThreads, 1);
        this.smooth = Math.max(smooth, 0.0);
        this.batchSize = Math.max(1, batchSize);
        this.scaling = scaling;
    }
    
    /**
     * 
     * @param useThreads
     * @param smooth
     * @param scaling 
     */
    public SampledEM(int useThreads, double smooth, double scaling){
        this(useThreads,smooth,3*useThreads, scaling);
    }

    /**
     * 
     * @param smooth 
     * @param scaling 
     */
    public SampledEM(double smooth, double scaling){
        this(Runtime.getRuntime().availableProcessors()/2,
                smooth,3 * (Runtime.getRuntime().availableProcessors()/2), scaling);
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
    public InterpretedTreeAutomaton makeGrammar(CreateCorpus cc, int learningRounds,
            List<LearningInstance> instances) throws ParserException, ExecutionException, InterruptedException{
        
        VariableIndication vi = new VariableIndicationByLookUp(cc.getMainManager());
        
        TreeAddingAutomaton currentModel = runTraining(cc, vi, learningRounds, instances);
        
        return new RuleFinder().getInterpretation(currentModel, cc.getMainManager(), cc.getAlgebra1(), cc.getAlgebra2());
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
    private TreeAddingAutomaton runTraining(CreateCorpus cc, VariableIndication vi,
            int learningRounds, List<LearningInstance> jobs) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(this.useThreads);
        
        List<LearningInstance> intermediate = new ObjectArrayList<>();
        
        Model mod = new InterpretingModel(cc.getMainManager(), this.smooth, this.emptynessConstraint);
        for (int round = 0; round < learningRounds; ++round) {
            for (int i = 0; i < jobs.size();) {
                int k = 0;

                intermediate.clear();
                for (; i + k < jobs.size() && k < this.batchSize; ++k) {
                    LearningInstance j = jobs.get(i + k);
                    j.setModel(mod);

                    intermediate.add(jobs.get(i + k));
                }
                
                i += k;

                Collection<Future<List<Tree<Rule>>>> results = es.invokeAll(intermediate);
                
                int num = 0;
                
                for (Future<List<Tree<Rule>>> result : results) {
                    List<Tree<Rule>> choices = result.get();
                    double amount = scaling / ((double) choices.size());
                    for (int h = 0; h < choices.size(); ++h) {
                        mod.add(choices.get(h), amount);
                    }
                }
            }
            
            System.out.println("finished one round of training");
        }
        
        es.shutdown();
        
        //TODO get final model or final trees ? 
        return null;
    }

    /**
     * 
     * @param numberOfSamplingResults
     * @param numberOfSamplesIntermediate
     * @param cc
     * @param data
     * @param sampleSmooth
     * @param seed
     * @param samplerAdaptionRounds
     * @return 
     */
    public List<LearningInstance> makeInstances(int numberOfSamplingResults, int numberOfSamplesIntermediate,
            CreateCorpus cc, List<TreeAutomaton> data, double sampleSmooth, 
            long seed, int samplerAdaptionRounds) {
        List<LearningInstance> jobs = new ObjectArrayList<>();
        IntIntFunction sampleSize = (int value) -> {
            if(value == 0){
                return numberOfSamplingResults;
            }else{
                return numberOfSamplesIntermediate;
            }
        };
        
        for(int i=0;i<data.size();++i){
            LearningInstance jo = makeLearningInstance(data.get(i), 
                    sampleSmooth, seed, sampleSize, samplerAdaptionRounds);
            jobs.add(jo);
        }
        return jobs;
    }

    /**
     * 
     * @param language
     * @param adaptionSmooth
     * @param seed
     * @param sampleSizes
     * @param samplerAdaptionRounds
     * @param cc
     * @return 
     */
    public LearningInstance makeLearningInstance(TreeAutomaton language, double adaptionSmooth, long seed,
            IntIntFunction sampleSizes, int samplerAdaptionRounds, CreateCorpus cc){
        return makeLearningInstance(language, adaptionSmooth, seed, sampleSizes, samplerAdaptionRounds);
    }
    
    /**
     * 
     * @param language
     * @param adaptionSmooth
     * @param seed
     * @param sampleSizes
     * @param samplerAdaptionRounds
     * @return 
     */
    public LearningInstance makeLearningInstance(TreeAutomaton language, double adaptionSmooth, long seed,
            IntIntFunction sampleSizes, int samplerAdaptionRounds) {
        /*
        SampleBenign sb = new RuleCountBenign(adaptionSmooth, seed);
        SampleBenign.Configuration conf = new SampleBenign.Configuration();
        sb.setAutomaton(language);
        
        conf.sampleSize = sampleSizes;
        conf.rounds = samplerAdaptionRounds;
        conf.target = null;
        
        LearningInstance jo = new LearningInstance(sb, conf);
        return jo;
        */
        //TODO
        return null;
    }    
    
    /**
     * 
     */
    public class LearningInstance implements Callable<List<Tree<Rule>>>{

        /**
         * 
         */
        private final SampleBenign samp;
        
        /**
         * 
         */
        private Model model;
        
        /**
         * 
         */
        private final SampleBenign.Configuration config;

        /**
         * 
         * @param samp
         * @param config 
         */
        public LearningInstance(SampleBenign samp, SampleBenign.Configuration config) {
            this.samp = samp;
            this.config = config.copy();
        }

        /**
         * 
         * @param model 
         */
        public void setModel(Model model) {
            this.model = model;
        }
        
        @Override
        public List<Tree<Rule>> call() throws Exception {
           try {
                config.setTarget(model);
                return this.samp.getSample(config);
            } catch (ConcurrentModificationException cme) {
                cme.printStackTrace();
                throw cme;
            }
        }
    }
}
