/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author koller
 */
public class QuotientPruningPolicy implements PruningPolicy {
    private final FOM fom;
    private final Int2ObjectMap<List<RulePair>> rulePairsPerParent;
    private final Int2DoubleMap bestFomPerParent;
    private final double factor;
    private long collectedRules = 0, iteratedRules = 0, unevalRules = 0;

    public QuotientPruningPolicy(FOM fom, double factor) {
        this.fom = fom;
        this.factor = factor;
        rulePairsPerParent = new Int2ObjectOpenHashMap<>();
        
        bestFomPerParent = new Int2DoubleOpenHashMap();
        bestFomPerParent.defaultReturnValue(Double.NEGATIVE_INFINITY);
    }

    @Override
    public void foreachPrunedRulePair(int rightParent, RulePairConsumer consumer) {
        List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);
        int iteratedHere = 0;

        if (rulePairs != null && ! rulePairs.isEmpty()) {
            double maxValue = bestFomPerParent.get(rightParent);

            for (int i = 0; i < rulePairs.size(); i++) {
                RulePair rp = rulePairs.get(i);
                
                if (rp.value > maxValue * factor) {
                    consumer.accept(rp.left, rp.right, rp.value);
                    iteratedRules++;
                    iteratedHere++;
                }
            }

            // clean up
            rulePairsPerParent.remove(rightParent);
            
//            System.err.printf("(%d/%d)", iteratedHere, rulePairs.size());
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
        
        // update max
        if( value > bestFomPerParent.get(rightParent)) {
            bestFomPerParent.put(rightParent, value);
        }
    }
//
//    @Override
//    public void printStatistics() {
////        System.err.printf("QuotientPP collected %d rules, skipped %d rules, iterated over %d rules (%5.2f%%).\n", collectedRules, unevalRules, iteratedRules, (100.0 * iteratedRules / collectedRules));
//        System.err.printf("[%d/%d rules = %5.2f%%] ", iteratedRules, collectedRules, (100.0 * iteratedRules / collectedRules));
//    }
    
    
    @Override
    @OperationAnnotation(code = "numIteratedRules")
    public long numIteratedRules() {
        return iteratedRules;
    }

    @Override
    @OperationAnnotation(code = "numCollectedRules")
    public long numCollectedRules() {
        return collectedRules;
    }
    
    @OperationAnnotation(code = "ppQuotient")
    public static PruningPolicy createQuotientPruningPolicy(FOM fom, double factor) {
        return new QuotientPruningPolicy(fom, factor);
    }
}
