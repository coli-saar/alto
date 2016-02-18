/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class VariablesTest {
    
    @Before
    public void setUp() {
    }

    /**
     * Test of makeVariable method, of class Variables.
     */
    @Test
    public void testMakeVariable() {
        String k = "usdfls";
        String s = Variables.createVariable(k);
        assertEquals(s,"__X__{"+k+"}");
        assertEquals(Variables.getInformation(s),k);
        
        assertTrue(Variables.isVariable("__X__{5}"));
        assertTrue(Variables.isVariable("__X__5"));
        assertFalse(Variables.isVariable("__X_5"));
        
        assertEquals(Variables.getInformation("__X__{5}"),"5");
    }
}
