/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * This class implements importance sampling from a tree automaton according to a
 * user specified target distribution that does not have to be expressible by a tree
 * automaton.
 * 
 * The importance sampling is adaptive and attempts to optimize the proposal
 * distribution.
 * 
 * @author teichmann
 */
public class AdaptiveImportanceSampler {
    /**
     * Used to generate samples.
     */
    private final Proposal prop;

    /**
     * Create a new instance that will use the given seed to initialize the
     * random number generation.
     * 
     * @param seed
     */
    public AdaptiveImportanceSampler(long seed) {
        this.prop = new Proposal(seed);
    }

    /**
     * Create a new instance that will use the current time to initialize the
     * random number generation.
     * 
     */
    public AdaptiveImportanceSampler() {
        this(new Date().getTime());
    }

    /**
     * Runs the sampler for the given number of rounds with the given population
     * size and adapting the given rule weights.
     * 
     * 
     * @param rounds how many rounds of adaption we run
     * @param populationSize the population size used during sampling.
     * @param rw the rule weighting that gives the proposal automaton and the target weights
     * @param deterministic whether the underlying tree automaton can be assumed to be deterministic
     * @param reset whether we reset the rule weighting before we start sampling
     * @return A list of tree samples - one for each adaption round - the samples can be normalized but generally
     * have not when the result has been returned.
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

    /**
     * Runs the sampler for the given number of rounds with the given population
     * size and adapting the given rule weights.
     * 
     * 
     * @param rounds how many rounds of adaption we run
     * @param populationSize the population size used during sampling.
     * @param rw the rule weighting that gives the proposal automaton and the target weights
     * @param deterministic whether the underlying tree automaton can be assumed to be deterministic
     * @param reset whether we reset the rule weighting before we start sampling
     * @return The final tree sample  the sample can be normalized but generally
     * has not when the result has been returned.
     */
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
     * A configuration that contains all the relevant information needed to generate
     * a number of samples from a group of tree automata.
     */
    public static class Configuration {

        /**
         * Is the underlying automaton from which we are sampling unambiguous?
         */
        private boolean deterministic = true;

        /**
         * How many rounds of sampling will we implement?
         */
        private int rounds = 50;

        /**
         * How many samples will be in the population?
         */
        private int populationSize = 2000;

        /**
         * The rule weightings that will be used for each input automaton.
         */
        private final Function<TreeAutomaton, RuleWeighting> rwSource;

        /**
         * Returns a number of seeds for initializing the samplers.
         */
        private LongSupplier seeds = () -> new Date().getTime();

        /**
         * Creates a new instance with default parameters and based on the given
         * RuleWeightings
         * 
         * @param rwSource
         */
        public Configuration(Function<TreeAutomaton, RuleWeighting> rwSource) {
            this.rwSource = rwSource;
        }

        /**
         * Set to true if the underlying automata may be ambiguous.
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
         * Used to set the population size used in sampling.
         * 
         * @param populationSize
         */
        public void setPopulationSize(int populationSize) {
            this.populationSize = populationSize;
        }

        /**
         * Used to configure the seeds used to initialize the sampler.
         * 
         * @param seeds
         */
        public void setSeeds(LongSupplier seeds) {
            this.seeds = seeds;
        }
        
        /**
         * Runs an importance sampler once for the given configuration.
         * 
         * @param ta
         * @return
         */
        public List<TreeSample<Rule>> run(TreeAutomaton ta) {
            RuleWeighting rw = this.rwSource.apply(ta);
            AdaptiveImportanceSampler adas = new AdaptiveImportanceSampler(this.seeds.getAsLong());

            return adas.adaSample(rounds, populationSize, rw, deterministic, true);
        }
    }
}
