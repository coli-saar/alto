/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFindingIntersectionAutomatonTest {
    
    /**
     * 
     */
    private RuleFindingIntersectionAutomaton rfi;
    
    @Before
    public void setUp() throws ParseException {
        StringAlgebra sal1 = new StringAlgebra();
        TreeAutomaton tal1 = sal1.decompose(sal1.parseString("a b"));
        
        StringAlgebra sal2 = new StringAlgebra();
        TreeAutomaton tal2 = sal2.decompose(sal2.parseString("c d"));
        
        Signature shared = new Signature();
        shared.addSymbol("l1", 2);
        shared.addSymbol("l2", 0);
        shared.addSymbol("l3", 0);
        
        Homomorphism hom1 = new Homomorphism(shared, tal1.getSignature());
        hom1.add("l1", TreeParser.parse("*(?1,?2)"));
        hom1.add("l2", Tree.create("a"));
        hom1.add("l3", Tree.create("b"));
        
        
        Homomorphism hom2 = new Homomorphism(shared, tal2.getSignature());
        hom2.add("l1", TreeParser.parse("*(?2,?1)"));
        hom2.add("l2", Tree.create("d"));
        hom2.add("l3", Tree.create("c"));
        
        this.rfi = new RuleFindingIntersectionAutomaton(tal1, tal2, hom1, hom2);
    }

    /**
     * Test of supportsBottomUpQueries method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testSupportsBottomUpQueries() {
        assertFalse(this.rfi.supportsBottomUpQueries());
    }

    /**
     * Test of supportsTopDownQueries method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testSupportsTopDownQueries() {
        assertTrue(this.rfi.supportsTopDownQueries());
    }

    /**
     * Test of getRulesBottomUp method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testGetRulesBottomUp() {
        boolean sawError = false;
        
        try{
            this.rfi.getRulesBottomUp(1, new int[0]);
        }catch(UnsupportedOperationException unsup){
            sawError = true;
        }
        
        assertTrue(sawError);
    }

    /**
     * Test of getRulesTopDown method, of class RuleFindingIntersectionAutomaton.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRulesTopDown() throws Exception {
        Set<Tree<String>> ts = this.rfi.language();
        
        assertEquals(ts.size(),1);
        assertTrue(ts.contains(TreeParser.parse("l1(l2,l3)")));
    }

    /**
     * Test of isBottomUpDeterministic method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testIsBottomUpDeterministic() {        
        assertFalse(this.rfi.isBottomUpDeterministic());
    }
}