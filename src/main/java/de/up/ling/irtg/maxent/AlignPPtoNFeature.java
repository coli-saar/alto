/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;
import de.saar.basic.Pair;

/**
 *
 * @author danilo
 */
public class AlignPPtoNFeature extends FeatureFunction {
    private static String PP_LABEL = "PP";
    private static String N_LABEL = "N";
    @Override
    public double evaluate(Rule rule){
        String parentLabel = this.getLabelFor(rule.getParent());
        if (parentLabel.equals(N_LABEL)) {
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
