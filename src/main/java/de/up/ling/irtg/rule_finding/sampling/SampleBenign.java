/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.util.ArraySampler;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

/**
 * This class uses adaptive iterated importance sampling in order to draw a
 * sample of rule trees from a given automaton.
 * 
 * The automaton MUST be well behaved: it must have at least one final state
 * and from any state reachable top down from the final states, there must be
 * a complete derivation (i.e. there are no states that derive no trees).
 * It is also assumed that the automaton has no cycles. The sampler actually
 * draws multiple samples and attempts to improve the proposal distribution
 * from each intermediate sample.
 * 
 * @author christoph_teichmann
 */
public abstract class SampleBenign {
    /**
     * 
     */
    private boolean resetEverySample = true;

    /**
     * 
     * @return 
     */
    public boolean isResetEverySample() {
        return resetEverySample;
    }
    
    /**
     * The initial smoothing for the rules that can be used for expanding a
     * state.
     */
    private final double smooth;
    
    /**
     * The automaton being sampled.
     */
    private InterpretedTreeAutomaton benign;
    
    /**
     * The sums of weights for each state, these are computed again for every
     * round of sampling, as they may change during round, basically this is
     * 'just' smoothing.
     */
    private final Int2DoubleMap stateNormalizers = new Int2DoubleOpenHashMap();
    
    /**
     * 
     */
    private final Int2DoubleOpenHashMap finalStateCounts = new Int2DoubleOpenHashMap();

    /**
     * Counts the number of times a rule was seen during the adaption rules.
     * 
     * Note that these counts may be fractional. When we have n effective samples
     * and an observation gets a portion of d percent of that sample, then
     * the rule counts for that observation are increased by d*n.
     */
    private final Object2DoubleOpenHashMap<Rule> ruleCounts = new Object2DoubleOpenHashMap<>();
    
    /**
     * Cache for the rules used to (substantially) speed up the sampling process.
     */
    private final Int2ObjectMap<List<Rule>> options = new Int2ObjectOpenHashMap<>();
    
    /**
     * Used in order to turn log weights into a CWF and for resampling.
     */
    private final ArraySampler arrSamp;
    
    /**
     * A random number generator used during sampling.
     */
    private final RandomGenerator rg;
    
    /**
     * The sum of the weights for final states, which is cached within a single
     * sampling round.
     */
    private double finalStateSum = -1.0;
    
    /**
     * Create a new instance with a random seed based on the time and with the
     * given smoothing for the initial rule weights.
     * 
     * Smoothing should not be set at too low a value, as it will make the
     * learner focus too much on certain examples.
     * 
     * @param smooth
     * @param benign
     */
    public SampleBenign(double smooth, InterpretedTreeAutomaton benign) {
        this.smooth = smooth;
        this.arrSamp = new ArraySampler(new Well44497a());
        this.rg = new Well44497a();
        this.stateNormalizers.defaultReturnValue(Double.NEGATIVE_INFINITY);
        this.benign = benign;
    }
    
    /**
     * Creates a new instance with the given smoothing for initial rule weights
     * and which is based on the given seed for the random number generation
     * (intended to improve reproducibility).
     * 
     * Smoothing should not be set at too low a value, as it will make the
     * learner focus too much on certain examples.
     * 
     * @param smooth 
     * @param seed 
     * @param benign 
     */
    public SampleBenign(double smooth, long seed, InterpretedTreeAutomaton benign) {
        this.smooth = smooth;
        Well44497a w = new Well44497a(seed);
        this.arrSamp = new ArraySampler(w);
        this.rg = new Well44497a(w.nextLong());
        this.stateNormalizers.defaultReturnValue(Double.NEGATIVE_INFINITY);
        this.benign = benign;
    }
    
    /**
     * Changes the given automaton and removes any adaption.
     * 
     * When the automaton is changed, then all cached information is changed.
     * It is assumed that once an automaton has been set its rules will not
     * change. Otherwise there would be a problem with caching. 
     * 
     * @param to 
     */
    public void setAutomaton(InterpretedTreeAutomaton to){
        this.benign = to;
        this.options.clear();
        resetAdaption();
    }

