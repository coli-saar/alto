/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighters.AutomatonWeighted;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author teichmann
 */
public class AdaptiveSampler {
    /**
     * 
     */
    private SamplingStatistics keptStats = null;
    
    /**
     *
     */
    private final Proposal prop;

    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     *
     * @param seed
     */
    public AdaptiveSampler(long seed) {
        this.rg = new Well44497b(seed);
        
        this.prop = new Proposal(this.rg.nextLong());
    }

    /**
     *
     */
    public AdaptiveSampler() {
        this(new Date().getTime());
    }

    /**
     * 
     * @return 
     */
    public SamplingStatistics getKeptStats() {
        return keptStats;
    }

    /**
     * 
     * @param keptStats 
     */
    public void setKeptStats(SamplingStatistics keptStats) {
        this.keptStats = keptStats;
    }
    
    /**
     * 
     * @param rounds
     * @param populationSize
     * @param rw
     * @param deterministic
     * @return 
     */
    public List<TreeSample<Rule>> adaSample(int rounds, int populationSize, RuleWeighting rw,
                                           boolean deterministic) {
        List<TreeSample<Rule>> result = new ArrayList<>();
        rw.reset();
        ProposalSumComputer psc = new ProposalSumComputer();
        
        for(int i=0;i<rounds;++i) {
            TreeSample<Rule> sample = this.prop.getTreeSample(rw, populationSize);
            
            
            for(int j=0;j<populationSize;++j) {
                sample.setLogTargetWeight(j, rw.getLogTargetProbability(sample.getSample(j)));
            }
            
            if(!deterministic) {
                psc.reset();
                
                for(int j=0;j<populationSize;++j) {
                    sample.setLogSumWeight(j, psc.computeInside(sample.getSample(j), rw));
                }
            }
            
            if(this.keptStats != null) {
                this.keptStats.addUnNormalizedRound(i,sample,rw);
            }
            
            rw.adapt(sample,deterministic);
            
            if(this.keptStats != null) {
                this.keptStats.trackAfterAdaption(rounds, sample, rw);
            }
            
            sample.expoNormalize(deterministic);
            result.add(sample);
        }
        
        return result;
    }
    
    /**
     * 
     */
    public static class Configuration {
        /**
         * 
         */
        private boolean deterministic = true;
        
        /**
         * 
         */
        private int rounds = 50;
        
        /**
         * 
         */
        private int populationSize = 2000;
        
        /**
         * 
         */
        private final Function<TreeAutomaton,RuleWeighting> rwSource;
        
        /**
         * 
         */
        private LongSupplier seeds = () -> new Date().getTime();

        /**
         * 
         * @param rwSource 
         */
        public Configuration(Function<TreeAutomaton, RuleWeighting> rwSource) {
            this.rwSource = rwSource;
        }
        
        /**
         * 
         * @param deterministic 
         */
        public void setDeterministic(boolean deterministic) {
            this.deterministic = deterministic;
        }

        /**
         * 
         * @param rounds 
         */
        public void setRounds(int rounds) {
            this.rounds = rounds;
        }

        /**
         * 
         * @param populationSize 
         */
        public void setPopulationSize(int populationSize) {
            this.populationSize = populationSize;
        }

        /**
         * 
         * @param seeds 
         */
        public void setSeeds(LongSupplier seeds) {
            this.seeds = seeds;
        }
        
        /**
         * 
         * @param ta
         * @return 
         */
        public List<TreeSample<Rule>> run(TreeAutomaton ta) {
            RuleWeighting rw = this.rwSource.apply(ta);
            AdaptiveSampler adas = new AdaptiveSampler(this.seeds.getAsLong());
            
            return adas.adaSample(rounds, populationSize, rw, deterministic);
        }
    }
}
