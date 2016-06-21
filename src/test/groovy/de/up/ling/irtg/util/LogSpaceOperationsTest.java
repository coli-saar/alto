/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class LogSpaceOperationsTest {
    /**
     * Test of addAlmostZero method, of class LogSpaceOperations.
     */
    @Test
    public void testAdd() {
        assertEquals(LogSpaceOperations.add(Double.NEGATIVE_INFINITY, Math.log(0.1)),Math.log(0.1),0.00000001);
        assertEquals(LogSpaceOperations.add(Double.NEGATIVE_INFINITY, Math.log(0.001)),Math.log(0.001),0.00000001);
        
        assertEquals(LogSpaceOperations.add(Math.log(0.00000003),
                Math.log(0.0000000001)),Math.log(0.00000003+0.0000000001),0.00000000000001);
        
        assertEquals(LogSpaceOperations.add(Math.log(3),
                Math.log(1)),Math.log(3+1),0.0000001);
        
        assertEquals(LogSpaceOperations.add(Math.log(600),
                Math.log(50)),Math.log(600+50),0.0000001);
        
        assertEquals(LogSpaceOperations.add(Math.log(1000000),
                Math.log(0.0000001)),Math.log(1000000+0.0000001),0.0000001);
        
        assertEquals(LogSpaceOperations.add(0.0, -4),Math.log1p(Math.exp(-4)),0.0000001);
    }   
}