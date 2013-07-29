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
    public E initialValue();
    public E evaluateRule(Rule rule, int i);
}
