/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
    private final TreeAutomaton basis;
    
    
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
    public Tree<Integer> nextSample(){
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

                Tree<Integer> ti = this.sampleTreeForState(state);

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
    private Tree<Integer> sampleTreeForState(int state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param state
     * @return 
     */
    public abstract double getStateWeight(int state);
}