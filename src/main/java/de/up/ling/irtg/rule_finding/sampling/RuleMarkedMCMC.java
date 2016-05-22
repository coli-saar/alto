/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Date;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;

/**
 * This class assumes that there is exactly one start state.
 * 
 * @author christoph_teichmann
 */
public class RuleMarkedMCMC {
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     * @param rg 
     */
    public RuleMarkedMCMC(RandomGenerator rg) {
        this.rg = rg;
    }
    
    /**
     * 
     * @param seed 
     */
    public RuleMarkedMCMC(long seed) {
        this(new Well44497b(seed));
    }
    
    /**
     * 
     */
    public RuleMarkedMCMC() {
        this(new Date().getTime());
    }
    
    /**
     * Does not work when automaton is ambigous.
     * 
     * @param marks
     * @return 
     */
    public Tree<Rule> sample(RuleMarking marks) {
        IntIterator reached = marks.iterator();
        DoubleList probs = new DoubleArrayList();
        
        while(reached.hasNext()) {
            int parent = reached.nextInt();
            probs.clear();
            double max = Double.NEGATIVE_INFINITY;
                        
            int numberOfRules = marks.getNumberOfRules(parent);
            for(int ruleNumber=0;ruleNumber<numberOfRules;++ruleNumber) {
                Rule r = marks.getRule(parent,ruleNumber);
                
                double insideStepLog = marks.getInsideChoiceLogProb(r);
                marks.setRule(parent,ruleNumber);
                
                Tree<Rule> tree = marks.getCurrentTree();
                double actualLog = marks.getLogTargetProb(tree);
                
                double val = actualLog-insideStepLog;
                probs.add(val);
                max = Math.max(max, val);
            }
            
            double sum = 0.0;
            for(int i=0;i<probs.size();++i) {
                double val = Math.exp(probs.getDouble(i)-max);
                sum += val;
                probs.set(i, val);
            }
            
            double remaining = this.rg.nextDouble();
            int choice = -1;
            for(int i=0;i<probs.size();++i) {
                remaining -= probs.getDouble(i);
                
                if(remaining <= 0.0) {
                    choice = 1;
                }
            }
            
            if(!Double.isFinite(remaining) || choice < 0) {
                throw new IllegalStateException("Problem with probabilities "+probs);
            }
            
            marks.setRule(parent, choice);
        }
        
        return marks.getCurrentTree();
    }
}
