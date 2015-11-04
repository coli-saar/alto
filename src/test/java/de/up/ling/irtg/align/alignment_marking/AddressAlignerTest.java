/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.alignment_marking;

import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class AddressAlignerTest {
    
    /**
     * 
     */
    private TreeAutomaton<String> decomp;
    
    /**
     * 
     */
    private AddressAligner aa;
    
    @Before
    public void setUp() throws ParserException {
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        
        decomp = mta.decompose(mta.parseString("answer(A,and(size(B,A),const(B,stateid(texas))))"));
        aa = new AddressAligner(decomp, "asdj0-0-0:5 \t0-2-0-1-1:7------");
    }

    /**
     * Test of getAlignmentMarkers method, of class AddressAligner.
     */
    @Test
    public void testGetAlignmentMarkers() {
        String s = decomp.getStateForId(decomp.getRulesBottomUp(decomp.getSignature().getIdForSymbol("answer"), new int[] {}).iterator().next().getParent());
        
        IntSet set = aa.getAlignmentMarkers(s);
        assertEquals(set.size(),1);
        assertTrue(set.contains(5));
        
        IntIterator iit = decomp.getAllStates().iterator();
        
        boolean seenFirst = false;
        boolean seenSecond = false;
        
        while(iit.hasNext()){
            String state = decomp.getStateForId(iit.nextInt());
            switch(state){
                case "0-0-0":
                    seenFirst = true;
                    set = aa.getAlignmentMarkers(state);
                    assertEquals(set.size(),1);
                    assertTrue(set.contains(5));
                    break;
                case "0-2-0-1-1":
                    seenSecond = true;
                    set = aa.getAlignmentMarkers(state);
                    assertEquals(set.size(),1);
                    assertTrue(set.contains(7));
                    break;
                default:
                    set = aa.getAlignmentMarkers(state);
                    assertTrue(set.isEmpty());
            }
        }
        
        assertTrue(seenFirst);
        assertTrue(seenSecond);
    }
}