/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import org.junit.Before;
import org.junit.Test;
import static de.up.ling.irtg.util.TestingTools.pt;
import static de.up.ling.irtg.util.TestingTools.pa;
import static de.up.ling.irtg.util.TestingTools.automatonFromItsString;
import java.io.IOException;
import java.util.Iterator;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RestrictionManagerTest {
    
    /**
     * 
     */
    private RestrictionManager rm;
    
    /**
     * 
     */
    private Signature sig;
    
    @Before
    public void setUp() throws Exception {
        rm = new RestrictionManager(this.sig = new Signature());
        String s = "XXX(x1) / XXX(x1)";
        Tree<String> t1 = pt("XXX(?1)");
        Tree<String> t2 = pt("XXX(?1)");
        
        this.sig.addSymbol(s, 1);
        rm.addSymbol(s, t1, t2);
        
        s = "k / g";
        t1 = pt("k");
        t2 = pt("g");
        
        this.sig.addSymbol(s, 0);
        rm.addSymbol(s, t1, t2);
        
        s = "x1 / g";
        t1 = pt("?1");
        t2 = pt("g");
        
        this.sig.addSymbol(s, 1);
        rm.addSymbol(s, t1, t2);
        
        s = "z / x1";
        t1 = pt("z");
        t2 = pt("?1");
        
        this.sig.addSymbol(s, 1);
        rm.addSymbol(s, t1, t2);
        
        s = "x3 / m(x1,x2)";
        t1 = pt("?3");
        t2 = pt("m(?1,?2)");
        
        this.sig.addSymbol(s, 3);
        rm.addSymbol(s, t1, t2);
        
        s = "x1 / m(x1,x2)";
        t1 = pt("?1");
        t2 = pt("m(?1,?2)");
        
        this.sig.addSymbol(s, 2);
        rm.addSymbol(s, t1, t2);
        
        s = "m(x1,x2) / x2";
        t1 = pt("m(?1,?2)");
        t2 = pt("?2");
        
        this.sig.addSymbol(s, 2);
        rm.addSymbol(s, t1, t2);
        
        s = "m(x1,x2) / x3";
        t1 = pt("m(?1,?2)");
        t2 = pt("?3");
        
        this.sig.addSymbol(s, 3);
        rm.addSymbol(s, t1, t2);
        
        s = "t(x1,x2,x3,x4) / l(x2,x4,x5)";
        t1 = pt("t(?1,?2,?3,?4)");
        t2 = pt("l(?2,?4,?5)");
        
        this.sig.addSymbol(s, 5);
        rm.addSymbol(s, t1, t2);
    }

    
    private final static String V_AUTOMATON = "false! -> 'k / g' [1.0]\n"+
                                                "true -> 'k / g' [1.0]\n"+
                                                "false! -> 'XXX(x1) / XXX(x1)'(true) [1.0]\n"+
                                                "true -> 'z / x1'(false) [1.0]\n"+
                                                "false! -> 'z / x1'(false) [1.0]\n"+
                                                "false! -> 'x1 / g'(false) [1.0]\n"+
                                                "true -> 'x1 / g'(false) [1.0]\n"+
                                                "true -> 'x1 / m(x1,x2)'(false, false) [1.0]\n"+
                                                "false! -> 'x1 / m(x1,x2)'(false, false) [1.0]\n"+
                                                "true -> 'm(x1,x2) / x2'(false, false) [1.0]\n"+
                                                "false! -> 'm(x1,x2) / x2'(false, false) [1.0]\n"+
                                                "false! -> 'm(x1,x2) / x3'(false, false, false) [1.0]\n"+
                                                "true -> 'm(x1,x2) / x3'(false, false, false) [1.0]\n"+
                                                "true -> 'x3 / m(x1,x2)'(false, false, false) [1.0]\n"+
                                                "false! -> 'x3 / m(x1,x2)'(false, false, false) [1.0]\n"+
                                                "false! -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, false, false, false, false) [1.0]\n"+
                                                "true -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, false, false, false, false) [1.0]";
    
    /**
     * Test of getVariableSequenceing method, of class RestrictionManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetVariableSequenceing() throws Exception {
        TreeAutomaton t = rm.getVariableSequenceing();
        TreeAutomaton correct = pa(V_AUTOMATON);
        
        assertTrue(t.accepts(pt("'XXX(x1) / XXX(x1)'('k / g')")));  
        assertFalse(t.accepts(pt("'XXX(x1) / XXX(x1)'('XXX(x1) / XXX(x1)'('k / g'))")));
        
        assertTrue(t.accepts(pt("'XXX(x1) / XXX(x1)'('x1 / g'('XXX(x1) / XXX(x1)'('k / g')))")));
        assertFalse(t.accepts(pt("'XXX(x1) / XXX(x1)'('x1 / g'('XXX(x1) / XXX(x1)'('XXX(x1) / XXX(x1)'('k / g'))))")));
        
        assertTrue(t.accepts(pt("'x3 / m(x1,x2)'('XXX(x1) / XXX(x1)'('x1 / g'('XXX(x1) / XXX(x1)'('k / g'))),'k / g','k / g')")));
        assertTrue(t.accepts(pt("'XXX(x1) / XXX(x1)'('x3 / m(x1,x2)'('XXX(x1) / XXX(x1)'('x1 / g'('XXX(x1) / XXX(x1)'('k / g'))),'k / g','k / g'))")));
        assertFalse(t.accepts(pt("'XXX(x1) / XXX(x1)'('x3 / m(x1,x2)'('XXX(x1) / XXX(x1)'('XXX(x1) / XXX(x1)'('x1 / g'('XXX(x1) / XXX(x1)'('k / g')))),'k / g','k / g'))")));
        
        assertEquals(automatonFromItsString(t),correct);
    }

    
    private final static String O_AUTOMATON = "false! -> 'k / g' [1.0]\n" +
                                                "true -> 'k / g' [1.0]\n" +
                                                "true -> 'x1 / g'(true) [1.0]\n" +
                                                "false! -> 'x1 / g'(true) [1.0]\n" +
                                                "true -> 'x1 / m(x1,x2)'(true, true) [1.0]\n" +
                                                "false! -> 'x1 / m(x1,x2)'(true, true) [1.0]\n" +
                                                "false! -> 'x3 / m(x1,x2)'(true, true, true) [1.0]\n" +
                                                "true -> 'x3 / m(x1,x2)'(true, true, true) [1.0]\n" +
                                                "false! -> 'z / x1'(false) [1.0]\n" +
                                                "false! -> 'XXX(x1) / XXX(x1)'(false) [1.0]\n" +
                                                "true -> 'XXX(x1) / XXX(x1)'(false) [1.0]\n" +
                                                "false! -> 'm(x1,x2) / x2'(false, false) [1.0]\n" +
                                                "false! -> 'm(x1,x2) / x3'(false, false, false) [1.0]\n" +
                                                "false! -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, false, false, false, false) [1.0]\n" +
                                                "true -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, false, false, false, false) [1.0]";
    
    /**
     * Test of getOrdering method, of class RestrictionManager.
     * @throws java.io.IOException
     */
    @Test
    public void testGetOrdering() throws IOException {
        TreeAutomaton ta = rm.getOrdering();
        TreeAutomaton correct = pa(O_AUTOMATON);
        
        assertEquals(automatonFromItsString(ta),correct);
    }

    /**
     * Test of getTermination method, of class RestrictionManager.
     */
    @Test
    public void testGetTermination() {
        TreeAutomaton ta = rm.getTermination();
        
        System.out.println(ta);
        //TODO
    }

    /**
     * Test of getSplitOrderedPairing method, of class RestrictionManager.
     */
    @Test
    public void testGetSplitOrderedPairing() {
        //TODO
    }

    /**
     * Test of getRestriction method, of class RestrictionManager.
     */
    @Test
    public void testGetRestriction() {
        //TODO
    }
}