/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.learning_rates;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class AdaGradTest {
    /**
     * 
     */
    private AdaGrad rate;
    
    @Before
    public void setUp() {
        rate = new AdaGrad();
    }

    /**
     * Test of getLearningRate method, of class AdaGrad.
     */
    @Test
    public void testGetLearningRate() {
        assertEquals(rate.getLearningRate(0, 2, 5.0),5.0,0.000001);
        assertEquals(rate.getLearningRate(1, 2, 2.0),2.0,0.000001);
        assertEquals(rate.getLearningRate(0, 2, 10.0),Math.sqrt(125.0),0.000001);
        assertEquals(rate.getLearningRate(1, 2, 2.0),Math.sqrt(8.0),0.000001);
        
        rate.reset();
        
        assertEquals(rate.getLearningRate(0, 2, 10.0),10.0,0.000001);
        assertEquals(rate.getLearningRate(0, 2, 20.0),Math.sqrt(500),0.000001);
    }
    
}
