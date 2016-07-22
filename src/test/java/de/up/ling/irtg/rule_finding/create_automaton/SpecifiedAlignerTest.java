/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SpecifiedAlignerTest {

    /**
     *
     */
    private static final String TEST_AUTOMATON =
            "'3-4' -> d [1.0]\n"
            + "'1-2' -> b [1.0]\n"
            + "'0-1' -> a [1.0]\n"
            + "'4-5' -> e [1.0]\n"
            + "'2-3' -> c [1.0]\n"
            + "'0-4' -> *('0-1', '1-4') [1.0]\n"
            + "'0-3' -> *('0-1', '1-3') [1.0]\n"
            + "'0-2' -> *('0-1', '1-2') [1.0]\n"
            + "'0-5'! -> *('0-1', '1-5') [1.0]\n"
            + "'0-4' -> *('0-3', '3-4') [1.0]\n"
            + "'0-5'! -> *('0-3', '3-5') [1.0]\n"
            + "'0-4' -> *('0-2', '2-4') [1.0]\n"
            + "'0-3' -> *('0-2', '2-3') [1.0]\n"
            + "'0-5'! -> *('0-2', '2-5') [1.0]\n"
            + "'3-5' -> *('3-4', '4-5') [1.0]\n"
            + "'2-5' -> *('2-4', '4-5') [1.0]\n"
            + "'1-5' -> *('1-4', '4-5') [1.0]\n"
            + "'2-4' -> *('2-3', '3-4') [1.0]\n"
            + "'2-5' -> *('2-3', '3-5') [1.0]\n"
            + "'0-5'! -> *('0-4', '4-5') [1.0]\n"
            + "'1-4' -> *('1-3', '3-4') [1.0]\n"
            + "'1-5' -> *('1-3', '3-5') [1.0]\n"
            + "'1-4' -> *('1-2', '2-4') [1.0]\n"
            + "'1-3' -> *('1-2', '2-3') [1.0]\n"
            + "'1-5' -> *('1-2', '2-5') [1.0]";
    
    /**
     * 
     */
    private final static String ALIGNMENTS = 
            "'2-3 ||| 1 5\n"
            + "'3-4' ||| 4 9\n"
            + "'0-1' ||| 2 8";

    
    /**
     * 
     */
    private SpecifiedAligner spac;
    
    /**
     *
     */
    private TreeAutomaton<String> t;

    @Before
    public void setUp() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_AUTOMATON.getBytes());
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        
        t = taic.read(bais);
        
        bais = new ByteArrayInputStream(ALIGNMENTS.getBytes());
        spac = new SpecifiedAligner(t, bais);
    }

    /**
     * Test of getAlignmentMarkers method, of class SpecifiedAligner.
     */
    @Test
    public void testGetAlignmentMarkers() {
        String state;
        Iterable<Rule> rs = t.getRulesBottomUp(t.getSignature().getIdForSymbol("d"),new int[] {});
        Rule r = rs.iterator().next();
        
        state = this.t.getStateForId(r.getParent());
        
        
        IntSet is1 = this.spac.evaluateRule(r);
        assertEquals(is1.size(),2);
        assertTrue(is1.contains(4));
        assertTrue(is1.contains(9));
        
        IntSet is2 = this.spac.getAlignmentMarkers(state);
        
        assertEquals(is1,is2);
        
        IntSet is3 = this.spac.getAlignmentMarkers("'1-2'");
        assertTrue(is3.isEmpty());
        
        IntSet is = new IntOpenHashSet();
        
        is.add(2);
        assertFalse(spac.containsVarSet(is));
        
        is.add(8);
        assertTrue(spac.containsVarSet(is));
    }
}
