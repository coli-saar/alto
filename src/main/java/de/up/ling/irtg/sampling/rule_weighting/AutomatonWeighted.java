/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.sampling.rule_weighting;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.tree.Tree;
import java.util.List;

/**
 * This class simply assigns each tree the weight of the multiplication of its
 * rule weights.
 * 
 * The underlying automaton is not changed in any way during sampling.
 * 
 * @author teichmann
 */
public class AutomatonWeighted extends RegularizedKLRuleWeighting {
    /**
     * Creates a new instance that weights trees according to the rule weights
     * from the given automaton and uses the automaton as the basis for sampling.
     * 
     * The adaption is implemented by the parent class.
     * 
     * @param basis
     * @param regularizationExponent
     * @param regularizationDivisor
     * @param rate 
     */
    public AutomatonWeighted(TreeAutomaton basis, int regularizationExponent, double regularizationDivisor, LearningRate rate) {
        super(basis, regularizationExponent, regularizationDivisor, rate);
    }

    @Override
    public double getLogTargetProbability(Tree<Rule> sample) {
        double total = 0.0;
        List<Tree<Rule>> list = sample.getAllNodes();
        
        for(int i=0;i<list.size();++i) {
            total += Math.log(list.get(i).getLabel().getWeight());
        }
        
        return total;
    }
}
