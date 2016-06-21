/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.tree.Tree;
import java.util.Date;
import java.util.function.BiFunction;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

/**
 * Generates a sample of trees according the proposal probabilities of a given
 * rule weighting.
 * 
 * It also keeps track of the log of the proposal probabilities for the the
 * generated trees.
 * 
 * @author christoph
 */
public class Proposal {
    /**
     * The basic random number sequence used for sampling.
     */
    private final RandomGenerator rg;
    
    /**
     * Create a new instance with the given random number sequence.
     * 
     * @param rg 
     */
    public Proposal(RandomGenerator rg) {
        this.rg = rg;
    }
    
    /**
     * Create a new instance with a WELL RNG with the given seed.
     * 
     * @param seed 
     */
    public Proposal(long seed) {
        this(new Well44497a(seed));
    }
    
    /**
     * Create a new instance with a WELL RNG with the current time as the seed.
     */
    public Proposal() {
        this(new Date().getTime());
    }
    
    /**
     * Used to turn rule trees into Integer trees.
     */
    private static final BiFunction<Rule,TreeAutomaton,Integer> RAW_MAPPING = (Rule r, TreeAutomaton t) -> r.getLabel();
    
    /**
     * Get a sample of trees with label IDs for the rules sampled. 
     * 
     * @param guide
     * @param sampleSize
     * @return 
     */
    public TreeSample<Integer> getRawTreeSample(RuleWeighting guide, int sampleSize) {
       return this.getTreeSample(RAW_MAPPING, guide, sampleSize);
    }
    
    /**
     * Returns a sample of rule trees.
     * 
     * @param guide
     * @param sampleSize
     * @return 
     */
    public TreeSample<Rule> getTreeSample(RuleWeighting guide, int sampleSize) {
        return this.getTreeSample(null, guide, sampleSize);
    }
    
    /**
     * Used to turn a sample of rule trees into a sample of string trees.
     */
    private final static BiFunction<Rule,TreeAutomaton, String> STRING_MAPPING =
                                (Rule rule, TreeAutomaton t) -> t.getSignature().resolveSymbolId(rule.getLabel());
    
    /**
     * 
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
     * @param guide
     * @param numberOfSamples
     * @return 
     */
    public <Type> TreeSample<Type> getTreeSample(BiFunction<Rule,TreeAutomaton,Type> mapping,
                                                    RuleWeighting guide, int numberOfSamples) {
        guide.prepareStartProbability();
        TreeSample result = new TreeSample();
        
        int i=0;
        while(i < numberOfSamples) {
            MutableDouble logPropProb = new MutableDouble(0.0);
            
            double d = this.rg.nextDouble();
            int pos = guide.getStartStateNumber(d);
            
            logPropProb.add(guide.getStateStartLogProbability(pos));
            Tree<Type> done = this.sampleTree(guide.getStartStateByNumber(pos), guide, mapping, logPropProb);
            
            result.addSample(done);
            result.setLogPropWeight(i, logPropProb.getValue());
            
            ++i;
        }
        
        return result;
    }

    /**
     * 
     * @param <Type>
     * @param state
     * @param guide
     * @param mapping
     * @param logPropProb
     * @return 
     */
    private <Type> Tree<Type> sampleTree(int state, RuleWeighting guide, BiFunction<Rule, TreeAutomaton, Type> mapping, MutableDouble logPropProb) {
        guide.prepareProbability(state);
        
        double val = this.rg.nextDouble();
        int pos = guide.getRuleNumber(state, val);
        Rule r  = guide.getRuleByNumber(state, pos);
        
        Type label;
        if(mapping != null) {
            label = mapping.apply(r, guide.getAutomaton());
        } else {
            label = (Type) r;
        }
        
        Tree<Type>[] choices = new Tree[r.getArity()];
        logPropProb.add(guide.getLogProbability(state, pos));
        
        for(int i=0;i<r.getArity();++i) {
            choices[i] = this.sampleTree(r.getChildren()[i], guide, mapping, logPropProb);
        }
        
        return Tree.create(label, choices);
    }
}