    /**
     * Flushes all the given adaption done so far.
     */
    public void resetAdaption() {
        this.finalStateCounts.clear();
        this.ruleCounts.clear();
    }
    
    /**
     * If set to true the adaption will be flushed for every new sampling
     * generation process.
     * 
     * Default value is true.
     * 
     * @param resetEverySample 
     */
    public void setResetEverySample(boolean resetEverySample) {
        this.resetEverySample = resetEverySample;
    }
    
    
    /**
     * Abstract method that returns a weight for a rule.
     * 
     * These weights will be summed up and then normalized
     * to give a probability.
     * 
     * @param r
     * @return 
     */
    protected abstract double makeRuleWeight(Rule r);
    
    /**
     * Returns the fractional count of the given state over all adaption rounds
     * with the smoothing value added.
     * 
     * @param state
     * @return 
     */
    public double getSmoothedFinalStateCount(int state){
        return this.smooth+this.getFinalStateCount(state);
    }
    
    /**
     * Returns the raw fractional count of the given state without smoothing.
     * 
     * @param state
     * @return 
     */
    public double getFinalStateCount(int state){
       return this.finalStateCounts.get(state);
    }
    
    /**
     * Returns the fractional count of how often the rule was seen during
     * adaption, without any smoothing.
     * 
     * @param rule
     * @return 
     */
    public double getRuleCount(Rule rule){
        return this.ruleCounts.getDouble(rule);
    }
    
    /**
     * Returns the fractional count of how often the rule was seen during adaption
     * with smoothing added.
     * 
     * @param rule
     * @return 
     */
    public double getSmoothedRuleCount(Rule rule){       
        return this.getRuleCount(rule)+this.smooth;
    }
    
    /**
     * Starts a new round of sampling that will return an (unweighted) sample
     * as a result - the automaton that is being sampled is the one from the
     * last call to setAutomaton.
     * 
     * The counts of how often all rules and states have been seen during adaption
     * will be reset, but rules are kept in a cache between calls, so changes
     * in the automaton after the first round of getSample will be
     * ignored, unless we hit a state that has not been cached.
     * 
     * Note that there will be a null pointer exception, if setAutomaton has not
     * been called before (or was called with a null argument).
     * 
     * @param config
     * @return 
     */
    public List<Tree<Rule>> getSample(Configuration config){
        if(this.resetEverySample){
            this.resetAdaption();
        }
        
        if(this.benign == null){
            throw new NullPointerException("The automaton to be sampled must"
                    + "be defined with setAutomaton before sampling starts");
        }
        
        // used to store the intermediate samples and their weights
        List<Tree<Rule>> sample = new ObjectArrayList<>();
        DoubleList weights = new DoubleArrayList();
        
        // here we do a number of adaption steps and take the actual sample
        // from the last round
        for(int round=0;round<config.getRounds();++round){
            // we reset the adapted sums for each state
            this.finalStateSum = Double.NEGATIVE_INFINITY;
            this.stateNormalizers.clear();
            
            // we clear the sample (in consecutive rounds, the adaption will
            // already have taken place at this point)
            sample.clear();
            weights.clear();
            
            // then find out how many samples we are supposed to generate
            int numberOfSamples = config.getSampleSize(round+1);
            
            // generate them, with the inverse proposal probabilist added to
            // weights - as logs
            for(int samp=0;samp<numberOfSamples;++samp){
                addSample(sample,weights);
            }
            
            // add the actual target weights - as logs
            addTargetWeight(sample,weights,config.getTarget());
            
            // normalize the weights and turn them into a CDF
            this.arrSamp.turnIntoCWF(weights);
            double effectiveSampleSize = makeESSFromCWF(weights);
            
            //here we adapt the proposal probabilities according to how often we
            // have seen certain proposals and how large our effective sample is
            
            for(int i=0;i<weights.size();++i){
                this.adapt(sample.get(i),
                        effectiveSampleSize*(weights.get(i) - (i > 0 ? weights.get(i-1) : 0.0)));
            }
        }
        
        // once we are finished we re-sample the last proposal in order to
        // get an unweighted sample
        List<Tree<Rule>> resample = new ObjectArrayList<>();
        
        // the 0 indicates that we are asking for the number of final samples
        int numberOfSamples = config.getSampleSize(0);
        for(int samp=0;samp<numberOfSamples;++samp){
            resample.add(sample.get(this.arrSamp.produceSample(weights)));
        }
        
        return resample;
    }

