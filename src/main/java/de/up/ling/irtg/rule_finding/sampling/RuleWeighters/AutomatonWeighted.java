/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.RuleWeighters;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.tree.Tree;
import java.util.List;

/**
 *
 * @author teichmann
 */
public class AutomatonWeighted extends RegularizedKLRuleWeighting {
    /**
     * 
     * @param basis
     * @param normalizationExponent
     * @param normalizationDivisor
     * @param rate 
     */
    public AutomatonWeighted(TreeAutomaton basis, int normalizationExponent, double normalizationDivisor, LearningRate rate) {
        super(basis, normalizationExponent, normalizationDivisor, rate);
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
