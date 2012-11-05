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
public class DetFeatureFunction implements FeatureFunction<String> {
    private static final double DEFAULT_VALUE = 0.5;
    public DetFeatureFunction(){
    }

    @Override
    public double evaluate(Rule<String> object){
        String label = object.getLabel();
        if(label.equals("r1")){
            return 0.1;
        }else if(label.equals("r2")){
            return 0.2;
        }else if(label.equals("r3")){
            return 0.7;
        }else if(label.equals("r4")){
            return 0.3;
        }else if(label.equals("r5")){
            return 0.08;
        }else if(label.equals("r6")){
            return 0.24;
        }else if(label.equals("r7")){
            return 0.8;
        }else if(label.equals("r8")){
            return 0.54;
        }else if(label.equals("r9")){
            return 0.66;
        }else{
            return DEFAULT_VALUE;
        }
    }
    
}
