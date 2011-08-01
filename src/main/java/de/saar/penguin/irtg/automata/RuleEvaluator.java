/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata;

/**
 *
 * @author koller
 */
public interface RuleEvaluator<State,E> {
    E evaluateRule(Rule<State> rule);
}
