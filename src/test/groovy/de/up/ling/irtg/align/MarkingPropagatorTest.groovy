/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.TreeAutomaton;
import org.junit.Before;
import org.junit.Test;
import de.up.ling.irtg.align.*;
import de.up.ling.irtg.align.alignment_algebras.StringAlignmentAlgebra;
import static de.up.ling.irtg.util.TestingTools.*;
import static org.junit.Assert.*;
import de.saar.basic.Pair

/**
 *
 * @author teichmann
 */
public class MarkingPropagatorTest {
    
    /**
     * 
     */
    private RuleMarker rm;
    
    /**
     * 
     */
    private TreeAutomaton ta1;
    
    /**
     * 
     */
    private TreeAutomaton ta2;
    
    /**
     * 
     */
    private MarkingPropagator mp;
    
    @Before
    public void setUp() {
        StringAlignmentAlgebra saa = new StringAlignmentAlgebra();
        
        String one = "a:17 b:3 c:4 d:2";
        String two = "c:4 d:2 a:17 b:3 h";
        
        Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> result = saa.decomposePair(one, two);
        rm = result.getLeft();
       
        ta1 = result.getRight().getLeft();
        ta2 = result.getRight().getRight();
        
        this.mp = new MarkingPropagator();
    }

    /**
     * Test of introduce method, of class MarkingPropagator.
     */
    @Test
    public void testIntroduce() {
        TreeAutomaton mta1 = this.mp.introduce(ta1,rm,0);
        TreeAutomaton mta2 = this.mp.introduce(ta2,rm,1);
        
        assertTrue(mta1.accepts(pt("'X_{0, 1, 2, 3}'(*(a,*(*(b,c),d)))")));
        assertTrue(mta1.accepts(pt("'X_{0, 1, 2, 3}'(*(a,'X_{0, 1, 2}'(*(*(b,c),d))))")));
        assertTrue(mta1.accepts(pt("'X_{0, 1, 2, 3}'(*('X_{3}'(a),'X_{0, 1, 2}'(*(*(b,c),d))))")));
        assertTrue(mta1.accepts(pt("'X_{0, 1, 2, 3}'(*('X_{3}'('X_{3}'(a)),'X_{0, 1, 2}'(*(*(b,c),d))))")));
        
        assertTrue(mta1.accepts(pt("*(*(a,b),*(c,d))")));
        assertTrue(mta1.accepts(pt("*('X_{2, 3}'(*(a,b)),*(c,d))")));
        assertTrue(mta1.accepts(pt("*('X_{2, 3}'(*(a,b)),'X_{0, 1}'(*(c,d)))")));
        
        assertTrue(mta2.accepts(pt("*(*(c,d),*(*(a,b),h))")));
        assertTrue(mta2.accepts(pt("*(*('X_{1}'(c),d),*(*(a,b),h))")));
        assertTrue(mta2.accepts(pt("*(*('X_{1}'(c),d),*(*(a,b),'X_{}'(h)))")));
        assertTrue(mta2.accepts(pt("*(*('X_{1}'(c),d),'X_{2, 3}'(*(*(a,b),'X_{}'(h))))")));
        assertTrue(mta2.accepts(pt("*(*('X_{1}'(c),d),'X_{2, 3}'(*(*(a,b),'X_{}'('X_{}'(h)))))")));
    }
    
}
