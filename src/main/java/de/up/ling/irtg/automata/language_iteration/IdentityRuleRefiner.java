/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class IdentityRuleRefiner implements RuleRefiner {
    @Override
    public List<Rule> refine(Rule rule) {
        List<Rule> ret = new ArrayList<>();
        ret.add(rule);
        return ret;
    }
    
}
