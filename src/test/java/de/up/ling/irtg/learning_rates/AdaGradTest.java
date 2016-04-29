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
        double rate = this.rate.getLearningRate(0, 1, 3.0);
        assertEquals(rate,0.5 / 3.0, 0.000000001);
        
        rate = this.rate.getLearningRate(0, 1, 1.0);
        assertEquals(rate,0.5 / Math.sqrt(10.0),0.0000001);
        
        rate = this.rate.getLearningRate(1, 4, -3.0);
        assertEquals(rate,0.5 / 3.0, 0.000000001);
        
        rate = this.rate.getLearningRate(1, 4, -1.0);
        assertEquals(rate,0.5 / Math.sqrt(10.0),0.0000001);
        
        rate = this.rate.getLearningRate(1, 4, 4.0);
        assertEquals(rate,0.5 / Math.sqrt(26.0),0.0000001);
        
        this.rate.reset();
        
        rate = this.rate.getLearningRate(0, 1, 3.0);
        assertEquals(rate,0.5 / 3.0, 0.000000001);
        
        rate = this.rate.getLearningRate(0, 1, 1.0);
        assertEquals(rate,0.5 / Math.sqrt(10.0),0.0000001);
        
        rate = this.rate.getLearningRate(1, 4, -3.0);
        assertEquals(rate,0.5 / 3.0, 0.000000001);
        
        rate = this.rate.getLearningRate(1, 4, -1.0);
        assertEquals(rate,0.5 / Math.sqrt(10.0),0.0000001);
        
        rate = this.rate.getLearningRate(1, 4, 4.0);
        assertEquals(rate,0.5 / Math.sqrt(26.0),0.0000001);
    }
    
}
