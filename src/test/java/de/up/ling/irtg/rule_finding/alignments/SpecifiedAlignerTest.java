/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.alignments;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
    private SpecifiedAligner<Span> align;
    
    /**
     * 
     */
    private TreeAutomaton<Span> t;
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        
        t = sal.decompose(sal.parseString("a b c d e"));
        
        align = new SpecifiedAligner<>(t);
        
        t.getAllStates().stream().map((i) -> t.getStateForId(i)).forEach((s) -> {
            int l = s.end-s.start;
            if (!(l > 2)) {
                int marker = (l*100) + s.start;
                IntSet is = new IntRBTreeSet();
                is.add(marker);
                
                align.put(s, is);
            }
        });
    }

    /**
     * Test of getAlignmentMarkers method, of class SpecifiedAligner.
     */
    @Test
    public void testGetAlignmentMarkers() {
        t.getAllStates().stream().map((i) -> t.getStateForId(i)).forEach((s) -> {
            int l = s.end-s.start;
            
            int marker = (l*100) + s.start;
            
            if(l > 2){
                assertTrue(this.align.getAlignmentMarkers(s).isEmpty());
            }else{
                assertEquals(this.align.getAlignmentMarkers(s).size(),1);
                assertTrue(this.align.getAlignmentMarkers(s).contains(marker));
            }
        });
        
        IntSet lookUp = new IntOpenHashSet();
        lookUp.add(101);
        
        assertTrue(this.align.containsVarSet(lookUp));
        lookUp.clear();
        assertTrue(this.align.containsVarSet(lookUp));
        lookUp.add(200);
        assertTrue(this.align.containsVarSet(lookUp));
        lookUp.add(19);
        assertFalse(this.align.containsVarSet(lookUp));
        lookUp.clear();
        lookUp.add(110);
        assertFalse(this.align.containsVarSet(lookUp));
    }
}