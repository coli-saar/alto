/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.maxent;

import de.saar.penguin.irtg.automata.Rule;
import de.saar.basic.Pair;

/**
 *
 * @author danilo
 */
public class AlignPPtoNFeature implements FeatureFunction {
    private static String PP_LABEL = "PP";
    private static String N_LABEL = "N";
    @Override
    public double evaluate(Rule rule){
        Pair parentState = (Pair) rule.getParent();
        if (parentState.left.equals(N_LABEL)) {
            for (Object child : rule.getChildren()) {
                String label = this.getLabelFor((Pair) child);
                if (label.equals(PP_LABEL)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    private String getLabelFor(Pair state) {
        if (state.left instanceof Pair) {
            return this.getLabelFor((Pair) state.left);
        }
        return (String) state.left;
    }
}
