/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton.single_input_nonterminals;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.MakeMonolingualAutomaton;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class FromTreeWithBarTest {
    /**
     * 
     */
    private TreeAutomaton ta;
    
    @Before
    public void setUp() throws ParserException {
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        
        ta = mta.decompose(mta.parseString("a(a,a)"));
    }

    /**
     * Test of apply method, of class FromTreeWithBar.
     */
    @Test
    public void testApply() throws ParseException {
        MakeMonolingualAutomaton mma = new MakeMonolingualAutomaton();
        
        TreeAutomaton tq = mma.introduce(ta, new FromTreeWithBar<>(ta), "ROOT");
        
        assertTrue(tq.accepts(TreeParser.parse("'__X__{a}'(__LR__('__X__{a}'(a),'__X__{a||}'(__RL__(a,'__X__{a}'(a)))))")));
        assertFalse(tq.accepts(TreeParser.parse("'__X__{a}'(__LR__('__X__{a}'(a),'__X__{a||}'(__RL__('__X__{a}'(a),'__X__{a}'(a)))))")));
        assertFalse(tq.accepts(TreeParser.parse("'__X__{a}'(__LR__('__X__{a}'(a),'__X__{a||}'(__RL__('__X__{a||}'(a),'__X__{a}'(a)))))")));
    }
}
