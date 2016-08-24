/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.CondensedRule;

/**
 *
 * @author koller
 */
public interface PruningPolicy {
    public void foreachPrunedRulePair(RulePairConsumer consumer);
    public void collect(Rule left, CondensedRule right);
    public void reset();
}
