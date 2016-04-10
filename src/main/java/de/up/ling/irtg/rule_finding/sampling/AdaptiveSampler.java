/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;

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
     */
    private final RandomGenerator rg;
    
    /**
     *
     * @param seed
     */
    public AdaptiveSampler(long seed) {
        this.rg = new Well44497b(seed);
        
        this.prop = new Proposal(this.rg.nextLong());
    }

    /**
     *
     */
    public AdaptiveSampler() {
        this(new Date().getTime());
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
     * @param resampleSize
     * @param rw
     * @return 
     */
    public List<TreeSample<Rule>> adaSample(int rounds, int populationSize,
                                           int resampleSize, RuleWeighting rw) {
        List<TreeSample<Rule>> result = new ArrayList<>();
        rw.reset();
        
        for(int i=0;i<rounds;++i) {
            TreeSample<Rule> sample = this.prop.getTreeSample(rw, populationSize);
            
            for(int j=0;j<populationSize;++j) {
                sample.addGoalWeight(j, rw.getLogTargetProbability(sample.getSample(j)));
            }
            
            if(this.keptStats != null) {
                this.keptStats.addUnNormalizedRound(i,sample,rw);
            }
            
            if(!rw.adaptsNormalized()) {
                rw.adaptUnNormalized(sample);
            }
            
            sample.expoNormalize();
            
            if(this.keptStats != null) {
                this.keptStats.addNormalizedRound(rounds, sample, rw);
            }
            
            sample.resample(this.rg,resampleSize);
            
            if(rw.adaptsNormalized()) {
                rw.adaptNormalized(sample);
            }
            
            if(this.keptStats != null) {
                this.keptStats.addResampledRound(rounds, sample, rw);
            }
            
            result.add(sample);
        }
        
        return result;
    }
}
