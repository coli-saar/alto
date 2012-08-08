/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.base.Predicate;

/**
 *
 * @author koller
 */
public class SkipFailRulesFilter<State> implements Predicate<Rule<State>> {
    @Override
    public boolean apply(Rule<State> t) {
        return ! InverseHomAutomaton.isFailedRule(t);
    }    
}
