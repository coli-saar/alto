/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author koller
 */
public class HistogramPruningPolicy implements PruningPolicy {
    private final FOM fom;
    private final int k;
    
    private final Int2ObjectMap<List<RulePair>> rulePairsPerParent;
    private long collectedRules = 0, iteratedRules = 0, unevalRules = 0;

    public HistogramPruningPolicy(FOM fom, int k) {
        this.fom = fom;
        this.k = k;
        rulePairsPerParent = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public void foreachPrunedRulePair(int rightParent, RulePairConsumer consumer) {
        List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);
        
        if (rulePairs != null && ! rulePairs.isEmpty()) {
            Collections.sort(rulePairs);
            int N = Math.min(k, rulePairs.size());
            
            for( int i = 0; i < N; i++ ) {
                RulePair rp = rulePairs.get(i);
                consumer.accept(rp.left, rp.right, rp.value);
                iteratedRules++;
            }
            
            // clean up
            rulePairsPerParent.remove(rightParent);
        }
    }

    @Override
    public void collect(int rightParent, Rule left, CondensedRule right) {
        List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);

        if (rulePairs == null) {
            rulePairs = new ArrayList<>();
            rulePairsPerParent.put(rightParent, rulePairs);
        }

        double value = fom.evaluate(left, right);
        
        if (!Double.isNaN(value)) {
            rulePairs.add(new RulePair(left, right, value));
            collectedRules++;
        } else {
            unevalRules++;
        }
    }
    
    public void printStatistics() {
//        System.err.printf("QuotientPP collected %d rules, skipped %d rules, iterated over %d rules (%5.2f%%).\n", collectedRules, unevalRules, iteratedRules, (100.0 * iteratedRules / collectedRules));
        System.err.printf("[%d/%d rules = %5.2f%%] ", iteratedRules, collectedRules, (100.0 * iteratedRules / collectedRules));
    }
    
}
