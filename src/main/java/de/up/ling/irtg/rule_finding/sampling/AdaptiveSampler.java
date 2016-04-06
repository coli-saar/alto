/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

/**
 *
 * @author teichmann
 */
public class AdaptiveSampler {

    /**
     *
     */
    private int populationSize = 1000;

    /**
     *
     */
    private boolean adaptNormalized = true;

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
    public int getPopulationSize() {
        return populationSize;
    }
    
    /**
     * 
     * @param populationSize 
     */
    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }
    
    /**
     * 
     * @return 
     */
    public boolean isAdaptNormalized() {
        return adaptNormalized;
    }
    
    /**
     * 
     * @param adaptNormalized 
     */
    public void setAdaptNormalized(boolean adaptNormalized) {
        this.adaptNormalized = adaptNormalized;
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
    
    
    
    
    
}
