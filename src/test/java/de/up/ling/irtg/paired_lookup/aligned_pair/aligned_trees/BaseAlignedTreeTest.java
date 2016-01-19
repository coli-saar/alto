/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.aligned_trees;

import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class BaseAlignedTreeTest {
    
    /**
     * 
     */
    private BaseAlignedTree bat1;
    
    /**
     * 
     */
    private BaseAlignedTree bat2;
    
    /**
     * 
     */
    private Tree<String> t1;
    
    /**
     * 
     */
    private Tree<String> t2;
    
    @Before
    public void setUp() throws ParseException {
        t1 = TreeParser.parse("a(j(c,'?2'), i('?1'))");
        t2 = TreeParser.parse("p('?1','?2')");
        
        IntList states1 = new IntArrayList();
        states1.add(14);
        states1.add(78);
        states1.add(22);
        
        IntSet ins = new IntOpenHashSet();
        ins.add(2);
        ins.add(6);
        List<IntCollection> align = new ArrayList<>();
        align.add(ins);
        align.add(ins);
        align.add(new IntOpenHashSet());
        
        bat1 = new BaseAlignedTree(t1, align, states1, 2.0);
        
        states1 = new IntArrayList();
        states1.add(1);
        states1.add(7);
        states1.add(2);
        
        ins = new IntOpenHashSet();
        ins.add(5);
        ins.add(1);
        align = new ArrayList<>();
        align.add(ins);
        align.add(new IntOpenHashSet());
        align.add(ins);
        
        bat2 = new BaseAlignedTree(t2, align, states1, 3.0);
    }

    /**
     * Test of getTree method, of class BaseAlignedTree.
     */
    @Test
    public void testGetTree() {
        assertTrue(t1 == bat1.getTree());
        assertTrue(t2 == bat2.getTree());
    }

    /**
     * Test of getNumberVariables method, of class BaseAlignedTree.
     */
    @Test
    public void testGetNumberVariables() {
        assertEquals(bat1.getNumberVariables(),2);
    }

    /**
     * Test of getStateForVariable method, of class BaseAlignedTree.
     */
    @Test
    public void testGetStateForVariable() {
        assertEquals(bat1.getStateForVariable(0),78);
        assertEquals(bat1.getStateForVariable(1),22);
        
        assertEquals(bat2.getStateForVariable(0),7);
        assertEquals(bat2.getStateForVariable(1),2);
    }

    /**
     * Test of getWeight method, of class BaseAlignedTree.
     */
    @Test
    public void testGetWeight() {
        assertEquals(bat1.getWeight(),2.0,0.0000000000001);
        assertEquals(bat2.getWeight(),3.0,0.0000000000001);
    }

    /**
     * Test of getRootAlignments method, of class BaseAlignedTree.
     */
    @Test
    public void testGetRootAlignments() {
        assertTrue(bat1.getRootAlignments() == bat1.getAlignmentsForVariable(0));
        assertTrue(bat2.getRootAlignments() == bat2.getAlignmentsForVariable(1));
        
        assertTrue(bat1.getRootAlignments() != bat1.getAlignmentsForVariable(1));
        assertTrue(bat2.getRootAlignments() != bat2.getAlignmentsForVariable(0));
    }

    /**
     * Test of getAlignmentsForVariable method, of class BaseAlignedTree.
     */
    @Test
    public void testGetAlignmentsForVariable() {
        IntCollection ic = bat1.getAlignmentsForVariable(1);
        assertTrue(ic.isEmpty());
        
        ic = bat1.getAlignmentsForVariable(0);
        assertEquals(ic.size(),2);
        
        assertTrue(ic.contains(6));
        assertTrue(ic.contains(2));
    }

    /**
     * Test of isEmpty method, of class BaseAlignedTree.
     * @throws de.up.ling.tree.ParseException
     */
    @Test
    public void testIsEmpty() throws ParseException {
        assertFalse(bat1.isEmpty());
        
        Tree<String> t = TreeParser.parse("'?1'");
        
        BaseAlignedTree bat = new BaseAlignedTree(t, new ArrayList<>(), new IntArrayList(), 1.0);
        
        assertTrue(bat.isEmpty());
    }
    
}
