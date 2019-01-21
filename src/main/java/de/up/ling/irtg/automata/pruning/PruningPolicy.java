/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.laboratory.OperationAnnotation;

/**
 *
 * @author koller
 */
public interface PruningPolicy {
    void foreachPrunedRulePair(int rightParent, RulePairConsumer consumer);
    void collect(int rightParent, Rule left, CondensedRule right);
    
    @OperationAnnotation(code = "numIteratedRules")
    long numIteratedRules();
    
    @OperationAnnotation(code = "numCollectedRules")
    long numCollectedRules();
}
