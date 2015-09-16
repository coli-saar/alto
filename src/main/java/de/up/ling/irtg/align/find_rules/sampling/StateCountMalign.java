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
public class StateCountMalign extends SampleMalign {
    /**
     * 
     * @param worker 
     */
    public StateCountMalign(SampleBenign worker) {
        super(worker);
    }

    /**
     * 
     * @param worker
     * @param smooth 
     */
    public StateCountMalign(SampleBenign worker, double smooth) {
        super(worker, smooth);
    }

    /**
     * 
     * @param worker
     * @param smooth
     * @param seed 
     */
    public StateCountMalign(SampleBenign worker, double smooth, long seed) {
        super(worker, smooth, seed);
    }

    @Override
    protected double makeRuleWeight(Rule r) {
        return this.getSmoothedStateWeigth(r.getParent());
    }
}