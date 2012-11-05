/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.maxent;

import de.saar.penguin.irtg.automata.Rule;

/**
 *
 * @author danilo
 */
public class StaticFeatureFunction implements FeatureFunction<String> {
    private double staticFeatureValue;
    public StaticFeatureFunction(){
        this.staticFeatureValue = 0.5;
    }

    @Override
    public double evaluate(Rule<String> object){
        return this.staticFeatureValue;
    }
}
