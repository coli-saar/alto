/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author christoph_teichmann
 */
public class RuleCountBenign extends SampleBenign {
    /**
     * 
     * @param smooth 
     */
    public RuleCountBenign(double smooth) {
        super(smooth);
    }

    /**
     * 
     * @param smooth
     * @param seed 
     */
    public RuleCountBenign(double smooth, long seed) {
        super(smooth, seed);
    }

    @Override
    protected double makeRuleWeight(Rule r) {    
        return this.getSmoothedRuleCount(r);
    }   
}