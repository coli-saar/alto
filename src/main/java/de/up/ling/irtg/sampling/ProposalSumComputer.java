/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.util.LogSpaceOperations;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * This class is used to compute the cumulative probability of proposing a given tree in case of an ambiguous automaton.
 * 
 * If there are more than one ways of generating a tree from the underlying automaton, the we need to compute the sum of
 * their probabilities. If the computer is used again after changing the underlying rule weights, then it must be reset,
 * because it stores inside values that have already been computed.
 * 
 * @author christoph
 */
public class ProposalSumComputer {
    /**
     * A cache used that holds inside values that have already been computed.
     */
    private final Int2ObjectMap<Object2DoubleMap<Tree<Rule>>> insides =
                                                  new Int2ObjectOpenHashMap<>();

    /**
     * Computes the proposal probability for each tree in the sample assuming the
     * given rule weightings and sets it as the log sum weight of the sample.
     * 
     * @param sample
     * @param rw 
     */
    public void fillInsides(TreeSample<Rule> sample, RuleWeighting rw) {
        for(int i=0;i<sample.populationSize();++i) {
            sample.setLogSumWeight(i, this.computeInside(sample.getSample(i), rw));
        }
    }
    
    
    /**
     * Computes the proposal probability for the given tree assuming the given
     * rule weights.
     * 
     * @param input
     * @param weights
     * @return 
     */
    public double computeInside(Tree<Rule> input, RuleWeighting weights) {
        double logSum = Double.NEGATIVE_INFINITY;
        
        weights.prepareStartProbability();
        
        for(int i=0;i<weights.getNumberOfStartStates();++i) {
            int startState = weights.getStartStateByNumber(i);
            
            double weight;
            if(containsCombination(startState,input)) {
                weight = this.getCombination(startState,input);
            } else {
                weight = weights.getStateStartLogProbability(i);
                weight += this.computeInside(input,startState,weights);
            }
            
            LogSpaceOperations.add(weight, logSum);
        }
        
        return logSum;
    }
    
    /**
     * Computes the proposal probability for the given tree assuming the given
     * rule weights and assuming that we start at the given state.
     * 
     * @param input
     * @param state
     * @param weights
     * @return 
     */
    public double computeInside(Tree<Rule> input, int state, RuleWeighting weights) {
        if(this.containsCombination(state, input)) {
            return this.getCombination(state, input);
        }
        
        Rule r = input.getLabel();
        
        weights.prepareProbability(state);
        int label = r.getLabel();
        
        Iterable<Rule> it = weights.getAutomaton().getRulesTopDown(label, state);
        
        double sum = Double.NEGATIVE_INFINITY;
        
        for(Rule expand : it) {
            double amount = weights.getLogProbability(expand);
            
            for(int i=0;i<r.getArity();++i) {
                amount += this.computeInside(input.getChildren().get(i), expand.getChildren()[i], weights);
            }
            
            LogSpaceOperations.add(amount, sum);
        }
        
        Object2DoubleMap<Tree<Rule>> map = this.insides.get(state);
        if(map == null) {
            map = new Object2DoubleOpenHashMap<>();
        }
        
        map.put(input, sum);
        
        return sum;
    }
    
    /**
     * This resets the cache of the inside values.
     */
    public void reset() {
        insides.clear();
    }

    /**
     * Returns true if we have already seen the tree derived from the given state.
     * @param startState
     * @param input
     * @return 
     */
    private boolean containsCombination(int startState, Tree<Rule> input) {
        Object2DoubleMap<Tree<Rule>> inner = this.insides.get(startState);
        
        if(inner == null) {
            return false;
        }
        
        return inner.containsKey(input);
    }

    /**
     * Returns the cached probability for generating the given tree starting
     * at the given state.
     * 
     * @param startState
     * @param input
     * @return 
     */
    private double getCombination(int startState, Tree<Rule> input) {
        Object2DoubleMap<Tree<Rule>> inner = this.insides.get(startState);
        
        if(inner == null) {
            return Double.NEGATIVE_INFINITY;
        }
        
        return inner.getDouble(input);
    }
}
