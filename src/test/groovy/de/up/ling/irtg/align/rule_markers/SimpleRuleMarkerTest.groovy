/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.rule_markers;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntIterator
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SimpleRuleMarkerTest {
    
    /**
     * 
     */
    private SimpleRuleMarker srm;
    
    /**
     * 
     */
    private TreeAutomaton<Span> t1;
    
    /**
     * 
     */
    private TreeAutomaton<Span> t2;
    
    /**
     * 
     */
    private Rule r1;
    
    /**
     * 
     */
    private Rule r2;
    
    /**
     * 
     */
    private Rule r3;
    
    /**
     * 
     */
    private Rule r4;
    
    @Before
    public void setUp() {
        
        StringAlgebra sal = new StringAlgebra();
        
        t1 = sal.decompose(sal.parseString("a b c"));
        
        int[] empty = new int[0];
        t2 = sal.decompose(sal.parseString("e d"));
        
        
        
        r1 = t1.getRulesBottomUp(t1.getSignature().getIdForSymbol("a"),empty).iterator().next();
        r2 = t1.getRulesBottomUp(t1.getSignature().getIdForSymbol("b"),empty).iterator().next();
       
        r3 = t2.getRulesBottomUp(t2.getSignature().getIdForSymbol("e"),empty).iterator().next();
        r4 = t2.getRulesBottomUp(t2.getSignature().getIdForSymbol("d"),empty).iterator().next();
        
        this.srm = new SimpleRuleMarker("V");
        
        this.srm.addPair(r1, r3);
        this.srm.addPair(r1, r4);
        this.srm.addPair(r2, r4);
    }

    /**
     * Test of addPair method, of class SimpleRuleMarker.
     */
    @Test
    public void testAddPair() {
        RuleEvaluator<IntSet> rel = srm.ruleMarkings(0);
        
        IntSet isR1 = rel.evaluateRule(r1);
        IntSet isR2 = rel.evaluateRule(r2);
        
        rel = srm.ruleMarkings(1);
        
        IntSet isR3 = rel.evaluateRule(r3);
        IntSet isR4 = rel.evaluateRule(r4);
        
        assertEquals(isR3.size(),1);
        assertTrue(isR1.contains(isR3.iterator().nextInt()));
        assertEquals(isR1.size(),2);
        
        assertEquals(isR2.size(),1);
        assertEquals(isR4.size(),2);
        
        assertTrue(isR4.contains(isR2.iterator().next()));
        
        IntIterator iit = isR4.iterator();
        
        for(int i=0;i<2;++i){
            int mark = iit.next();
            
            assertTrue(isR1.contains(mark) || isR2.contains(mark));
        }
        
        Rule r5 = t1.getRulesBottomUp(t1.getSignature().getIdForSymbol("*"),[r1.getParent(),r2.getParent()]).iterator().next();
        Rule r6 = t1.getRulesBottomUp(t1.getSignature().getIdForSymbol("*"),[r3.getParent(),r4.getParent()]).iterator().next();
        
        IntSet isR5 = srm.getMarkings(0,r5);
        assertTrue(isR5.isEmpty());
        
        IntSet isR6 = srm.ruleMarkings(1).evaluateRule(r6);
        assertTrue(isR6.isEmpty());
        
        this.srm.addPair(r5,r6);
        
        assertEquals(isR5.size(),1);
        assertEquals(isR6.size(),1);
        
        assertTrue(isR5.contains(isR6.iterator().nextInt()));
    }

    /**
     * Test of makeCode method, of class SimpleRuleMarker.
     */
    @Test
    public void testMakeCode() {
        IntSet i = new IntOpenHashSet();
        String s = this.srm.makeCode(i,t1,0);
        assertEquals(s,"V_"+i.toString());
        
        assertTrue(srm.checkCompatible(s,s));
        assertTrue(srm.checkCompatible(s,"V_"+i.toString()));
        
        
        i.add(23);
        assertFalse(srm.checkCompatible(s,"V_"+i.toString()));
        i.add(42);
        
        s = this.srm.makeCode(i,t1,0);
        assertEquals(s,"V_"+i.toString());
    }

    /**
     * Test of isFrontier method, of class SimpleRuleMarker.
     */
    @Test
    public void testIsFrontier() {
        assertTrue(srm.isFrontier("V_a"));
        assertFalse(srm.isFrontier("V_"));
        assertFalse(srm.isFrontier("VLa"));
        assertFalse(srm.isFrontier("X_a"));
        
        
        IntSet i = new IntOpenHashSet();
        String s = this.srm.makeCode(i,t1,0);
        assertTrue(srm.isFrontier(s));
    }

    /**
     * Test of getCorresponding method, of class SimpleRuleMarker.
     */
    @Test
    public void testGetCorresponding() {
        IntSet i = new IntOpenHashSet();
        String s = this.srm.makeCode(i,t1,0);
        
        assertEquals(s,this.srm.getCorresponding(s));
    }   
}