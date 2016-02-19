/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import org.junit.Before;
import org.junit.Test;
import static de.up.ling.irtg.util.TestingTools.pt;
import static de.up.ling.irtg.util.TestingTools.pa;
import static de.up.ling.irtg.util.TestingTools.automatonFromItsString;
import java.io.IOException;
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
        String s = Variables.createVariable("XXX");
        Tree<String> t1 = pt("__X__(?1)");
        Tree<String> t2 = pt("__X__(?1)");
        
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

    /**
     * 
     */
    private final static String V_AUTOMATON = "false -> 'k / g' [1.0]\n"+
            "true! -> 'z / x1'(false) [1.0]\n"+
            "false -> 'z / x1'(false) [1.0]\n"+
            "false -> 'x1 / g'(false) [1.0]\n"+
            "true! -> 'x1 / g'(false) [1.0]\n"+
            "false -> 'x1 / m(x1,x2)'(false, false) [1.0]\n"+
            "true! -> 'x1 / m(x1,x2)'(false, false) [1.0]\n"+
            "false -> 'm(x1,x2) / x2'(false, false) [1.0]\n"+
            "true! -> 'm(x1,x2) / x2'(false, false) [1.0]\n"+
            "false -> 'm(x1,x2) / x3'(false, false, false) [1.0]\n"+
            "true! -> 'm(x1,x2) / x3'(false, false, false) [1.0]\n"+
            "true! -> 'x3 / m(x1,x2)'(false, false, false) [1.0]\n"+
            "false -> 'x3 / m(x1,x2)'(false, false, false) [1.0]\n"+
            "false -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, false, false, false, false) [1.0]\n"+
            "true! -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, false, false, false, false) [1.0]\n"+
            "false -> '__X__{XXX}'(true) [1.0]";
    
    /**
     * Test of getVariableSequenceing method, of class RestrictionManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetVariableSequenceing() throws Exception {
        TreeAutomaton t = rm.getVariableSequenceing();
        TreeAutomaton correct = pa(V_AUTOMATON);
        
        assertFalse(t.accepts(pt("'__X__{XXX}'('__X__{XXX}'('k / g'))")));
        assertFalse(t.accepts(pt("'__X__{XXX}'('x1 / g'('__X__{XXX}'('__X__{XXX}'('k / g'))))")));
        
        assertTrue(t.accepts(pt("'x3 / m(x1,x2)'('__X__{XXX}'('x1 / g'('__X__{XXX}'('x1 / g'('k / g')))),'k / g','k / g')")));
        assertFalse(t.accepts(pt("'x3 / m(x1,x2)'('__X__{XXX}'('__X__{XXX}'('x1 / g'('__X__{XXX}'('k / g')))),'k / g','k / g')")));
        
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
                                                "false! -> '__X__{XXX}'(false) [1.0]\n" +
                                                "true -> '__X__{XXX}'(false) [1.0]\n" +
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

    private static final String T_AUTOMATON = "BOTH_TRUE -> 'k / g' [1.0]\n"+
            "RIGHT_TRUE -> 'z / x1'(BOTH_TRUE) [1.0]\n"+
            "LEFT_TRUE -> 'x1 / g'(BOTH_TRUE) [1.0]\n"+
            "BOTH_FALSE! -> 'z / x1'(LEFT_TRUE) [1.0]\n"+
            "LEFT_TRUE -> 'x1 / m(x1,x2)'(LEFT_TRUE, LEFT_TRUE) [1.0]\n"+
            "LEFT_TRUE -> 'x3 / m(x1,x2)'(LEFT_TRUE, LEFT_TRUE, BOTH_TRUE) [1.0]\n"+
            "BOTH_FALSE! -> 'x3 / m(x1,x2)'(LEFT_TRUE, LEFT_TRUE, RIGHT_TRUE) [1.0]\n"+
            "BOTH_FALSE! -> '__X__{XXX}'(BOTH_FALSE) [1.0]\n"+
            "BOTH_FALSE! -> 'x1 / m(x1,x2)'(BOTH_FALSE, LEFT_TRUE) [1.0]\n"+
            "BOTH_FALSE! -> 'x1 / g'(RIGHT_TRUE) [1.0]\n"+
            "BOTH_FALSE! -> 'm(x1,x2) / x2'(RIGHT_TRUE, BOTH_FALSE) [1.0]\n"+
            "BOTH_FALSE! -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(RIGHT_TRUE, BOTH_FALSE, RIGHT_TRUE, BOTH_FALSE, LEFT_TRUE) [1.0]\n"+
            "RIGHT_TRUE -> 'm(x1,x2) / x2'(RIGHT_TRUE, RIGHT_TRUE) [1.0]\n"+
            "RIGHT_TRUE -> 'm(x1,x2) / x3'(RIGHT_TRUE, RIGHT_TRUE, BOTH_TRUE) [1.0]\n"+
            "BOTH_FALSE! -> 'm(x1,x2) / x3'(RIGHT_TRUE, RIGHT_TRUE, LEFT_TRUE) [1.0]";
    
    /**
     * Test of getTermination method, of class RestrictionManager.
     * @throws java.io.IOException
     */
    @Test
    public void testGetTermination() throws IOException {
        TreeAutomaton ta = rm.getTermination();
        
        assertEquals(automatonFromItsString(ta),pa(T_AUTOMATON));
    }

    
    private final static String S_AUTOMATON = "false! -> 'k / g' [1.0]\n"+
            "true -> 'x1 / m(x1,x2)'(true, false) [1.0]\n"+
            "false! -> 'x1 / m(x1,x2)'(true, false) [1.0]\n"+
            "false! -> 'z / x1'(false) [1.0]\n"+
            "false! -> '__X__{XXX}'(false) [1.0]\n"+
            "true -> '__X__{XXX}'(false) [1.0]\n"+
            "false! -> 'x1 / g'(false) [1.0]\n"+
            "false! -> 'm(x1,x2) / x2'(false, true) [1.0]\n"+
            "true -> 'm(x1,x2) / x2'(false, true) [1.0]\n"+
            "false! -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, true, false, true, false) [1.0]\n"+
            "true -> 't(x1,x2,x3,x4) / l(x2,x4,x5)'(false, true, false, true, false) [1.0]\n"+
            "false! -> 'm(x1,x2) / x3'(false, false, false) [1.0]\n"+
            "false! -> 'x3 / m(x1,x2)'(false, false, false) [1.0]";
    
    /**
     * Test of getSplitOrderedPairing method, of class RestrictionManager.
     * @throws java.io.IOException
     */
    @Test
    public void testGetSplitOrderedPairing() throws IOException {
        TreeAutomaton ta = rm.getSplitOrderedPairing();
        
        assertEquals(automatonFromItsString(ta),pa(S_AUTOMATON));
    }

    /**
     * Test of getRestriction method, of class RestrictionManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRestriction() throws Exception {
        TreeAutomaton cta = rm.getRestriction();
        
        Iterable<Rule> rs = cta.getAllRulesTopDown();
        for(Rule r : rs){
            r.setWeight(1);
        }
        
        assertTrue(cta.accepts(pt("'z / x1'('x1 / g'('k / g'))")));
        
        String s = "'x1 / m(x1,x2)'('z / x1'('x1 / g'('k / g')),'x1 / g'('k / g'))";
        assertFalse(cta.accepts(pt(s)));
        
        s = "'x1 / m(x1,x2)'('__X__{XXX}'('z / x1'('x1 / g'('k / g'))),'x1 / g'('k / g'))";
        assertTrue(cta.accepts(pt(s)));
        
        s = "'x1 / m(x1,x2)'('__X__{XXX}'('z / x1'('x1 / g'('k / g'))),'z / x1'('x1 / g'('k / g')))";
        assertFalse(cta.accepts(pt(s)));
        
        TreeAutomaton t = rm.getVariableSequenceing().intersect(rm.getOrdering())
                    .intersect(rm.getTermination()).intersect(rm.getSplitOrderedPairing());
        TreeAutomaton con = automatonFromItsString(t);
        cta = automatonFromItsString(cta);
        
        assertEquals(con,cta);
    }
}