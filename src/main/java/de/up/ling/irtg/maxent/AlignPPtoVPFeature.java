/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author danilo
 */
public class AlignPPtoVPFeature extends FeatureFunction {
    private static String PP_LABEL = "PP";
    private static String VP_LABEL = "VP";
    @Override
    public double evaluate(Rule rule){
        String parentLabel = this.getLabelFor(rule.getParent());
        if (parentLabel.equals(VP_LABEL)) {
            for (Object child : rule.getChildren()) {
                String label = this.getLabelFor(child);
                if (label.equals(PP_LABEL)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }
}
