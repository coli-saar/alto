/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 *
 * @author teichmann
 */
public class AdaptiveSampler {

    /**
     * 
     */
    private final Proposal prop;

    /**
     *
     * @param seed
     */
    public AdaptiveSampler(long seed) {
        this.prop = new Proposal(seed);
    }

    /**
     *
     */
    public AdaptiveSampler() {
        this(new Date().getTime());
    }

    /**
     * 
     * @param rounds
     * @param populationSize
     * @param rw
     * @param deterministic
     * @param reset
     * @return 
     */
    public List<TreeSample<Rule>> adaSample(int rounds, int populationSize, RuleWeighting rw,
            boolean deterministic, boolean reset) {
        List<TreeSample<Rule>> result = new ArrayList<>();

        ProposalSumComputer psc = new ProposalSumComputer();
        
        if(reset) {
            rw.reset();
        }

        for (int i = 0; i < rounds; ++i) {
            TreeSample<Rule> sample = prop.getTreeSample(rw, populationSize);

            for (int j = 0; j < populationSize; ++j) {
                sample.setLogTargetWeight(j, rw.getLogTargetProbability(sample.getSample(j)));
            }

            if (!deterministic) {
                psc.reset();

                for (int j = 0; j < populationSize; ++j) {
                    sample.setLogSumWeight(j, psc.computeInside(sample.getSample(j), rw));
                }
            }

            rw.adapt(sample, deterministic);

            result.add(sample);
        }

        return result;
    }

    
    public TreeSample<Rule> adaSampleMinimal(int rounds, int populationSize, RuleWeighting rw,
            boolean deterministic, boolean reset) {
        ProposalSumComputer psc = new ProposalSumComputer();
        
        if(reset) {
            rw.reset();
        }

        TreeSample<Rule> sample = null;
        for (int i = 0; i < rounds; ++i) {
            sample = prop.getTreeSample(rw, populationSize);

            for (int j = 0; j < populationSize; ++j) {
                sample.setLogTargetWeight(j, rw.getLogTargetProbability(sample.getSample(j)));
            }

            if (!deterministic) {
                psc.reset();

                for (int j = 0; j < populationSize; ++j) {
                    sample.setLogSumWeight(j, psc.computeInside(sample.getSample(j), rw));
                }
            }

            rw.adapt(sample, deterministic);
        }

        return sample;
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
        private final Function<TreeAutomaton, RuleWeighting> rwSource;
        
        /**
         * 
         */
        private boolean reset = false;

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
         * ,
         * @param reset 
         */
        public void setReset(boolean reset) {
            this.reset = reset;
        }
        
        /**
         *
         * @param ta
         * @return
         */
        public List<TreeSample<Rule>> run(TreeAutomaton ta) {
            RuleWeighting rw = this.rwSource.apply(ta);
            AdaptiveSampler adas = new AdaptiveSampler(this.seeds.getAsLong());

            return adas.adaSample(rounds, populationSize, rw, deterministic, reset);
        }
    }
}
