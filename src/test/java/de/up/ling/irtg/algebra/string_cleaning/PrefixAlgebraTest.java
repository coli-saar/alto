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
public class PrefixAlgebraTest {
    /**
     * 
     */
    private PrefixAlgebra pal;
    
    @Before
    public void setUp() {
        pal = new PrefixAlgebra(2);
    }

    /**
     * Test of parseString method, of class PrefixAlgebra.
     */
    @Test
    public void testParseString() {
        List<String> result = pal.parseString("Dies ist ein einfaches Beispiel ...");
        String[] parts = new String[] {"di", "is", "ei", "ei", "be"};
        
        assertEquals(parts.length,result.size());
        for(int i=0;i<parts.length;++i) {
            assertEquals(parts[i],result.get(i));
        }
    }
}
