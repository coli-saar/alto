/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Map;

/**
 * A feature that returns 1 iff the rule name matches
 * the name that is passed to the constructor. The feature
 * function returns 0 otherwise.
 * 
 * @author koller
 */
public class RuleNameFeature extends FeatureFunction<String, Double> {
    private String x;

    public RuleNameFeature(String x) {
        this.x = x;
    }

    public String getX() {
        return x;
    }

    @Override
    public Double evaluate(Rule rule, TreeAutomaton<String> automaton, MaximumEntropyIrtg irtg, Map<String,Object> inputs) {
        if( x.equals(rule.getLabel(irtg.getAutomaton())) ) {
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
