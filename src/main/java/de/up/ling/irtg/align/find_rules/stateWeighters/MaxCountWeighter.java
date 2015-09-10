/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.stateWeighters;

import de.up.ling.irtg.align.find_rules.StateWeighter;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 *
 * @author christoph
 */
public class MaxCountWeighter extends StateWeighter {
    
    /**
     * 
     */
    private final double smooth;
    
    /**
     * 
     */
    private final Int2DoubleOpenHashMap counts;
    
    /**
     * 
     * @param basis 
     */
    public MaxCountWeighter(TreeAutomaton basis){
        this(basis,0.01);
    }
    
    /**
     * 
     * @param basis 
     * @param smooth 
     */
    public MaxCountWeighter(TreeAutomaton basis, double smooth) {
        super(basis);
        this.counts = new Int2DoubleOpenHashMap();
        this.smooth = smooth;
    }
    
    /**
     * 
     * @param basis
     * @param seed 
     * @param smooth 
     */
    public MaxCountWeighter(TreeAutomaton basis, long seed, double smooth) {
        super(basis, seed);
        this.smooth = smooth;
        this.counts = new Int2DoubleOpenHashMap();
    }

    @Override
    public double getStateWeight(int state) {
        return this.counts.get(state)+this.smooth;
    }

    @Override
    protected double combine(double score, double val) {
        return Math.max(score, val);
    }

    @Override
    protected void update(Tree<Rule> observation, double weight) {
        Rule r = observation.getLabel();
        
        this.counts.addTo(r.getParent(), weight);
        
        for(int i=0;i<observation.getChildren().size();++i){
            this.update(observation.getChildren().get(i), weight);
        }
    }

    @Override
    public void reset() {
        counts.clear();
    }
}
