/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author teichmann
 */
public class AdaptiveSampler {
    /**
     * 
     */
    private SamplingStatistics keptStats = null;
    
    /**
     *
     */
    private final Proposal prop;

    /**
     *
     * @param seed
     */
    public AdaptiveSampler(long seed) {
        this.prop = new Proposal(seed);
    }

    /**
     *
     * @param prop
     */
    public AdaptiveSampler(Proposal prop) {
        this.prop = prop;
    }

    /**
     *
     */
    public AdaptiveSampler() {
        this.prop = new Proposal();
    }

    /**
     * 
     * @return 
     */
    public SamplingStatistics getKeptStats() {
        return keptStats;
    }

    /**
     * 
     * @param keptStats 
     */
    public void setKeptStats(SamplingStatistics keptStats) {
        this.keptStats = keptStats;
    }
    
    /**
     * 
     * @param rounds
     * @param populationSize
     * @param rw
     * @return 
     */
    public List<TreeSample<Rule>> adaSample(int rounds, int populationSize, RuleWeighting rw) {
        List<TreeSample<Rule>> result = new ArrayList<>();
        rw.reset();
        
        for(int i=0;i<rounds;++i) {
            TreeSample<Rule> sample = this.prop.getRuleTreeSample(rw, populationSize);
            
            for(int j=0;j<populationSize;++j) {
                sample.addWeight(j, rw.getLogTargetProbability(sample.getSample(j)));
            }
            
            if(this.keptStats != null) {
                this.keptStats.addUnNormalizedRound(i,sample);
            }
            
            if(!rw.adaptsNormalized()) {
                rw.adaptNormalized(sample);
            }
            
            sample.expoNormalize();
            
            //TODO include Re-Sampling here
            
            if(rw.adaptsNormalized()) {
                rw.adaptUnNormalized(sample);
            }
            
            if(this.keptStats != null) {
                this.keptStats.addNormalizedRound(rounds, sample);
            }
            
            result.add(sample);
        }
        
        return result;
    }
    
}
