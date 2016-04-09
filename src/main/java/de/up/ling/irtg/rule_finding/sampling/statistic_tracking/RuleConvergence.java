/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.statistic_tracking;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighting;
import de.up.ling.irtg.rule_finding.sampling.SamplingStatistics;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;

/**
 *
 * @author teichmann
 */
public class RuleConvergence implements SamplingStatistics {
    /**
     * 
     */
    private final TreeAutomaton lookUp;
    
    /**
     * 
     */
    private int[] tracked;
    
    /**
     * 
     */
    private ArrayList<Object2DoubleMap<Rule>>[] tracker;
    
    /**
     * 
     * @param lookUp
     * @param toTrack 
     */
    public RuleConvergence(TreeAutomaton lookUp, int[] toTrack) {
        this.lookUp = lookUp;
        this.tracked = toTrack;
        
        tracker = new ArrayList[toTrack.length];
        for(int i=0;i<tracked.length;++i) {
            tracker[i] = new ArrayList<>();
        }
    }
    
    @Override
    public void addUnNormalizedRound(int round, TreeSample<Rule> sample, RuleWeighting adaptor) {
        return;
    }
    
    @Override
    public void addNormalizedRound(int round, TreeSample<Rule> sample, RuleWeighting adaptor) {
        return;
    }
    
    @Override
    public void addResampledRound(int round, TreeSample<Rule> sample, RuleWeighting adaptor) {
        for(int i=0;i<tracked.length;++i) {
            int state = tracked[i];
            
            Object2DoubleOpenHashMap<Rule> weights = new Object2DoubleOpenHashMap<>();
            this.lookUp.foreachRuleTopDown(state, (Object o) -> {
                Rule r = (Rule) o;
                weights.put(r, adaptor.getLogProbability(r));
            });
               
            tracker[i].add(weights);
        }
    }
    
    public ArrayList<Object2DoubleMap<Rule>>[] getData() {
        return this.tracker;
    }
}
