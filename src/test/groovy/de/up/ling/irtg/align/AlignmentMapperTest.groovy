/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.align.alignment_algebras.StringAlignmentAlgebra
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.hom.Homomorphism
import de.up.ling.tree.Tree;
import static de.up.ling.irtg.util.TestingTools.*;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class AlignmentMapperTest {
    
    /**
     *
     */
    private AlignmentMapper am;
    
    /**
     * 
     */
    private Homomorphism h1;
    
    /**
     * 
     */
    private Homomorphism h2;
    
    /**
     * 
     */
    private Tree<String> t;
    
    /**
    *
    */
    @Before
    public void setUp() {
        RuleTreeGenerator rgen = new RuleTreeGenerator();
        
        MarkingPropagator mp = new MarkingPropagator();
        
        StringAlignmentAlgebra saa = new StringAlignmentAlgebra();
        
        String one = "a:1:4 b";
        String two = "c d:1 e:4";
        
        Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> parts = saa.decomposePair(one, two);
        
        TreeAutomaton ta1 = mp.introduce(parts.getRight().getLeft(),parts.getLeft(),0);
        TreeAutomaton ta2 = mp.introduce(parts.getRight().getRight(),parts.getLeft(),1);
        
        Pair<TreeAutomaton,Pair<Homomorphism,Homomorphism>> result = rgen.makeInverseIntersection(ta1,ta2,parts.getLeft());
        h1 = result.getRight().getLeft();
        h2 = result.getRight().getRight();
        
        am = new AlignmentMapper(h1,h2,parts.getLeft());
        
        TreeAutomaton ta = result.getLeft();
        
        for(Rule h : ta.getRuleIterable()){
            String label = h.getLabel(ta);
            
            if(label.matches("X.+")){
               h.setWeight(3.0);
               continue;
            }
            
            h.setWeight(0.5);
        }
        
        Iterator<Tree<String>> it = ta.languageIterator();
        
        this.t = it.next();
    }

    /**
     * Test of getOriginalTreeHomOne method, of class AlignmentMapper.
     */
    @Test
    public void testGetOriginalTreeHomOne() {
        assertEquals(this.am.getOriginalTreeHomOne(t),pt("*(a,b)"));
        
        assertEquals(this.am.getHomOne(t),h1.apply(t));
    }
    
    /**
     * Test of getOriginalTreeHomTwo method, of class AlignmentMapper.
     */
    @Test
    public void testGetOriginalTreeHomTwo() {
        assertEquals(this.am.getOriginalTreeHomTwo(t),pt("*(c,*(d,e))"));
        
        assertEquals(this.am.getHomTwo(t),h2.apply(t));
    }

    /**
     * Test of variableTreeHomOne method, of class AlignmentMapper.
     */
    @Test
    public void testVariableTreeHomOne() {
        assertEquals(this.am.variableTreeHomOne(t),pt("*(x_1(a),x_2(b))"));
    }

    /**
     * Test of variableTreeHomTwo method, of class AlignmentMapper.
     */
    @Test
    public void testVariableTreeHomTwo() {
        assertEquals(this.am.variableTreeHomTwo(t),pt("*(x_2(c),x_1(*(d,e)))"));
    }

    /**
     * Test of getPairings method, of class AlignmentMapper.
     */
    @Test
    public void testGetPairings() {
        assertEquals(this.am.getOperationPairs(this.t).toString(),"\"*('?1','?2'),*('?2','?1')\"("+
        "\"'X_{0, 1}'('?1'),'X_{0, 1}'('?1')\"(\"'?2',*('?1','?2')\"('a,d','a,e')),\"'X_{}'('?1'),'X_{}'('?1')\"('b,c'))");
        
        List<Pair<String,String>> list = this.am.getPairings(t);
        
        assertEquals(list.size(),3);
        assertTrue(list.contains(new Pair<Tree<String>>(pt("a"),pt("*(d,e)"))));
        assertTrue(list.contains(new Pair<Tree<String>>(pt("b"),pt("c"))));
        assertTrue(list.contains(new Pair<Tree<String>>(pt("*(x_1,x_2)"),pt("*(x_2,x_1)"))));
    }
}
