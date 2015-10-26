/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.align.find_rules.sampling.Model;
import de.up.ling.irtg.align.find_rules.sampling.RuleCountBenign;
import de.up.ling.irtg.align.find_rules.sampling.SampleBenign;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * @author christoph_teichmann
 */
public class LearningInstance implements Callable<List<Tree<Rule>>> {

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
            System.out.println(cme);
            throw cme;
        }
    }

    public class LearningInstanceFactory {
        
        /**
         * 
         */
        private double adaptionSmooth;
        
        /**
         * 
         */
        private int samplerAdaptionRounds;

        /**
         * 
         * @param adaptionSmooth
         * @return 
         */
        public LearningInstanceFactory setAdaptionSmooth(double adaptionSmooth) {
            this.adaptionSmooth = adaptionSmooth;
            return this;
        }

        /**
         * 
         * @param samplerAdaptionRounds
         * @return 
         */
        public LearningInstanceFactory setSamplerAdaptionRounds(int samplerAdaptionRounds) {
            this.samplerAdaptionRounds = samplerAdaptionRounds;
            return this;
        }        
        
        /**
         * 
         * @param sampleSizes 
         */
        public LearningInstanceFactory(IntIntFunction sampleSizes) {
            samplerAdaptionRounds = 10;
            adaptionSmooth = 1.0;
        }

        /**
         *
         * @param language
         * @param seed
         * @param sampleSizes
         * @return
         */
        public LearningInstance makeLearningInstance(TreeAutomaton language,long seed,
                IntIntFunction sampleSizes) {
            SampleBenign sb = new RuleCountBenign(adaptionSmooth, seed, language);
            SampleBenign.Configuration conf = new SampleBenign.Configuration(null);

            conf.setSampleSize(sampleSizes).setRounds(samplerAdaptionRounds);

            LearningInstance jo = new LearningInstance(sb, conf);
            return jo;
        }

        /**
         *
         * @param numberOfSamplingResults
         * @param numberOfSamplesIntermediate
         * @param data
         * @param seed
         * @return
         */
        public List<LearningInstance> makeInstances(int numberOfSamplingResults,
                int numberOfSamplesIntermediate, List<TreeAutomaton> data, long seed) {
            List<LearningInstance> jobs = new ObjectArrayList<>();
            IntIntFunction sampleSize = (int value) -> {
                if (value == 0) {
                    return numberOfSamplingResults;
                } else {
                    return numberOfSamplesIntermediate;
                }
            };

            for (int i = 0; i < data.size(); ++i) {
                LearningInstance jo = this.makeLearningInstance(data.get(i), seed,
                        sampleSize);
                jobs.add(jo);
            }
            return jobs;
        }
        
        /**
         * 
         * @param numberOfSamplingResults
         * @param numberOfSamplesIntermediate
         * @param data
         * @return 
         */
        public List<LearningInstance> makeInstances(int numberOfSamplingResults,
                int numberOfSamplesIntermediate, List<TreeAutomaton> data){
            return makeInstances(numberOfSamplingResults, numberOfSamplesIntermediate,
                    data, new Date().getTime());
        }
    }
}
