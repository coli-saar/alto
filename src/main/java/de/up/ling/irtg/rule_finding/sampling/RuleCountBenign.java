/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 * Implements the Abstract SampleBenign interface by weighting each rule
 * according to how often it was seen plus smoothing.
 * 
 * @author christoph_teichmann
 */
public class RuleCountBenign extends SampleBenign {
    /**
     * Creates a new instance that is set to sample the benign automaton.
     * 
     * @param benign
     * @param smooth 
     */
    public RuleCountBenign(double smooth, TreeAutomaton benign) {
        super(smooth, benign);
    }

    /**
     * Creates a new instance that is set to sample the benign automaton.
     * 
     * This version of the constructor is used for debugging, since it uses
     * an externally determined seed.
     * 
     * @param smooth
     * @param seed 
     * @param benign 
     */
    public RuleCountBenign(double smooth, long seed, TreeAutomaton benign) {
        super(smooth, seed, benign);
    }

    @Override
    protected double makeRuleWeight(Rule r) {    
        return this.getSmoothedRuleCount(r);
    }
}