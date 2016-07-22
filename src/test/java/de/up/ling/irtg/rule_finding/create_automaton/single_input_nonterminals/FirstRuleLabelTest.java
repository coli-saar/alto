/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton.single_input_nonterminals;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeForcedBinaryWithAritiesAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.MakeMonolingualAutomaton;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class FirstRuleLabelTest {
    /**
     * 
     */
    private MakeMonolingualAutomaton mma;
    
    /**
     * 
     */
    private TreeAutomaton aut;
    
    @Before
    public void setUp() throws ParserException {
        mma = new MakeMonolingualAutomaton();
        
       
        TreeForcedBinaryWithAritiesAlgebra tfb = new TreeForcedBinaryWithAritiesAlgebra();
        
        Tree<String> words = tfb.parseString("a(a,a,a)");
        
        aut = tfb.decompose(words);
        
        mma = new MakeMonolingualAutomaton();
    }

    /**
     * Test of apply method, of class FirstRuleLabel.
     */
    @Test
    public void testApply() throws ParseException {
        FirstRuleLabel frl = new FirstRuleLabel(aut, "false");
        
        TreeAutomaton ta = mma.introduce(aut, frl, aut);
        
        assertEquals(ta.language().size(),2);
        assertTrue(ta.accepts(TreeParser.parse("'__X__{a_2}'(a_2(a_0,'__X__{a_@_2}'('a_@_2'(a_0,a_0))))")));
        assertTrue(ta.accepts(TreeParser.parse("'__X__{a_2}'(a_2(a_0,'a_@_2'(a_0,a_0)))")));
        
        frl = new FirstRuleLabel(aut, "  true  ");
        
        ta = mma.introduce(aut, frl, aut);
        
        assertEquals(ta.language().size(),16);
        assertTrue(ta.accepts(TreeParser.parse("'__X__{a_2}'(a_2(a_0,'__X__{a_@_2}'('a_@_2'(a_0,a_0))))")));
        assertTrue(ta.accepts(TreeParser.parse("'__X__{a_2}'(a_2('__X__{a_0}'(a_0),'a_@_2'('__X__{a_0}'(a_0),a_0)))")));
        assertTrue(ta.accepts(TreeParser.parse("'__X__{a_2}'(a_2(a_0,'a_@_2'(a_0,a_0)))")));
    }
}
