/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.string_cleaning;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class DownNoPunctAlgebraTest {
    
    /**
     * 
     */
    private DownNoPunctAlgebra dpa;
    
    @Before
    public void setUp() {
        dpa = new DownNoPunctAlgebra();
    }

    /**
     * Test of parseString method, of class DownNoPunctAlgebra.
     */
    @Test
    public void testParseString() {
        List<String> tokens = dpa.parseString("Dies ist ein Beispiel für downcasing und Interpunktionsentfernung.");
        String[] arr = new String[] {"dies", "ist", "ein", "beispiel", "für", "downcasing", "und", "interpunktionsentfernung"};
        assertEquals(arr.length,tokens.size());
        
        for(int i=0;i<arr.length;++i) {
            assertEquals(arr[i],tokens.get(i));
        }
    }
}
