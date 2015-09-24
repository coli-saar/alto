/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import com.google.common.base.Function;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.RuleFinder;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.find_rules.sampling.RuleCountBenign;
import de.up.ling.irtg.align.find_rules.sampling.SampleBenign;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntDoubleFunction;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
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
 
    
    public InterpretedTreeAutomaton makeGrammar(List<CreateCorpus.InputPackage> data1,
            List<CreateCorpus.InputPackage> data2,
            Algebra alg1, Algebra alg2, double mainSmooth, double variableSmooth,
            long seed, double sampleSmooth, int threads, int modelSamples, int interMediateSamples,
            int samplerAdaptionRounds, int learningRounds) throws ParserException, ExecutionException, InterruptedException{
        
        CreateCorpus cc = new CreateCorpus(alg1, alg2);
        List<TreeAutomaton> l = cc.makeDataSet(data1, data2);
        
        VariableIndication vi = new VariableIndicationByLookUp(cc.getMainManager());
        
        List<Job> jobs = makeJobs(modelSamples, interMediateSamples, cc, l, sampleSmooth, seed, samplerAdaptionRounds);
        
        TreeAddingAutomaton currentModel = 
                runTraining(threads, cc, variableSmooth, mainSmooth, vi, learningRounds, jobs);
        
        return new RuleFinder().getInterpretation(currentModel, cc.getMainManager(), cc.getAlgebra1(), cc.getAlgebra1());
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
    private TreeAddingAutomaton runTraining(int threads, CreateCorpus cc, double variableSmooth, double mainSmooth, VariableIndication vi, int learningRounds, List<Job> jobs) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(threads);
        List<Job> intermediate = new ObjectArrayList<>();
        IntDoubleFunction idf = (int value) -> {
            if(cc.getMainManager().isVariable(value)){
                return variableSmooth;
            }else{
                return mainSmooth;
            }
        };
        TreeAddingAutomaton estimate = new TreeAddingAutomaton(cc.getMainManager().getSignature(), idf, vi);
        TreeAddingAutomaton currentModel = estimate;
        for (int round = 0; round < learningRounds; ++round) {
            for (int i = 0; i < jobs.size();) {
                int k = 0;
                intermediate.clear();
                for (; i + k < jobs.size() && k < threads; ++k) {
                    Job j = jobs.get(i + k);
                    j.setModel(currentModel);

                    intermediate.add(jobs.get(i + k));
                }
                i += k;

                Collection<Future<List<Tree<Integer>>>> results = es.invokeAll(intermediate);
                for (Future<List<Tree<Integer>>> result : results) {
                    List<Tree<Integer>> choices = result.get();
                    for (int h = 0; h < choices.size(); ++h) {
                        estimate.addVariableTree(choices.get(i), 1.0 / choices.size());
                    }
                }

            }
            currentModel = estimate;
            estimate = new TreeAddingAutomaton(cc.getMainManager().getSignature(), idf, vi);
        }
        
        es.shutdown();
        
        return currentModel;
    }

    /**
     * 
     * @param modelSamples
     * @param interMediateSamples
     * @param cc
     * @param l
     * @param sampleSmooth
     * @param seed
     * @param samplerAdaptionRounds
     * @return 
     */
    private List<Job> makeJobs(int modelSamples, int interMediateSamples,
            CreateCorpus cc, List<TreeAutomaton> l, double sampleSmooth, long seed,
            int samplerAdaptionRounds) {
        List<Job> jobs = new ObjectArrayList<>();
        IntIntFunction sampleSize = (int value) -> {
            if(value == 0){
                return modelSamples;
            }else{
                return interMediateSamples;
            }
        };
        Function<Rule,Integer> map = new DefaultVariableMapping(cc.getMainManager());
        for(int i=0;i<l.size();++i){
            SampleBenign sb = new RuleCountBenign(sampleSmooth, seed);
            
            SampleBenign.Configuration conf = new SampleBenign.Configuration();
            conf.sampleSize = sampleSize;
            conf.rounds = samplerAdaptionRounds;
            conf.target = null;
            conf.label2TargetLabel = map;
            
            Job jo = new Job(sb, conf);
            jobs.add(jo);
        }
        return jobs;
    }
    
    /**
     * 
     */
    private class Job implements Callable<List<Tree<Integer>>>{

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
        public Job(SampleBenign samp, SampleBenign.Configuration config) {
            this.samp = samp;
            this.config = config;
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
            List<Tree<Integer>> ret = new ObjectArrayList<>();
            config.target = model;
            List<Tree<Rule>> sample = this.samp.getSample(config);
            
            for(int i=0;i<sample.size();++i){
                ret.add(sample.get(i).map(config.label2TargetLabel));
            }
            
            return ret;
        }
    }
}