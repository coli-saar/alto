/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;

/**
 * A trivial feature class that is only used for testing.
 * It is only suitable to illustrate how to declare a feature
 * function whose constructor takes arguments.
 * Do not use this in your own program. 
 * 
 * @author koller
 */
public class TestFeature extends FeatureFunction<String> {
    private String x;

    public TestFeature(String x) {
        this.x = x;
    }

    public String getX() {
        return x;
    }

    @Override
    public double evaluate(Rule<String> object) {
        if( object.getLabel().equals(x) ) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(super.toString());
        ret.append("('");
        ret.append(x);
        ret.append("')");
        return ret.toString();
    }
}
