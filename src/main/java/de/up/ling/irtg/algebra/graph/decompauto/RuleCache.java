/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author koller
 */
public interface RuleCache {
    public Iterable<Rule> put(Iterable<Rule> rules, int labelId, int[] childStates);
    public Iterable<Rule> get(int labelId, int[] childStates);
}
