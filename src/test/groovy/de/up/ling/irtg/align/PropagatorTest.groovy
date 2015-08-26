/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class PropagatorTest {
    
    /**
     * 
     */
    private TreeAutomaton<Span> decomp;
    
    /**
     * 
     */
    private SpanAligner spa;
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        decomp = sal.decompose(sal.parseString("This is a test case ."));
        
        spa = new SpanAligner("1:2:14 0:6:44 5:6:2", decomp);
    }

    /**
     * Test of propagate method, of class Propagator.
     */
    @Test
    public void testPropagate() throws Exception {
        Propagator prop = new Propagator();
        
        TreeAutomaton<Span> t = prop.convert(decomp, spa);
        
        assertTrue(t.accepts(pt("*(*('XX_{}'(This),'XX_{14}'(*('XX_{14}'('XX_{14}'('XX_{14}'(is))),'XX_{}'(a)))),*(*(test,'XX_{}'(case)),'.'))")));
        assertFalse(t.accepts(pt("*(*('XX_{12}'(This),'XX_{14}'(*('XX_{14}'('XX_{14}'('XX_{14}'(is))),'XX_{}'(a)))),*(*(test,'XX_{}'(case)),'.'))")));
        
        assertTrue(t.accepts(pt("*('XX_{14}'(*(This,is)),'XX_{2}'(*('XX_{}'('XX_{}'(*(a,*(test,'XX_{}'(case))))),'XX_{2}'('.'))))")));
        assertFalse(t.accepts(pt("*('XX_{44}'(*(This,is)),'XX_{2}'(*('XX_{}'('XX_{}'(*(a,*(test,'XX_{}'(case))))),'XX_{2}'('.'))))")));
        
        assertTrue(t.accepts(pt("'XX_{2, 14, 44}'(*(*(*(*('XX_{}'('XX_{}'('XX_{}'('XX_{}'('XX_{}'(This))))),is),'XX_{}'(a)),*('XX_{}'(test),'XX_{}'(case))),'XX_{2}'('.')))")));
        assertFalse(t.accepts(pt("'XX_{2, 44}'(*(*(*(*('XX_{}'('XX_{}'('XX_{}'('XX_{}'('XX_{}'(This))))),is),'XX_{}'(a)),*('XX_{}'(test),'XX_{}'(case))),'XX_{2}'('.')))")));
        
        Pruner p = new Pruner() {

            @Override
            public TreeAutomaton prePrune(TreeAutomaton automaton, StateAlignmentMarking stateMarkers) {
                return automaton;
            }

            @Override
            public TreeAutomaton postPrune(TreeAutomaton automaton, StateAlignmentMarking stateMarkers) {
                return null;
            }
        };
        
        prop = new Propagator(p);
        t = prop.convert(decomp, spa);
        
        assertEquals(null,t);
        TreeAutomaton<Span> fake = new ConcreteTreeAutomaton<>();
        
        p = new Pruner() {

            @Override
            public TreeAutomaton prePrune(TreeAutomaton automaton, StateAlignmentMarking stateMarkers) {
                return fake;
            }

            @Override
            public TreeAutomaton postPrune(TreeAutomaton automaton, StateAlignmentMarking stateMarkers) {
                return automaton;
            }
        };
        
        prop = new Propagator(p);
        t = prop.convert(decomp, spa);
        
        assertEquals(fake,t);
    }    
}
