/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;

/**
 * A feature that returns 1 iff the rule name matches
 * the name that is passed to the constructor. The feature
 * function returns 0 otherwise.
 * 
 * @author koller
 */
public class RuleNameFeature extends FeatureFunction<String> {
    private int x;

    public RuleNameFeature(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    @Override
    public double evaluate(Rule<String> object) {
        if( object.getLabel() == x ) { 
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
