/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author teichmann
 */
public interface SamplingStatistics {
    /**
     * 
     * @param round
     * @param sample
     * @param adaptor 
     */
    public void addUnNormalizedRound(int round,
                               TreeSample<Rule> sample, RuleWeighting adaptor);
    
    /**
     * 
     * @param round
     * @param sample
     * @param adaptor 
     */
    public void addNormalizedRound(int round,
                               TreeSample<Rule> sample, RuleWeighting adaptor);

    /**
     * 
     * @param round
     * @param sample
     * @param adaptor 
     */
    public void addResampledRound(int round,
                               TreeSample<Rule> sample, RuleWeighting adaptor);
}
