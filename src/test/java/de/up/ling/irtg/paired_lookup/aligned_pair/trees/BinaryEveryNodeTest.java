/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.trees;

import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedTree;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class BinaryEveryNodeTest {
    
    /**
     * 
     */
    private BinaryEveryNode tree1;
    
    /**
     * 
     */
    private BinaryEveryNode tree2;
    
    @Before
    public void setUp() throws ParseException {
        String q = "S(CP(NP('Hans::1'),'VP::8'('remembered::2  3 4', NP('his::5', 'book::6'))), '.::7')";
        tree1 = new BinaryEveryNode(q, true);
        
        String s = "S(CP(NP('Hans::1'), 'VP::8'('erinnerte::2', CP('sich::3', 'an::4', NP('sein::5', 'Buch::6')))), '.::7')";
        tree2 = new BinaryEveryNode(s, false);
    }

    /**
     * Test of getAlignedTrees method, of class BinaryEveryNode.
     */
    @Test
    public void testGetAlignedTrees_int() {
        System.out.println(this.tree1);
        
        int fin = this.tree1.getFinalStates().iterator().nextInt();
        
        Stream<AlignedTree> st = this.tree1.getAlignedTrees(fin);
        st.forEach((AlignedTree at) -> System.out.println(at));
        
        
        //TODO
    }

    /**
     * Test of getAlignedTrees method, of class BinaryEveryNode.
     */
    @Test
    public void testGetAlignedTrees_int_AlignedTree() {
        System.out.println("getAlignedTrees");
        int parent = 0;
        AlignedTree at = null;
        BinaryEveryNode instance = null;
        Stream<AlignedTree> expResult = null;
        Stream<AlignedTree> result = instance.getAlignedTrees(parent, at);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getState method, of class BinaryEveryNode.
     */
    @Test
    public void testGetState() {
        System.out.println("getState");
        int state = 0;
        BinaryEveryNode instance = null;
        Tree<String> expResult = null;
        Tree<String> result = instance.getState(state);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFinalStates method, of class BinaryEveryNode.
     */
    @Test
    public void testGetFinalStates() {
        IntStream fin = this.tree1.getFinalStates();
        AtomicInteger ai = new AtomicInteger();
        
        fin.forEach((int state) -> {
            Tree<String> t = this.tree1.getState(state);
            
            try {
                assertEquals(t,TreeParser.parse("S(CP(NP(Hans), VP(remembered, NP(his, book))), '.')"));
            } catch (ParseException ex) {
                throw new RuntimeException("Could not parse comparison tree.");
            }
            
            ai.incrementAndGet();
        });
        
        assertEquals(ai.get(),1);
    }

    /**
     * Test of getAlignments method, of class BinaryEveryNode.
     */
    @Test
    public void testGetAlignments() {
        System.out.println("getAlignments");
        int state = 0;
        BinaryEveryNode instance = null;
        IntCollection expResult = null;
        IntCollection result = instance.getAlignments(state);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
