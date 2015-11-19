/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.rule_finding.SubtreeIterator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SubtreeIteratorTest {
    
    /**
     * 
     */
    private SubtreeIterator si1;
    
    
    /**
     * 
     */
    private SubtreeIterator si2;
    
    
    @Before
    public void setUp() {
        /*
        Signature sig1 = new Signature();
        Signature sig2 = new Signature();
        
        HomomorphismManager hm = new HomomorphismManager(sig1, sig2);
        
        int splitPointCode = hm.getDefaultVariable();
        
        int otherBin = splitPointCode +1;
        int otherUn  = otherBin+1;
        int other0   = otherUn+1;
        
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton();
        
        Rule bin = cta.createRule(0, otherBin, new int[] {0,0}, 0.0);
        Rule un = cta.createRule(0, otherUn, new int[] {0}, 0.0);
        Rule zero = cta.createRule(0, other0, new int[] {}, 0.0);
        Rule split = cta.createRule(0, splitPointCode, new int[] {0,0,0,0,0}, 0.0);
        
        Tree<Rule> t1 = Tree.create(zero);
        Tree<Rule> t2 = Tree.create(bin, t1,t1);
        Tree<Rule> t3 = Tree.create(split, t2);
        Tree<Rule> t4 = Tree.create(un, t3);
        Tree<Rule> t5 = Tree.create(bin, t4, t3);
        Tree<Rule> t6 = Tree.create(bin, t3, t5);
        Tree<Rule> t7 = Tree.create(un, t6);
        
        si1 = new SubtreeIterator(t7, hm);
        
        Tree<Rule> t8 = Tree.create(bin, t3,t3);
        Tree<Rule> t9 = Tree.create(split, t8);
        Tree<Rule> t10 = Tree.create(un, t9);
        
        si2 = new SubtreeIterator(t10, hm);
        */
    }

    /**
     * Test of hasNext method, of class SubtreeIterator.
     */
    @Test
    public void testHasNext() {
        /*
        int count = 0;
        Object2LongOpenHashMap<IntList> map = new Object2LongOpenHashMap<>();
        
        IntArrayList last = null;
        boolean called = false;
        while(si1.hasNext()){
            IntArrayList ial = si1.next();
            if(last != null){
                called = true;
                assertTrue(ial == last);
            }
            last = ial;
            ial = new IntArrayList(ial);
            
            map.addTo(ial, 1);
            
            ++count;
        }
        
        assertTrue(called);
        last = new IntArrayList(new int[] {3,2,1,2,3,1,1});
        assertEquals(map.getLong(last),1);
        
        last = new IntArrayList(new int[] {2,4,4});
        assertEquals(map.getLong(last),3);
        
        assertEquals(4,count);
        assertFalse(si1.hasNext());
        System.out.println("------------");
        
        count = 0;
        map.clear();
        while(si2.hasNext()){
            IntArrayList ial = si2.next();
            ial = new IntArrayList(ial);
            
            map.addTo(ial, 1);
            ++count;
        }
        
        assertEquals(4,count);
        
        last = new IntArrayList(new int[] {3,1});
        assertEquals(map.getLong(last),1);
        
        last = new IntArrayList(new int[] {2,1,1});
        assertEquals(map.getLong(last),1);
        
        last = new IntArrayList(new int[] {2,4,4});
        assertEquals(map.getLong(last),2);
        
        assertFalse(si1.hasNext());
                */
    }
}
