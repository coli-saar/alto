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
        /*
        assertEquals(rate.getLogLearningRate(0, 2, 0.0),0.5,0.000001);
        assertEquals(rate.getLogLearningRate(0, 2, 5.0),0.1,0.000001);
        assertEquals(rate.getLogLearningRate(1, 2, 2.0),0.25,0.000001);
        assertEquals(rate.getLogLearningRate(0, 2, 10.0),(1/Math.sqrt(125.0))*0.5,0.000001);
        assertEquals(rate.getLogLearningRate(1, 2, 2.0),0.17677669529663687,0.000001);
        
        rate.reset();
        
        assertEquals(rate.getLogLearningRate(0, 2, 10.0),0.05,0.000001);
        assertEquals(rate.getLogLearningRate(0, 2, 20.0),0.0223,0.001);
                */
    }
    
}
