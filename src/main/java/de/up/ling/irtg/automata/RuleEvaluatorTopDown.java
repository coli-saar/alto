/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

/**
 *
 * @author koller
 */
public interface RuleEvaluatorTopDown<E> {
    E initialValue();
    E evaluateRule(Rule rule, int i);
}
