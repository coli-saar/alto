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
public class QuotientPruningPolicy implements PruningPolicy {
    private final FOM fom;
    private final Int2ObjectMap<List<RulePair>> rulePairsPerParent;
    private final double factor;
    private long collectedRules = 0, iteratedRules = 0, unevalRules = 0;

    public QuotientPruningPolicy(FOM fom, double factor) {
        this.fom = fom;
        this.factor = factor;
        rulePairsPerParent = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public void foreachPrunedRulePair(int rightParent, RulePairConsumer consumer) {
        List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);

        if (rulePairs != null && ! rulePairs.isEmpty()) {
            Collections.sort(rulePairs);

            double maxValue = rulePairs.get(0).value;
//            System.err.println("max: " + maxValue);

            for (int i = 0; i < rulePairs.size(); i++) {
                RulePair rp = rulePairs.get(i);

//                System.err.println(" - found " + rp.value + ", quot=" + rp.value / maxValue);

                if (rp.value < maxValue * factor) {
                    break;
                } else {
                    consumer.accept(rp.left, rp.right, rp.value);
                    iteratedRules++;
                }
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
//            throw new RuntimeException("fom value is nan");
        }
    }

    public void printStatistics() {
//        System.err.printf("QuotientPP collected %d rules, skipped %d rules, iterated over %d rules (%5.2f%%).\n", collectedRules, unevalRules, iteratedRules, (100.0 * iteratedRules / collectedRules));
    }
}
