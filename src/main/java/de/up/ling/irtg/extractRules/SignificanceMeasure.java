/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules;

/**
 *
 * @author christoph_teichmann
 */
public interface SignificanceMeasure {
    
    /**
     * 
     * @param leftCount
     * @param rightCount
     * @param pairCount
     * @param reversePairCount
     * @param allPairsCount
     * @return 
     */
    public double getPairSignificance(double leftCount, double rightCount, double pairCount,
                                        double reversePairCount, double allPairsCount);
}
