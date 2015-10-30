/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules.SignificanceMeasures;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class WeightedPMIDifferenceTest {
    
    /**
     * Test of getPairSignificance method, of class WeightedPMIDifference.
     */
    @Test
    public void testGetPairSignificance() {
        WeightedPMIDifference wpd = new WeightedPMIDifference();
        
        double score = wpd.getPairSignificance(10.0, 10.0, 5.0, 5.0, 20.0);
        assertEquals(score,0.0,0.000000001);

        score = wpd.getPairSignificance(2.0, 4.0, 1.0, 0.0, 30.0);
        assertEquals(score,0.044058527999410645,0.00000000001);
        
        score = wpd.getPairSignificance(13.0, 14.0, 1.0, 12.0, 30.0);
        assertTrue(score < 0.0);
    }
}
