/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sample;

import com.google.common.base.Function;
import de.up.ling.irtg.align.find_rules.StateWeighter;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.ArraySampler;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.apache.commons.math3.random.Well44497a;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph_teichmann
 */
public class Sampler {

    /**
     * 
     */
    private final StateWeighter sw;
    
    /**
     * 
     */
    private final List<Tree<Rule>> samples = new ObjectArrayList<>();
    
    /**
     * 
     */
    private final DoubleList weights = new DoubleArrayList();
    
    /**
     * 
     */
    private final List<Tree<Rule>> result = new ObjectArrayList<>();

    /**
     * 
     */
    private final ArraySampler samp;
    
    /**
     * 
     * @param sw 
     */
    public Sampler(StateWeighter sw){
        this.sw = sw;
        samp = new ArraySampler(new Well44497b());
    }
    
    /**
     * 
     * @param sw 
     */
    public Sampler(StateWeighter sw, long seed) {
        this.sw = sw;
        samp = new ArraySampler(new Well44497a(seed));
    }
    
    /**
     * 
     * @param t1
     * @param mc
     * @return 
     */
    public List<Tree<Rule>> sample(TreeAutomaton t1, ModelContainer mc){
        for(int i=0;i<mc.samplingRounds;++i){
            if(i != 0){
                adapt(i, mc);
            }
            
            this.samples.clear();
            this.weights.clear();
            this.sw.setBasis(t1);
            
            for(int k=0;k<mc.samples;++k){
                Tree<Rule> t = this.sw.nextSample();
                double weight = -this.sw.logWeightOfLastSample();
                weight += Math.log(mc.sharedModel.getWeightRaw(t.map(mc.mapToSharedLabel)));
                
                this.samples.add(t);
                this.weights.add(weight);
            }
        }
        
        this.samp.turnIntoCWF(weights);
        this.result.clear();
        
        for(int i=0;i<mc.resultSize;++i){
            int pos = this.samp.produceSample(weights);
            
            this.result.add(this.samples.get(i));
        }
        
        return result;
    }

    /**
     * 
     * @param i
     * @param mc 
     */
    private void adapt(int i, ModelContainer mc) {
        this.samp.turnIntoCWF(weights);
            
        double weight = 1.0 / mc.adaptors;
        for(int j=0;j<mc.adaptors;++j){
            int pos = this.samp.produceSample(weights);
                
            this.sw.update(this.samples.get(pos), weight);
        }
    }
    
    
    /**
     * 
     */
    public static class ModelContainer{
        /**
         * The automaton that gives the target weight for the trees we
         * are sampling.
         */
        public TreeAutomaton sharedModel;
        
        /**
         * Turns the rule trees that we draw into trees over labels.
         */
        public Function<Rule,Integer> mapToSharedLabel;
        
        /**
         * 
         */
        public int samplingRounds;
        
        /**
         * 
         */
        public int adaptors;
        
        /**
         * 
         */
        public int samples;
        
        /**
         * 
         */
        public int resultSize;
    }
}