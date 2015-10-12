/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
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
 *
 * @author christoph_teichmann
 */
public abstract class SampleBenign {
    
    /**
     * 
     */
    private final double smooth;
    
    /**
     * 
     */
    private TreeAutomaton benign;
    
    /**
     * 
     */
    private final Int2DoubleMap stateNormalizers = new Int2DoubleOpenHashMap();
    
    /**
     * 
     */
    private final Int2DoubleOpenHashMap finalStateCounts = new Int2DoubleOpenHashMap();

    /**
     * 
     */
    private final Object2DoubleOpenHashMap<Rule> ruleCounts = new Object2DoubleOpenHashMap<>();
    
    /**
     * 
     */
    private final Int2ObjectMap<List<Rule>> options = new Int2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final ArraySampler arrSamp;
    
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     */
    private double finalStateSum = -1.0;
    
    /**
     * 
     * @param smooth
     */
    public SampleBenign(double smooth) {
        this.smooth = smooth;
        this.arrSamp = new ArraySampler(new Well44497a());
        this.rg = new Well44497a();
        this.stateNormalizers.defaultReturnValue(Double.NEGATIVE_INFINITY);
    }
    
    /**
     * 
     * @param smooth 
     * @param seed 
     */
    public SampleBenign(double smooth, long seed) {
        this.smooth = smooth;
        Well44497a w = new Well44497a(seed);
        this.arrSamp = new ArraySampler(w);
        this.rg = new Well44497a(w.nextLong());
        this.stateNormalizers.defaultReturnValue(Double.NEGATIVE_INFINITY);
    }
    
    /**
     * 
     * @param to 
     */
    public void setAutomaton(TreeAutomaton to){
        this.benign = to;
    }
    
    /**
     * 
     * @param r
     * @return 
     */
    protected abstract double makeRuleWeight(Rule r);
    
    /**
     * 
     * @param state
     * @return 
     */
    public double getSmoothedFinalStateCount(int state){
        return this.smooth+this.getFinalStateCount(state);
    }
    
    /**
     * 
     * @param state
     * @return 
     */
    public double getFinalStateCount(int state){
       return this.finalStateCounts.get(state);
    }
    
    /**
     * 
     * @param rule
     * @return 
     */
    public double getRuleCount(Rule rule){
        return this.ruleCounts.getDouble(rule);
    }
    
    /**
     * 
     * @param rule
     * @return 
     */
    public double getSmoothedRuleCount(Rule rule){       
        return this.getRuleCount(rule)+this.smooth;
    }
    
    /**
     * 
     * @param config
     * @return 
     */
    public List<Tree<Rule>> getSample(Configuration config){
        List<Tree<Rule>> sample = new ObjectArrayList<>();
        DoubleList weights = new DoubleArrayList();
        List<Tree<Rule>> resample = new ObjectArrayList<>();
        this.options.clear();
        
        for(int round=0;round<config.rounds;++round){
            this.finalStateSum = Double.NEGATIVE_INFINITY;
            this.stateNormalizers.clear();
            
            sample.clear();
            weights.clear();
            
            int numberOfSamples = config.sampleSize.apply(round+1);
            for(int samp=0;samp<numberOfSamples;++samp){
                addSample(sample,weights);
            }
            
            addTargetWeight(sample,weights,config);
            
            this.arrSamp.turnIntoCWF(weights);
            
            for(int i=0;i<weights.size();++i){
                this.adapt(sample.get(i), weights.get(i) - (i > 0 ? weights.get(i-1) : 0.0));
            }
        }
        
        resample.clear();
        int numberOfSamples = config.sampleSize.apply(0);
        for(int samp=0;samp<numberOfSamples;++samp){
            resample.add(sample.get(this.arrSamp.produceSample(weights)));
        }
        
        return resample;
    }

    /**
     * 
     * @param sample
     * @param weights 
     */
    private void addSample(List<Tree<Rule>> sample, DoubleList weights) {
        MutableDouble md = new MutableDouble(0.0);
        
        if(this.finalStateSum < 0.0){
            this.finalStateSum = 0.0;
            
            IntIterator iit = this.benign.getFinalStates().iterator();
            while(iit.hasNext()){
                this.finalStateSum += this.getSmoothedFinalStateCount(iit.nextInt());
            }
        }
        
        double d = this.rg.nextDouble()*this.finalStateSum;
        
        int state = -1;
        IntIterator iit = this.benign.getFinalStates().iterator();
        while(iit.hasNext()){
            state = iit.nextInt();
            double w = this.getSmoothedFinalStateCount(state);
            
            d -= w;
            if(d <= 0.0){
                md.add(Math.log(w/this.finalStateSum));
                break;
            }
        }
        
        Tree<Rule> samp = sample(md,state);
        
        sample.add(samp);
        weights.add(-md.getValue());
    }

    /**
     * 
     * @param sample
     * @param weights
     * @param config 
     */
    private void addTargetWeight(List<Tree<Rule>> sample, DoubleList weights, Configuration config) {
        for(int i=0;i<sample.size();++i){
            double d = lookUpWeight(config, sample.get(i));
            
            weights.set(i, weights.get(i)+d);
        }
    }

    /**
     * 
     * @param config
     * @param t
     * @return 
     */
    private double lookUpWeight(Configuration config, Tree<Rule> t) {
        return config.target.getLogWeight(t);
        //return Math.log(config.target.getWeightRaw(t));
    }

    /**
     * 
     * @param t 
     */
    private void adapt(Tree<Rule> t, double amount) {
        this.finalStateCounts.addTo(t.getLabel().getParent(), amount);
        
        this.adaptRules(t,amount);
    }
    
    
    /**
     * 
     * @param md
     * @param state
     * @return 
     */
    private Tree<Rule> sample(MutableDouble md, int state) {
        List<Rule> r = this.options.get(state);
        
        if(r == null){
            r = new ObjectArrayList<>();
            Iterable<Rule> it = this.benign.getRulesTopDown(state);
            for(Rule k : it){
                r.add(k);
            }
            this.options.put(state, r);
        }
        
        double sum = this.stateNormalizers.get(state);
        if(sum <= 0.0){
            sum = 0.0;
            for(int i=0;i<r.size();++i){
                sum += this.makeRuleWeight(r.get(i));
            }
            
            this.stateNormalizers.put(state, sum);
        }
        
        double d = this.rg.nextDouble()*sum;
        Rule choice = null;
        for(int k=0;k<r.size();++k){
           Rule j = r.get(k);
           double w = this.makeRuleWeight(j);
           d -= w;
           if(d <= 0.0){
               choice = j;
               md.add(Math.log(w / sum));
               break;
           }
        }
        
        Tree<Rule> t = Tree.create(choice);
        for(int child :  choice.getChildren()){
            t.getChildren().add(this.sample(md, child));
        }
        
        return t;
    }

    /**
     * 
     */
    public void clear() {
        this.finalStateCounts.clear();
        this.ruleCounts.clear();
    }

    /**
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
     * 
     */
    public static class Configuration{
        /**
         * 
         */
        public IntIntFunction sampleSize;
        
        /**
         * 
         */
        public int rounds;
        
        /**
         * 
         */
        public Model target;

        /**
         * 
         * @return 
         */
        public Configuration copy() {
            Configuration conf = new Configuration();
            
            conf.rounds = this.rounds;
            conf.sampleSize = this.sampleSize;
            conf.target = this.target;
            
            return conf;
        }
    }
}