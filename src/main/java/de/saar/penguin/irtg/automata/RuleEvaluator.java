/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata;

import java.util.List;

/**
 *
 * @author koller
 */
public interface RuleEvaluator<State,E> {
    E evaluateRule(State parent, String label, List<State> children);
}
