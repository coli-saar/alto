/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules.SignificanceMeasures;

import de.up.ling.irtg.extractRules.SignificanceMeasure;

/**
 *
 * @author christoph_teichmann
 */
public class WeightedPMIDifference implements SignificanceMeasure {

    @Override
    public double getPairSignificance(double leftCount, double rightCount, double pairCount, double reversePairCount, double allPairsCount) {
        double pmi1 = makeWeightedPMI(leftCount,rightCount,pairCount,allPairsCount);
        double pmi2 = makeWeightedPMI(leftCount,rightCount,reversePairCount,allPairsCount);
        
        if(Double.isNaN(pmi1)){
            pmi1 = 0.0;
        }
        if(Double.isNaN(pmi2)){
            pmi2 = 0.0;
        }
        
        return pmi1-pmi2;
    }

    /**
     * 
     * @param leftCount
     * @param rightCount
     * @param pairCount
     * @param allPairsCount
     * @return 
     */
    private double makeWeightedPMI(double leftCount, double rightCount, double pairCount, double allPairsCount) {
        double multi = pairCount / allPairsCount;
        
        double sum = Math.log(pairCount);
        sum -= Math.log(leftCount);
        sum -= Math.log(rightCount/allPairsCount);
        
        return multi*sum;
    }
    
}
