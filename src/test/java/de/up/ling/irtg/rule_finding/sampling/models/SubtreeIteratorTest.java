/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntPredicate;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class SubtreeIteratorTest {
    /**
     * 
     */
    private Tree<Integer> ti;
    
    /**
     * 
     */
    private Function<Tree<Integer>,Tree<Integer>> mapping;
    
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
        
        com.google.common.base.Function<Integer, Integer> f = (Integer i) -> i+1;
        mapping = (Tree<Integer> k) -> {
            return k.map(f);
        };
        
        choicePoint = (int i) -> (i > 10);
    }

    /**
     * Test of next method, of class ProjectedSubtreeIterator.
     */
    @Test
    public void testNext() {
        ConcreteTreeAutomaton<Integer> cta = new ConcreteTreeAutomaton<>();
        int pNum = cta.addState(1);
        com.google.common.base.Function<Integer,Rule> funct = (Integer i) -> {
            String s = "";
            
            while(true) {
                int num = cta.getSignature().addSymbol(s, 0);
                
                if(num == i+1) {
                    Rule r = cta.createRule(pNum, num, new int[0], 1.0);
                    
                    return r;
                } else {
                    s += "a";
                }
            }
        };
        
        SubtreeIterator psi = new SubtreeIterator(this.ti.map(funct),this.choicePoint);
        
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
                    assertEquals(current.toString(),"[21, 10, 2, 2, 2]");
                    break;
                case 2:
                    assertEquals(current.toString(),"[13, 9, 6, 7]");
                    break;
                default:
                    throw new IllegalStateException();
            }
            
            ++number;
        }
        
        
        assertEquals(number,3);
    }
}
