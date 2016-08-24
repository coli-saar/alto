/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author koller
 */
public class QuotientPruningPolicy implements PruningPolicy {
    private final FOM fom;
    private final List<RulePair> rulePairs;
    private final double factor;

    public QuotientPruningPolicy(FOM fom, double factor) {
        this.fom = fom;
        this.factor = factor;
        rulePairs = new ArrayList<>();
    }

    @Override
    public void foreachPrunedRulePair(RulePairConsumer consumer) {
        if (!rulePairs.isEmpty()) {
            Collections.sort(rulePairs);
            
            double maxValue = rulePairs.get(0).value;
            
            for( int i = 0; i < rulePairs.size(); i++ ) {
                RulePair rp = rulePairs.get(i);
                
                if( rp.value < maxValue * factor ) {
                    break;
                } else {
                    consumer.accept(rp.left, rp.right, rp.value);
                }
            }
        }
    }

    @Override
    public void collect(Rule left, Rule right) {
        rulePairs.add(new RulePair(left, right, fom.evaluate(left, right)));
    }

    @Override
    public void reset() {
        rulePairs.clear();
    }

}
