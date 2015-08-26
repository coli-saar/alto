/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.alignment_marking;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.TreeAutomaton;
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
        
        for(Integer i : t.getAllStates()){
            Span s = t.getStateForId(i);
            int l = s.end-s.start;
            
            if(l > 2){
                continue;
            }
            
            int marker = (l*100) + s.start;
            IntSet is = new IntRBTreeSet();
            is.add(marker);
            
            align.put(s, is);
        }
    }

    /**
     * Test of getAlignmentMarkers method, of class SpecifiedAligner.
     */
    @Test
    public void testGetAlignmentMarkers() {
        for(Integer i : t.getAllStates()){
            Span s = t.getStateForId(i);
            int l = s.end-s.start;
            
            int marker = (l*100) + s.start;
            
            if(l > 2){
                assertTrue(this.align.getAlignmentMarkers(s).isEmpty());
            }else{
                assertEquals(this.align.getAlignmentMarkers(s).size(),1);
                assertTrue(this.align.getAlignmentMarkers(s).contains(marker));
            }
        }
    }
}