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
        String s = Variables.makeVariable(k);
        assertEquals(s,"X"+k);
        assertEquals(Variables.getInformation(s),k);
    }
}
