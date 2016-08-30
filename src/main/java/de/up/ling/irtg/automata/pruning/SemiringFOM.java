/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.semiring.DoubleArithmeticSemiring;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

/**
 *
 * @author koller
 */
public class SemiringFOM implements FOM {
    private final Semiring<Double> semiring;
    private final Long2DoubleMap statesToEvaluation;

    public SemiringFOM(Semiring<Double> semiring) {
        this.semiring = semiring;
        statesToEvaluation = new Long2DoubleOpenHashMap();
        statesToEvaluation.defaultReturnValue(Double.NaN);
    }
    
    @Override
    public double evaluate(Rule left, CondensedRule right) {
        double ruleVal = semiring.multiply(left.getWeight(), right.getWeight());
        
//        System.err.println("[eval] initial ruleVal " + ruleVal);
//        System.err.println("[eval] left arity " + left.getArity());
//        System.err.println("[eval] left  " + left);
        
        for( int i = 0; i < left.getArity(); i++ ) {
            double childVal = evaluateStates(left.getChildren()[i], right.getChildren()[i]);
            
            if( Double.isNaN(childVal) ) {
                // child state pair not previously evaluated => violates invariant => return NaN
                return Double.NaN;
            }
            
            ruleVal = semiring.multiply(ruleVal, childVal);
        }
        
        double parentStateVal = evaluateStates(left.getParent(), right.getParent());
        double newParentStateVal = Double.isNaN(parentStateVal) ? ruleVal : semiring.add(ruleVal, parentStateVal);
        
        setStatesEvaluation(left.getParent(), right.getParent(), newParentStateVal);
        
        return ruleVal;
    }
    
    @Override
    public double evaluateStates(int leftState, int rightState) {
        long encoding = NumbersCombine.combine(leftState, rightState);
        return statesToEvaluation.get(encoding);
    }
    
    private void setStatesEvaluation(int p, int q, double newValue) {
        long encoding = NumbersCombine.combine(p, q);
        statesToEvaluation.put(encoding, newValue);
    }
    
    @OperationAnnotation(code = "insideFom")
    public static FOM createInsideFom() {
        return new SemiringFOM(new DoubleArithmeticSemiring());
    }
}
