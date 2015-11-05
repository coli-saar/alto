/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import static de.up.ling.irtg.util.TestingTools.pt;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class JustXEveryWhereTest {
    
    /**
     * 
     */
    private JustXEveryWhere<StringAlgebra.Span> intro;
    
    /**
     * 
     */
    private AlignedTrees data;
    
    @Before
    public void setUp() {
        intro = new JustXEveryWhere<>();
        
        StringAlgebra sal = new StringAlgebra();
        
        TreeAutomaton ts = sal.decompose(sal.parseString("Watch this"));
        SpanAligner sp = new SpanAligner("", ts);
        
        this.data = new AlignedTrees(ts, sp);
    }

    /**
     * Test of apply method, of class JustXEveryWhere.
     * @throws java.lang.Exception
     */
    @Test
    public void testApply() throws Exception {
        AlignedTrees at  = this.intro.apply(data);
        
        TreeAutomaton ta = at.getTrees();
        ta.getAllStates().forEach((Integer i) -> {
            assertTrue(at.getAlignments().getAlignmentMarkers(ta.getStateForId(i)).isEmpty());
        });
        
        assertTrue(ta.accepts(pt("*(Watch,this)")));
        assertTrue(ta.accepts(pt("*(Watch,X(this))")));
        assertTrue(ta.accepts(pt("*(X(Watch),X(this))")));
        assertTrue(ta.accepts(pt("*(X(Watch),this)")));
        assertTrue(ta.accepts(pt("X(*(Watch,this))")));
        assertTrue(ta.accepts(pt("X(*(Watch,X(this)))")));
        assertTrue(ta.accepts(pt("X(*(X(Watch),X(this)))")));
        assertTrue(ta.accepts(pt("X(X(*(X(Watch),X(this))))")));
        assertTrue(ta.accepts(pt("X(X(*(X(X(Watch)),X(X(this)))))")));
    }
    
}
