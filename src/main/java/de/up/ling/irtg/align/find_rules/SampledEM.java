/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import com.google.common.base.Function;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.RuleFinder;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.find_rules.sampling.RuleCountBenign;
import de.up.ling.irtg.align.find_rules.sampling.SampleBenign;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntDoubleFunction;
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
    private final double variableInit;
    
    /**
     * 
     */
    private final double mainInit;

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
     * @param variableInit
     * @param mainInit 
     * @param batchSize 
     * @param scaling 
     */
    public SampledEM(int useThreads, double variableInit, double mainInit, int batchSize,
            double scaling) {
        this.useThreads = Math.max(useThreads, 1);
        this.variableInit = Math.max(variableInit, 0.0);
        this.mainInit = Math.max(mainInit, 0.0);
        this.batchSize = Math.max(1, batchSize);
        this.scaling = scaling;
    }
    
    /**
     * 
     * @param useThreads
     * @param variableInit
     * @param mainInit 
     * @param scaling 
     */
    public SampledEM(int useThreads, double variableInit, double mainInit, double scaling){
        this(useThreads,variableInit,mainInit,3*useThreads, scaling);
    }

    /**
     * 
     * @param variableInit
     * @param mainInit 
     * @param scaling 
     */
    public SampledEM(double variableInit, double mainInit, double scaling){
        this(Runtime.getRuntime().availableProcessors()/2,
                variableInit,mainInit,3 * (Runtime.getRuntime().availableProcessors()/2), scaling);
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
        IntDoubleFunction idf = (int value) -> {
            if(cc.getMainManager().isVariable(value)){
                return this.variableInit;
            }else{
                return this.mainInit;
            }
        };
        
        TreeAddingAutomaton estimate = new TreeAddingAutomaton(cc.getMainManager().getSignature(), idf, vi);
        TreeAddingAutomaton currentModel = estimate;
        
        for (int round = 0; round < learningRounds; ++round) {
            for (int i = 0; i < jobs.size();) {
                int k = 0;

                intermediate.clear();
                for (; i + k < jobs.size() && k < this.batchSize; ++k) {
                    LearningInstance j = jobs.get(i + k);
                    j.setModel(currentModel);

                    intermediate.add(jobs.get(i + k));
                }
                
                i += k;

                Collection<Future<List<Tree<Integer>>>> results = es.invokeAll(intermediate);
                
                int num = 0;
                
                for (Future<List<Tree<Integer>>> result : results) {
                    List<Tree<Integer>> choices = result.get();
                    for (int h = 0; h < choices.size(); ++h) {
                        estimate.addVariableTree(choices.get(h), scaling / choices.size());
                    }
                }
            }
            
            currentModel = estimate;
            
            currentModel.normalizeStart();
            
            estimate = new TreeAddingAutomaton(cc.getMainManager().getSignature(), idf, vi);
            
            System.out.println("finished one round of training");
        }
        
        es.shutdown();
        
        return currentModel;
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
        
        
        Function<Rule,Integer> map = new DefaultVariableMapping(cc.getMainManager());
        for(int i=0;i<data.size();++i){
            LearningInstance jo = makeLearningInstance(data.get(i), 
                    sampleSmooth, seed, sampleSize, samplerAdaptionRounds, map);
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
        DefaultVariableMapping dvm = new DefaultVariableMapping(cc.getMainManager());
        
        return makeLearningInstance(language, adaptionSmooth, seed, sampleSizes, samplerAdaptionRounds, dvm);
    }
    
    /**
     * 
     * @param language
     * @param adaptionSmooth
     * @param seed
     * @param sampleSizes
     * @param samplerAdaptionRounds
     * @param variableMapping
     * @return 
     */
    public LearningInstance makeLearningInstance(TreeAutomaton language, double adaptionSmooth, long seed,
            IntIntFunction sampleSizes, int samplerAdaptionRounds, Function<Rule, Integer> variableMapping) {
        SampleBenign sb = new RuleCountBenign(adaptionSmooth, seed);
        SampleBenign.Configuration conf = new SampleBenign.Configuration();
        sb.setAutomaton(language);
        
        conf.sampleSize = sampleSizes;
        conf.rounds = samplerAdaptionRounds;
        conf.target = null;
        conf.label2TargetLabel = variableMapping;
        
        LearningInstance jo = new LearningInstance(sb, conf);
        return jo;
    }    
    
    /**
     * 
     */
    public class LearningInstance implements Callable<List<Tree<Integer>>>{

        /**
         * 
         */
        private final SampleBenign samp;
        
        /**
         * 
         */
        private TreeAutomaton model;
        
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
        public void setModel(TreeAutomaton model) {
            this.model = model;
        }
        
        @Override
        public List<Tree<Integer>> call() throws Exception {
            try {
                List<Tree<Integer>> ret = new ObjectArrayList<>();
                config.target = model;
                List<Tree<Rule>> sample = this.samp.getSample(config);

                for (int i = 0; i < sample.size(); ++i) {
                    ret.add(sample.get(i).map(config.label2TargetLabel));
                }

                return ret;
            } catch (ConcurrentModificationException cme) {
                cme.printStackTrace();
                throw cme;
            }
        }
    }
}