    /**
     * This method draws one sample from the proposal distribution and adds
     * it and it's unnormalized log proposal probability to the given lists.
     * 
     * @param sample
     * @param weights 
     */
    private void addSample(List<Tree<Rule>> sample, DoubleList weights) {
        // this will be used to store the weight in recursive steps
        MutableDouble md = new MutableDouble(0.0);
        
        // if the final state sum is less than 0.0 then that means that we have
        // to recompute it
        if(this.finalStateSum < 0.0){
            this.finalStateSum = 0.0;
            
            IntIterator iit = this.benign.getAutomaton().getFinalStates().iterator();
            while(iit.hasNext()){
                this.finalStateSum += this.getSmoothedFinalStateCount(iit.nextInt());
            }
        }
        
        // we draw some fraction of the sum
        double d = this.rg.nextDouble()*this.finalStateSum;
        
        // then we go over the states in some random order and select the first
        // one for which the cumulative weight is larger then the fraction we
        // proposed
        int state = -1;
        IntIterator iit = this.benign.getAutomaton().getFinalStates().iterator();
        while(iit.hasNext()){
            state = iit.nextInt();
            double w = this.getSmoothedFinalStateCount(state);
            
            d -= w;
            if(d <= 0.0){
                // when we have made a decision, we need to add the corresponding
                // weight in my experience log of division is faster than log-log
                // when the subtracted numbers are in the same order of magnitude
                md.add(Math.log(w / this.finalStateSum));
                break;
            }
        }
        
        // now we can sample a tree derived from the chosen state
        Tree<Rule> samp = sample(md,state);
        
        // and then add the information (note that here we do the inversion of
        // the weight)
        sample.add(samp);
        weights.add(-md.getValue());
    }

    /**
     * Adds the weight from the actual target distribution for the given sample
     * trees.
     * 
     * @param sample
     * @param weights
     * @param mod 
     */
    protected void addTargetWeight(List<Tree<Rule>> sample, DoubleList weights, Model mod) {
        // we look up the weight for each tree
        for(int i=0;i<sample.size();++i){
            double d = lookUpWeight(mod, sample.get(i));
            
            // and add it to the proposal weights
            weights.set(i, weights.get(i)+d);
        }
    }

    /**
     * Retrieves the weight for a single tree from the target function.
     * 
     * @param mod
     * @param t
     * @return 
     */
    protected double lookUpWeight(Model mod, Tree<Rule> t) {
        return mod.getLogWeight(t,this.benign);
    }

    /**
     * This method changes the proposal weight for each rule in the given
     * three by adding the given amount; it also changes the weight of
     * the state from which the tree was derived.
     * 
     * @param t 
     */
    private void adapt(Tree<Rule> t, double amount) {
        this.finalStateCounts.addTo(t.getLabel().getParent(), amount);
        
        this.adaptRules(t,amount);
    }
    
    
    /**
     * Draws a rule tree from a given state.
     * 
     * The method calls itself recursively to expand states until
     * all rules are terminal. It keeps track of the log proposal weight
     * in the mutable double.
     * 
     * @param md
     * @param state
     * @return 
     */
    private Tree<Rule> sample(MutableDouble md, int state) {
        List<Rule> r = this.options.get(state);
        
        // if the cache of rules for the given state is empty, create one
        // it's a bit ugly, but it makes a BIG difference in speed.
        if(r == null){
            r = new ObjectArrayList<>();
            Iterable<Rule> it = this.benign.getAutomaton().getRulesTopDown(state);
            for(Rule k : it){
                r.add(k);
            }
            
            this.options.put(state, r);
        }
        
        // if the normalizer has not been set, then we need to re-compute it
        double sum = this.stateNormalizers.get(state);
        if(sum <= 0.0){
            sum = 0.0;
            for(int i=0;i<r.size();++i){
                sum += this.makeRuleWeight(r.get(i));
            }
            
            this.stateNormalizers.put(state, sum);
        }
        
        // we draw a point
        double d = this.rg.nextDouble()*sum;
        Rule choice = null;
        for(int k=0;k<r.size();++k){
           Rule j = r.get(k);
           double w = this.makeRuleWeight(j);
           d -= w;
           
           // find the rule that contains the point
           if(d <= 0.0){
               choice = j;
               md.add(Math.log(w / sum));
               break;
           }
        }
        
        //and get a tree that starts with that rule
        Tree<Rule> t = Tree.create(choice);
        //choice will be null if something about the weights is not correct
        // e.g. if some of them are infinite or NaN
        for(int child :  choice.getChildren()){
            t.getChildren().add(this.sample(md, child));
        }
        
        return t;
    }

