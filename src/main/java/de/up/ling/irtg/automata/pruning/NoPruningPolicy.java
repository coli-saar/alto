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
import java.util.List;

/**
 *
 * @author koller
 */
public class NoPruningPolicy implements PruningPolicy {
    private final Int2ObjectMap<List<RulePair>> rulePairsPerParent;

    public NoPruningPolicy() {
        rulePairsPerParent = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public void foreachPrunedRulePair(int rightParent, RulePairConsumer consumer) {
        List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);

        if (rulePairs != null) {
            rulePairs.forEach(rp -> consumer.accept(rp.left, rp.right, rp.value));
        }
        
        rulePairsPerParent.remove(rightParent);
    }

    @Override
    public void collect(int rightParent, Rule left, CondensedRule right) {
        List<RulePair> rulePairs = rulePairsPerParent.get(rightParent);
        
        if( rulePairs == null ) {
            rulePairs = new ArrayList<>();
            rulePairsPerParent.put(rightParent, rulePairs);
        }
        
        
        rulePairs.add(new RulePair(left, right, 1));
    }
}
