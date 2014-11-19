/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata;

/**
 *
 * @author koller
 */
public interface RuleEvaluator<E> {
    E evaluateRule(Rule rule);
}
