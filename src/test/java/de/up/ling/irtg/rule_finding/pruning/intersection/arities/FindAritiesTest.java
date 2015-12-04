/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection.arities;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class FindAritiesTest {
    /**
     * 
     */
    private static final String TEST_INPUT = ""
            + "\n"
            + "\n"
            + "iiiiisdfsf\n"
            + "fsdll\n"
            + "a(b,c)\n"
            + "\n\n\n"
            + "uuuuu\n"
            + "zsdz\n"
            + "i(a(b,b,b))\n"
            + "fkdkjldfsslkj\n"
            + "\n"
            + "kkkkkd\n\n\n"
            + "iop\n"
            + "zus\n"
            + "t(c(c,c))\n"
            + "\n";

    /**
     * Test of find method, of class FindArities.
     * @throws java.lang.Exception
     */
    @Test
    public void testFind() throws Exception {
        Object2ObjectMap<String,IntSet> map;
        try (InputStream in = new ByteArrayInputStream(TEST_INPUT.getBytes())) {
            map = FindArities.find(in, 2);
        }
        
        assertEquals(map.size(),5);
        
        IntSet ins = map.get("c");
        assertEquals(ins.size(),2);
        assertTrue(ins.contains(0));
        assertTrue(ins.contains(2));
        
        ins = map.get("a");
        assertEquals(ins.size(),2);
        assertTrue(ins.contains(3));
        assertTrue(ins.contains(2));
        
        ins = map.get("t");
        assertEquals(ins.size(),1);
        assertTrue(ins.contains(1));
        
        ins = map.get("b");
        assertEquals(ins.size(),1);
        assertTrue(ins.contains(0));
        
        ins = map.get("i");
        assertEquals(ins.size(),1);
        assertTrue(ins.contains(1));
    }
}
