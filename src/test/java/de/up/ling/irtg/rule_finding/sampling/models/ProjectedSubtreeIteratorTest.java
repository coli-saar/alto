/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import com.google.common.base.Function;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class ProjectedSubtreeIteratorTest {
    /**
     * 
     */
    private Tree<Integer> ti;
    
    /**
     * 
     */
    private Function<Integer,Integer> mapping;
    
    /**
     * 
     */
    private IntPredicate choicePoint;
    
    
    @Before
    public void setUp() {
        List<Tree<Integer>> children = new ArrayList<>();
        
        children.add(Tree.create(1));
        children.add(Tree.create(1));
        children.add(Tree.create(1));
        
        Tree<Integer> next = Tree.create(9, children);
        Tree<Integer> cut = Tree.create(20, new Tree[] {next});
        
        children = new ArrayList<>();
        children.add(cut);
        children.add(next);
        
        Tree<Integer> longer = Tree.create(7, children);
        Tree<Integer> evenLonger = Tree.create(8, new Tree[] {longer});
        
        children = new ArrayList<>();
        
        children.add(Tree.create(5));
        children.add(Tree.create(6));
        
        Tree<Integer> intermediate = Tree.create(8,children);
        Tree<Integer> higher = Tree.create(12, new Tree[] {intermediate});
        
        children = new ArrayList<>();
        children.add(evenLonger);
        children.add(higher);
        
        ti = Tree.create(5, children);
        
        mapping = (Integer k) -> k+1;
        
        choicePoint = (int i) -> (i > 10);
    }

    /**
     * Test of next method, of class ProjectedSubtreeIterator.
     */
    @Test
    public void testNext() {
        ProjectedSubtreeIterator<Integer> psi =
                new ProjectedSubtreeIterator<>(this.ti,this.mapping,this.choicePoint);
        
        int number = 0;
        IntList last = null;
        while(psi.hasNext()) {
            IntList current;
            current = psi.next();
            if(last != null) {
                assertTrue(last == current);
            }
            
            last = current;
            
            switch(number) {
                case 0:
                    assertEquals(current.toString(),"[6, 9, 8, 21, 10, 2, 2, 2, 13]");
                    break;
                case 1:
                    assertEquals(current.toString(),"[10, 2, 2, 2]");
                    break;
                case 2:
                    assertEquals(current.toString(),"[9, 6, 7]");
                    break;
                default:
                    throw new IllegalStateException();
            }
            
            ++number;
        }
        
        
        assertEquals(number,3);
    }
}
