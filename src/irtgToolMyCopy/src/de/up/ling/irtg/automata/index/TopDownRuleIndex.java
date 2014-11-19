/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.index;

import de.up.ling.irtg.automata.Rule;
import it.unimi.dsi.fastutil.ints.IntIterable;

/**
 *
 * @author koller
 */
public interface TopDownRuleIndex {
    public void add(Rule rule);
    
    public Iterable<Rule> getRules(final int parentState);
    public IntIterable getLabelsTopDown(int parentState);
    public Iterable<Rule> getRules(final int labelId, final int parentState);
    public boolean useCachedRule(int label, int parent);
}
