/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

/**
 *
 * @author christoph_teichmann
 */
public abstract class StateWeighter {
    
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     */
    private TreeAutomaton basis;
    
    
    /**
     * 
     */
    private final IntSet useless = new IntOpenHashSet();

    /**
     * 
     * @param basis 
     * @param seed 
     */
    public StateWeighter(TreeAutomaton basis, long seed) {
        this.basis = basis;
        this.rg = new Well44497a(seed);
    }
    
    /**
     * 
     * @param basis 
     */
    public StateWeighter(TreeAutomaton basis) {
        this.basis = basis;
        this.rg = new Well44497a();
    }
    
    /**
     * 
     * @param state 
     */
    public void setUseles(int state){
        this.useless.add(state);
    }
    
    /**
     * 
     * @param state
     * @return 
     */
    public boolean isUseless(int state){
        return this.useless.contains(state);
    }
    
    /**
     * 
     * @return 
     */
    public double logWeightOfLastSample(){
        return lastSampleWeight;
    }
    
    /**
     * 
     */
    private double lastSampleWeight = 0.0;
    
    /**
     * 
     * @return 
     */
    public Tree<Rule> nextSample(){
        while (true) {
            this.lastSampleWeight = 0.0;

            IntIterator fin = this.basis.getFinalStates().iterator();
            double sum = 0.0;

            while (fin.hasNext()) {
                int state = fin.nextInt();
                sum += this.getFullStateWeight(state);
            }

            if (sum <= 0.0) {
                throw new IllegalStateException("Weight of start states is not larger than 0.0.");
            }

            double threshold = sum * this.rg.nextDouble();

            int state;
            fin = this.basis.getFinalStates().iterator();
            while (fin.hasNext()) {
                state = fin.nextInt();

                threshold -= this.getFullStateWeight(state);

                if (threshold <= 0.0) {
                    break;
                }

                Tree<Rule> ti = this.sampleTreeForState(state);

                if (ti != null) {
                    this.lastSampleWeight += Math.log(this.getFullStateWeight(state));
                    this.lastSampleWeight -= Math.log(sum);

                    return ti;
                } else {
                    this.useless.add(state);
                }
            }
        }
    }

    /**
     * 
     * @param state
     * @return 
     */
    private double getFullStateWeight(int state) {
        if(this.isUseless(state)){
            return 0.0;
        }
        else{
            return this.getStateWeight(state);
        }
    }

    /**
     * 
     * @param state
     * @return 
     */
    private Tree<Rule> sampleTreeForState(int state) {
        Iterable<Rule> it = this.basis.getRulesTopDown(state);
        List<Tree<Rule>> children = new ObjectArrayList<>();
        
        double oldWeight = this.lastSampleWeight;
        
        mainLoop : while(true){
            this.lastSampleWeight = oldWeight;
            double sum = 0.0;
            
            for(Rule r : it){
                sum += makeWeight(r);
            }
            
            if(sum <= 0.0){
                this.setUseles(state);
                return null;
            }
            
            double border = sum*this.rg.nextDouble();
            
            Rule r = null;
            
            for(Rule k : it){
                border -= makeWeight(k);
                
                if(border <= 0.0){
                    r = k;
                    break;
                }
            }
            
            if(r == null){
                this.setUseles(state);
                return null;
            }
            
            children.clear();
            
            for(int i=0;i<r.getArity();++i){
                Tree<Rule> t = this.sampleTreeForState(r.getChildren()[i]);
                
                if(t == null){
                    continue mainLoop;
                }else{
                    children.add(t);
                }
            }
            
            this.lastSampleWeight += Math.log(this.makeWeight(r)) - Math.log(sum);
            
            return Tree.create(r, children);
        }
    }

    /**
     * 
     * @param state
     * @return 
     */
    public abstract double getStateWeight(int state);

    /**
     * 
     * @param r
     * @return 
     */
    private double makeWeight(Rule r) {
        double score = 0.0;
        
        for(int i=0;i<r.getArity();++i){
            double val = this.getFullStateWeight(r.getChildren()[i]);
            
            if(val <= 0.0){
                return 0.0;
            }else{
                score = combine(score,val);
            }
        }
        
        return score;
    }

    /**
     * 
     * @param score
     * @param val
     * @return 
     */
    protected abstract double combine(double score, double val);
    
    /**
     * 
     * @param observation
     * @param weight 
     */
    protected abstract void update(Tree<Rule> observation, double weight);
    
    /**
     * 
     */
    public void reset(){
        this.useless.clear();
    }

    /**
     * 
     * @return 
     */
    public TreeAutomaton getBasis() {
        return basis;
    }

    /**
     * 
     * @param basis 
     */
    public void setBasis(TreeAutomaton basis) {
        this.basis = basis;
        this.reset();
    }
}