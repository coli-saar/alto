/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import com.google.common.base.Function;
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
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
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
    private final TreeAutomaton.BottomUpStateVisitor visit = new TreeAutomaton.BottomUpStateVisitor() {
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            double sum = 0.0;
            
            for(Rule r : rulesTopDown){
               double weight = makeRuleWeight(r);
               r.setWeight(weight);
               
               sum += weight;
            }
            
            
            double w = 0.0;
            
            for(Rule r : rulesTopDown){
                double mult = r.getWeight() / sum;
                
                double add = mult;
                for(int child : r.getChildren()){
                    add *= insides.get(child);
                }
                
                w += add;
                r.setWeight(mult);
            }
            
            insides.put(state, w);
            for(Rule r : rulesTopDown){
                r.setWeight(r.getWeight()/w);
            }
        }
    };
    
    /**
     * 
     */
    private double insideSum = 0.0;
    
    /**
     * 
     */
    private final Int2DoubleOpenHashMap counts = new Int2DoubleOpenHashMap();
    
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
    private final Int2DoubleMap insides = new Int2DoubleOpenHashMap();

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
     * @param smooth
     * @param benign 
     */
    public SampleBenign(double smooth, TreeAutomaton benign) {
        this.smooth = smooth;
        this.benign = benign;
        this.arrSamp = new ArraySampler(new Well44497a());
        this.rg = new Well44497a();
    }
    
    /**
     * 
     * @param smooth
     * @param benign 
     * @param seed 
     */
    public SampleBenign(double smooth, TreeAutomaton benign, long seed) {
        this.smooth = smooth;
        this.benign = benign;
        Well44497a w = new Well44497a(seed);
        this.arrSamp = new ArraySampler(w);
        this.rg = new Well44497a(w.nextLong());
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
     */
    public void clear(){
        this.counts.clear();
    }
    
    /**
     * 
     */
    private void makeInsides(){
        this.insides.clear();
        this.benign.foreachStateInBottomUpOrder(this.visit);
        
        IntIterator iit = this.benign.getFinalStates().iterator();
        
        insideSum = 0.0;
        while(iit.hasNext()){
            this.insideSum += this.insides.get(iit.nextInt());
        }
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
    public double getSmoothedStateCount(int state){
        return this.smooth+this.getStateCount(state);
    }
    
    /**
     * 
     * @param state
     * @return 
     */
    public double getStateCount(int state){
       return this.counts.get(state);
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
        
        for(int round=0;round<config.rounds;++round){
            this.makeInsides();
            sample.clear();
            weights.clear();
            resample.clear();
            
            int numberOfSamples = config.sampleSize.apply(round+1);
            for(int samp=0;samp<numberOfSamples;++samp){
                addSample(sample,weights);
            }
            
            addTargetWeight(sample,weights,config);
            
            this.arrSamp.turnIntoCWF(weights);
            for(int samp=0;samp<numberOfSamples;++samp){
                resample.add(sample.get(this.arrSamp.produceSample(weights)));
            }
            
            this.adapt(resample);
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
        
        double d = this.rg.nextDouble();
        
        int state = -1;
        IntIterator iit = this.benign.getFinalStates().iterator();
        while(iit.hasNext()){
            state = iit.nextInt();
            double w = this.insides.get(state)/this.insideSum;
            
            d -= w;
            if(d <= 0.0){
                md.add(Math.log(w));
                break;
            }
        }
        
        Tree<Rule> samp = sample(md,state);
        
        sample.add(samp);
        weights.add(md.getValue());
    }

    /**
     * 
     * @param sample
     * @param weights
     * @param config 
     */
    private void addTargetWeight(List<Tree<Rule>> sample, DoubleList weights, Configuration config) {
        for(int i=0;i<sample.size();++i){
            Tree<Integer> t = sample.get(i).map(config.label2TargetLabel);
            double w = Math.log(config.target.getWeightRaw(t));
            
            weights.set(i, weights.get(i)+w);
        }
    }

    /**
     * 
     * @param resample 
     */
    private void adapt(List<Tree<Rule>> resample) {
        for(int i=0;i<resample.size();++i){
            Tree<Rule> t = resample.get(i);
            this.adapt(t);
        }
    }

    /**
     * 
     * @param t 
     */
    private void adapt(Tree<Rule> t) {
        this.counts.addTo(t.getLabel().getParent(), 1.0);
        for(int i=0;i<t.getChildren().size();++i){
            adapt(t.getChildren().get(i));
        }
    }
    
    
    /**
     * 
     * @param md
     * @param state
     * @return 
     */
    private Tree<Rule> sample(MutableDouble md, int state) {
        Iterable<Rule> r = this.benign.getRulesTopDown(state);
        
        double d = this.rg.nextDouble();
        Rule choice = null;
        for(Rule j : r){
           d -= j.getWeight();
           if(d <= 0.0){
               choice = j;
               md.add(Math.log(j.getWeight()));
           }
        }
        
        List<Tree<Rule>> l = new ArrayList<>();
        for(int child :  choice.getChildren()){
            l.add(this.sample(md, child));
        }
        
        return Tree.create(choice, l);
    }
    
    /**
     * 
     */
    public class Configuration{
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
        public TreeAutomaton target;
        
        /**
         * 
         */
        public Function label2TargetLabel;
    }
}