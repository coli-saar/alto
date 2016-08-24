/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author koller
 */
@FunctionalInterface
public interface RulePairConsumer {
    public void accept(Rule left, Rule right, double value);
}
