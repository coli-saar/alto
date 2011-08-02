/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

/**
 *
 * @author koller
 */
public interface RuleEvaluatorTopDown<State, E> {
    public E initialValue();
    public E evaluateRule(Rule<State> rule, int i);
}
