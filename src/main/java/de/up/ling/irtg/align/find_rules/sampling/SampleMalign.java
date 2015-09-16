/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.automata.FromRuleTreesAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

/**
 *
 * @author christoph_teichmann
 */
public abstract class SampleMalign {
    /**
     * 
     */
    private final SampleBenign worker;
    
    /**
     * 
     */
    private TreeAutomaton malign;
    
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     */
    private final double smooth;
    
    /**
     * 
     */
    private final IntSet useLess = new IntOpenHashSet();
    
    /**
     * 
     * @param worker 
     */
    public SampleMalign(SampleBenign worker){
        this(worker,0.0001);
    }
    
    /**
     * 
     * @param worker 
     * @param smooth 
     */
    public SampleMalign(SampleBenign worker, double smooth){
        this.worker = worker;
        this.rg = new Well44497a();
        this.smooth = smooth;
    }
    
    /**
     * 
     * @param worker 
     * @param smooth 
     * @param seed 
     */
    public SampleMalign(SampleBenign worker, double smooth, long seed){
        this.rg = new Well44497a(seed);
        this.worker = worker;
        this.smooth = smooth;
    }

    /**
     * 
     * @param malign 
     */
    public void setMalign(TreeAutomaton malign) {
        this.malign = malign;
        this.worker.clear();
    }
   
    /**
     * 
     * @param config
     * @return 
     */
    public List<Tree<Rule>> createSample(SamplingConfiguration config){
        List<Tree<Rule>> list = null;
        
        for(int i=1;i<=config.outerPreSamplingRounds;++i){
            TreeAutomaton benign = sampleBenign(config);
            if(benign == null){
                return new ArrayList<>();
            }
            
            this.worker.setAutomaton(benign);
            
            this.worker.getSample(config);
        }
        
        this.worker.getSample(config);
        
        return list;
    }

    /**
     * 
     * @return 
     */
    private TreeAutomaton sampleBenign(SamplingConfiguration sc) {
        FromRuleTreesAutomaton result = new FromRuleTreesAutomaton(malign);
        
        for(int i=0;i<sc.outerSampleSize;++i){
            Tree<Rule> t = null;
            
            while(t == null){
                double sum = 0.0;
                IntIterator iit = this.malign.getFinalStates().iterator();
                
                while (iit.hasNext()) {
                    int state = iit.nextInt();
                    sum += this.getSmoothedStateWeigth(state);
                }
                
                if(sum <= 0.0){
                    return null;
                }
                double split = sum*this.rg.nextDouble();
                
                iit = this.malign.getFinalStates().iterator();
                while (iit.hasNext()) {
                    int state = iit.nextInt();
                    split -= this.getSmoothedStateWeigth(iit.nextInt());
                    
                    if(split <= 0.0){
                        t = this.sampleFromState(state);
                        if(t == null){
                            this.setUseLess(state);
                        }
                        break;
                    }
                }
            }
            
            result.addRules(t);
        }
        
        return result;
    }

    /**
     * 
     * @param state
     * @return 
     */
    public double getSmoothedStateWeigth(int state) {
        if(this.useLess.contains(state)){
            return 0.0;
        }else{
            return this.worker.getStateCount(state)+this.smooth;
        }
    }

    /**
     * 
     * @param state
     * @return 
     */
    private Tree<Rule> sampleFromState(int state) {
        List<Tree<Rule>> children = new ArrayList<>();
        
        Set<Rule> imp = new ObjectOpenHashSet<>();
        
        Iterable<Rule> it = this.malign.getAllRulesTopDown();
        double sum = 0.0;
        for(Rule r : it){
            double weight = this.makeRuleWeight(r);
            r.setWeight(weight);
            if(weight > 0.0){
                imp.add(r);
                sum += weight;
            }
        }
        
        while(true){
            double split = sum*this.rg.nextDouble();
            
            if(sum == 0.0 || imp.isEmpty()){
                this.setUseLess(state);
                return null;
            }
            
            for(Rule r : imp){
                split -= r.getWeight();
                
                if(split <= 0.0){
                    children.clear();
                    for(int child : r.getChildren()){
                        Tree<Rule> chVal = this.sampleFromState(child);
                        if(chVal == null){
                            sum -= r.getWeight();
                            r.setWeight(0.0);
                            imp.remove(r);
                            r = null;
                            break;
                        }
                        else{
                            children.add(chVal);
                        }
                    }
                    
                    if(r != null){
                        return Tree.create(r, children);
                    }
                }
            }
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
     */
    private void setUseLess(int state) {
        this.useLess.add(state);
    }
    
    /**
     * 
     */
    public static class SamplingConfiguration extends SampleBenign.Configuration{
        /**
         * 
         */
        public int outerPreSamplingRounds;
        
        /**
         * 
         */
        public int outerSampleSize;
    }
}