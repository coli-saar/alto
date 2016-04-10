/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 *
 * @author christoph
 */
public class ProposalSumComputer {
    /**
     * 
     */
    private final Int2ObjectMap<Object2DoubleMap<Tree<Rule>>> insides =
                                                  new Int2ObjectOpenHashMap<>();
    
    
    public double computeInside(Tree<Rule> input, RuleWeighting weights) {
        double sum = 0.0;
        
        weights.prepareStateStartProbability();
        for(int i=0;i<weights.getNumberOfStartStates();++i) {
            
            
            
        }
        
        //TODO
        return sum;
    }
}
