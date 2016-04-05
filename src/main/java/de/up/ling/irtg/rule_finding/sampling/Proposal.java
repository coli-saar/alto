/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BiFunction;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

/**
 *
 * @author christoph
 */
public class Proposal {
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     * @param rg 
     */
    public Proposal(RandomGenerator rg) {
        this.rg = rg;
    }
    
    /**
     * 
     * @param seed 
     */
    public Proposal(long seed) {
        this(new Well44497a(seed));
    }
    
    /**
     * 
     */
    public Proposal() {
        this(new Date().getTime());
    }
    
    /**
     * 
     */
    private static final BiFunction<Rule,TreeAutomaton,Integer> RAW_MAPPING = (Rule r, TreeAutomaton t) -> r.getLabel();
    
    /**
     * 
     * @param carrier
     * @param guide
     * @param sampleSize
     * @return 
     */
    public TreeSample<Integer> getRawTreeSample(RuleWeighting guide, int sampleSize) {
       return this.getTreeSample(RAW_MAPPING, guide, sampleSize);
    }
    
    /**
     * 
     */
    private static final BiFunction<Rule,TreeAutomaton,Rule> RULE_MAPPING = (Rule r, TreeAutomaton t) -> r;
    
    /**
     * 
     * @param carrier
     * @param guide
     * @param sampleSize
     * @return 
     */
    public TreeSample<Rule> getRuleTreeSample(RuleWeighting guide, int sampleSize) {
        return this.getTreeSample(RULE_MAPPING, guide, sampleSize);
    }
    
    /**
     * 
     */
    private final static BiFunction<Rule,TreeAutomaton, String> STRING_MAPPING =
                                (Rule rule, TreeAutomaton t) -> t.getSignature().resolveSymbolId(rule.getLabel());
    
    /**
     * 
     * @param carrier
     * @param guide
     * @param sampleSize
     * @return 
     */
    public TreeSample<String> getStringTreeSample(RuleWeighting guide, int sampleSize) {
        return this.getTreeSample(STRING_MAPPING, guide, sampleSize);
    }
    
    /**
     * 
     * @param <Type>
     * @param mapping
     * @param carrier
     * @param guide
     * @param numberOfSamples
     * @return 
     */
    public <Type> TreeSample<Type> getTreeSample(BiFunction<Rule,TreeAutomaton,Type> mapping,
                                                    RuleWeighting guide, int numberOfSamples) {
        guide.prepareStateStartProbability();
        TreeSample result = new TreeSample();
        
        int i=0;
        while(i < numberOfSamples) {
            MutableDouble logPropProb = new MutableDouble(0.0);
            
            double d = this.rg.nextDouble();
            int start = guide.getStartState(d);
            
            logPropProb.add(guide.getStateStartProbability(start));
            Tree<Type> done = this.sampleTree(start, guide, mapping, logPropProb);
            
            result.addSample(done, logPropProb.getValue());
            
            ++i;
        }
        
        return result;
    }

    /**
     * 
     * @param <Type>
     * @param start
     * @param guide
     * @param mapping
     * @param logPropProb
     * @return 
     */
    private <Type> Tree<Type> sampleTree(int start, RuleWeighting guide, BiFunction<Rule, TreeAutomaton, Type> mapping, MutableDouble logPropProb) {
        
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
