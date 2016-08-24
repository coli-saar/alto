/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class NoPruningPolicy implements PruningPolicy {
    private final List<RulePair> rulePairs;

    public NoPruningPolicy() {
        rulePairs = new ArrayList<>();
    }
    
    @Override
    public void foreachPrunedRulePair(RulePairConsumer consumer) {
        rulePairs.forEach(rp -> consumer.accept(rp.left, rp.right, rp.value));
    }

    @Override
    public void collect(Rule left, Rule right) {
        rulePairs.add(new RulePair(left, right, 1));
    }

    @Override
    public void reset() {
        rulePairs.clear();
    }
    
}
