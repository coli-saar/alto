/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Predicate;

/**
 *
 * @author koller
 */
public class SkipFailRulesFilter implements Predicate<Rule> {
    private TreeAutomaton auto;

    public SkipFailRulesFilter(TreeAutomaton auto) {
        this.auto = auto;
    }
    
    @Override
    public boolean apply(Rule t) {
        return ! InverseHomAutomaton.isFailedRule(t, auto);
    }    
}
