/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class RuleTest {
    /**
     * 
     */
    private Rule r1;
    
    /**
     * 
     */
    private Rule r2;
    
    /**
     * 
     */
    private Rule r3;
    
    /**
     * 
     */
    private Rule r4;
    
    /**
     * 
     */
    private Rule r5;
    
    /**
     * 
     */
    private Rule r6;
    
    @Before
    public void setUp() {
        TreeAutomaton<Integer> ta = new ConcreteTreeAutomaton<>();
        
        r1 = ta.createRule(0, "a", new Integer[] {2,2});
        r2 = ta.createRule(0, "a", new Integer[] {0,2});
        r3 = ta.createRule(0, "b", new Integer[] {2,2});
        r4 = ta.createRule(1, "a", new Integer[] {2,2});
        r5 = ta.createRule(0, "a", new Integer[] {2,2});
        r6 = ta.createRule(0, "c", new Integer[] {2,2,2});
    }

    /**
     * Test of compareTo method, of class Rule.
     */
    @Test
    public void testCompareTo() {
        int comp = r1.compareTo(r1);
        assertEquals(comp,0);
        assertEquals(r1,r1);
        
        comp = r1.compareTo(r5);
        assertEquals(comp,0);
        assertEquals(r1,r5);
        
        comp = r5.compareTo(r1);
        assertEquals(comp,0);
        assertEquals(r5,r1);
        
        comp = r1.compareTo(r2);
        assertEquals(comp,1);
        assertFalse(r1.equals(r2));

        comp = r2.compareTo(r1);
        assertEquals(comp,-1);
        assertFalse(r2.equals(r1));
        
        comp = r1.compareTo(r3);
        assertEquals(comp,-1);
        assertFalse(r1.equals(r3));
        
        comp = r3.compareTo(r1);
        assertEquals(comp,1);
        assertFalse(r3.equals(r1));
        
        comp = r3.compareTo(r3);
        assertEquals(comp,0);
        assertEquals(r3,r3);
        
        comp = r3.compareTo(r6);
        assertEquals(comp,-1);
        assertFalse(r3.equals(r6));
        
        comp = r6.compareTo(r3);
        assertEquals(comp,1);
        assertFalse(r6.equals(r3));
        
        comp = r1.compareTo(r4);
        assertEquals(comp,-1);
        assertFalse(r1.equals(r4));
        
        comp = r4.compareTo(r1);
        assertEquals(comp,1);
        assertFalse(r4.equals(r1));
        // TODO review the generated test code and remove the default call to fail.
    }
    
}
