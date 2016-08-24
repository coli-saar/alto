/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 *
 * @author koller
 */
public class StatewiseHistogramPruningPolicy implements PruningPolicy {
    private final FOM fom;
    private final int k;

    private final Int2ObjectMap<List<RulePair>> rulePairsPerParent;
    private final Int2ObjectMap<IntSet> partners; // rightParent -> cooccurring leftParents    

    private long collectedRules = 0, iteratedRules = 0, unevalRules = 0;

    public StatewiseHistogramPruningPolicy(FOM fom, int k) {
        this.fom = fom;
        this.k = k;
        rulePairsPerParent = new Int2ObjectOpenHashMap<>();
        partners = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public void foreachPrunedRulePair(int rightParent, RulePairConsumer consumer) {
        if (partners.get(rightParent) != null && !partners.get(rightParent).isEmpty()) {
            // compute list of left states that occurred with rightParent, sorted descending by FOM            
            IntSet partnersHere = partners.get(rightParent);
            final int[] aPartners = getSortedLeftPartners(rightParent, partnersHere);

            // obtain permissible left states
            IntSet allowedLeftParents = new IntOpenHashSet();
            int N = Math.min(k, aPartners.length);
            for (int i = 0; i < N; i++) {
                allowedLeftParents.add(aPartners[i]);
            }

            // iterate over rules
            List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);
            assert rulePairs != null;

            for (int i = 0; i < rulePairs.size(); i++) {
                RulePair rp = rulePairs.get(i);

                if (allowedLeftParents.contains(rp.left.getParent())) {
                    consumer.accept(rp.left, rp.right, rp.value);
                    iteratedRules++;
                }
            }

            // clean up
            rulePairsPerParent.remove(rightParent);
        }
    }

    /**
     * Computes the array of left partners of rightParent, sorted descending by
     * total FOM of each (leftstate,rightstate) pair. The method uses arrays
     * and the Fastutil merge-sort method to sort the left partners in place.
     * 
     * @param rightParent
     * @param leftPartners
     * @return 
     */
    private int[] getSortedLeftPartners(int rightParent, IntSet leftPartners) {
        final int[] aPartners = new int[leftPartners.size()];
        final double[] partnerValues = new double[leftPartners.size()];
        int ii = 0;

        for (int partner : leftPartners) {
            double val = fom.evaluateStates(partner, rightParent);
            aPartners[ii] = partner;
            partnerValues[ii++] = val;
        }

        IntComparator sortingComparator = new IntComparator() {
            @Override
            public int compare(int i1, int i2) {
                return -Double.compare(partnerValues[i1], partnerValues[i2]);
            }

            @Override
            public int compare(Integer o1, Integer o2) {
                return compare(o1.intValue(), o2.intValue());
            }
        };

        Swapper sortingSwapper = (int i1, int i2) -> {
            int h = aPartners[i1];
            aPartners[i1] = aPartners[i2];
            aPartners[i2] = h;
            
            double hh = partnerValues[i1];
            partnerValues[i1] = partnerValues[i2];
            partnerValues[i2] = hh;
        };

        Arrays.mergeSort(0, aPartners.length, sortingComparator, sortingSwapper);
        
        return aPartners;
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

        // collect partners
        IntSet partnersHere = partners.get(rightParent);
        if (partnersHere == null) {
            partnersHere = new IntOpenHashSet();
            partners.put(rightParent, partnersHere);
        }

        partnersHere.add(left.getParent());
    }

    public void printStatistics() {
//        System.err.printf("QuotientPP collected %d rules, skipped %d rules, iterated over %d rules (%5.2f%%).\n", collectedRules, unevalRules, iteratedRules, (100.0 * iteratedRules / collectedRules));
        System.err.printf("[%d/%d rules = %5.2f%%] ", iteratedRules, collectedRules, (100.0 * iteratedRules / collectedRules));
    }

}
