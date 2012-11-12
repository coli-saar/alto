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
public class AlignPPtoNFeature implements FeatureFunction {
    private static String PP_LABEL = "PP";
    private static String N_LABEL = "N";
    @Override
    public double evaluate(Rule rule){
        if(rule.getParent().equals(N_LABEL) && (rule.getChildren().length > 0)){
            for(Object child : rule.getChildren()){
                if(child.equals(PP_LABEL)){
                    return 1.0;
                }
            }
        }
        return 0.0;
    }
}