    /**
     * This does only the adaptation of the rules.
     * 
     * @param t
     * @param amount 
     */
    private void adaptRules(Tree<Rule> t, double amount) {
        this.ruleCounts.addTo(t.getLabel(), amount);
               
        for(int i=0;i<t.getChildren().size();++i){
            adaptRules(t.getChildren().get(i),amount);
        }
    }

    /**
     * This is used to compute the effective sample size from the
     * cumulative weight (probability) function.
     * 
     * @param weights
     * @return 
     */
    private double makeESSFromCWF(DoubleList weights) {
        if(weights.size() <= 0){
            return 0.0;
        }
        
        double val = weights.get(0);
        double sum = val*val;
        for(int i=1;i<weights.size();++i){
            val = weights.get(i)-weights.get(i-1);
            
            sum += val*val;
        }
        
        return 1.0 / sum;
    }
    
    /**
     * Used to configure the sampling process.
     */
    public static class Configuration{
        /**
         * The number of samples drawn in each round, the 0th round is the
         * final resampling step.
         */
        private IntIntFunction sampleSize = (int num) -> 1000;
        
        /**
         * The number of rounds used for adaption.
         */
        private int rounds = 20;
        
        /**
         * The model used for evaluation.
         */
        private Model target;

        /**
         * Create a default instance with the given model as a target weighting,
         * with 5 rounds and 100 samples
         * 
         * @param target
         */
        public Configuration(Model target){
           this.target = target; 
        }        
        
        /**
         * Create a shallow copy of this configuration (rounds are deep copied,
         * everything else uses a shallow copy).
         * @return 
         */
        public Configuration copy() {
            Configuration conf = new Configuration(this.target);
            
            conf.rounds = this.rounds;
            conf.sampleSize = this.sampleSize;
            conf.target = this.target;
            
            return conf;
        }

        /**
         * Returns the target sample size for the given round.
         * 
         * Never less than 1.
         * 
         * @param round
         * @return 
         */
        public int getSampleSize(int round) {
            return Math.max(1, sampleSize.apply(round));
        }

        /**
         * The sample size is the function that gives the number of desired
         * samples for a round (counting from 1); with round 0 indicating the
         * final sample number.
         * 
         * @param sampleSize 
         * @return  
         */
        public Configuration setSampleSize(IntIntFunction sampleSize) {
            this.sampleSize = sampleSize;
            return this;
        }

        /**
         * Returns the number of adaption rounds that will be done, never less
         * than 1.
         * 
         * @return 
         */
        public int getRounds() {
            return Math.max(1, rounds);
        }

        /**
         * Returns the number of adaption rounds that will be done.
         * 
         * @param rounds 
         * @return  
         */
        public Configuration setRounds(int rounds) {
            this.rounds = rounds;
            return this;
        }

        /**
         * Returns the model that is used as a target weighting.
         * @return 
         */
        public Model getTarget() {
            return target;
        }

        /**
         * Configures a new target for sampling.
         * 
         * @param target 
         * @return  
         */
        public Configuration setTarget(Model target) {
            this.target = target;
            return this;
        }
    }
}