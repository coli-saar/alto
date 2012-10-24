/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.maxent;

import de.saar.penguin.irtg.automata.Rule;

/**
 *
 * @author koller
 */
public interface FeatureFunction<State> {
    public double evaluate(Rule<State> object);
}
