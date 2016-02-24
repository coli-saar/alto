/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.language_iteration;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 *
 * @author koller
 */
public class IdentityRuleRefiner implements RuleRefiner {
    @Override
    public IntList refine(int label) {
        IntList ret = new IntArrayList();
        ret.add(label);
        return ret;
    }
    
}
