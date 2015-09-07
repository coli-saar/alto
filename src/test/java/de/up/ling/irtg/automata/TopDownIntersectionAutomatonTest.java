/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.algebra.StringAlgebra;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class TopDownIntersectionAutomatonTest {
   
    /**
     * 
     */
    private TreeAutomaton ta1;
    
    /**
     * 
     */
    private ConcreteTreeAutomaton ta2;
    
    /**
     * 
     */
    private TopDownIntersectionAutomaton tia;
    
    
    @Before
    public void setUp() {
        StringAlgebra alg = new StringAlgebra();
        ta1 = alg.decompose(alg.parseString("a b c"));
        
        ta2 = new ConcreteTreeAutomaton(alg.getSignature());
        ta2.addFinalState(ta2.addState("q1"));
        ta2.addRule(ta2.createRule("q1", "*", new String[] {"q1","q1"}));
        
        ta2.addRule(ta2.createRule("q1", "a", new String[0]));
        ta2.addRule(ta2.createRule("q1", "b", new String[0]));
        ta2.addRule(ta2.createRule("q1", "c", new String[0]));
        
        tia = new TopDownIntersectionAutomaton(ta1, ta2);
    }

    /**
     * Test of getRulesBottomUp method, of class TopDownIntersectionAutomaton.
     */
    @Test
    public void testGetRulesBottomUp() {
        boolean sawError = false;
        try{
            tia.getRulesBottomUp(1, new int[] {1,2,3});
        }catch(UnsupportedOperationException uoe){
            sawError = true;
        }
        
        assertTrue(sawError);
    }

    /**
     * Test of supportsBottomUpQueries method, of class TopDownIntersectionAutomaton.
     */
    @Test
    public void testSupportsBottomUpQueries() {
        assertFalse(tia.supportsBottomUpQueries());
    }

    /**
     * Test of supportsTopDownQueries method, of class TopDownIntersectionAutomaton.
     */
    @Test
    public void testSupportsTopDownQueries() {
        assertTrue(tia.supportsTopDownQueries());
    }

    /**
     * Test of getRulesTopDown method, of class TopDownIntersectionAutomaton.
     */
    @Test
    public void testGetRulesTopDown() throws Exception {
        int top = tia.getFinalStates().iterator().nextInt();
        assertEquals(tia.getFinalStates().size(),1);
        
        Set<Tree<String>> language = tia.language();
        
        Set<Tree<String>> expected = new HashSet<>();
        expected.add(pt("*(*(a,b),c)"));
        expected.add(pt("*(a,*(b,c))"));
        
        for(Tree<String> t : language){
            assertTrue(expected.contains(t));
            expected.remove(t);
        }
        
        assertEquals(language.size(),2);
        assertTrue(expected.isEmpty());
    }

    /**
     * Test of isBottomUpDeterministic method, of class TopDownIntersectionAutomaton.
     */
    @Test
    public void testIsBottomUpDeterministic() {
        assertTrue(tia.isBottomUpDeterministic());
    }
    
}