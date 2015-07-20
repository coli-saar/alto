/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.align.alignment_algebras.StringAlignmentAlgebra
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree
import org.junit.Before;
import static de.up.ling.irtg.util.TestingTools.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class RuleTreeFinderTest {
    
    /**
     * 
     */
    private RuleTreeFinder rtf;
    
    @Before
    public void setUp() {
        Map<String,AlignmentAlgebra> map = new HashMap<>();
        map.put("string", new StringAlignmentAlgebra());
        
        rtf = new RuleTreeFinder(map);
    }

    /**
     * Test of getRuleOptionsReweighted method, of class RuleTreeFinder.
     */
    @Test
    public void testGetRuleOptionsReweighted() {
        Pair<TreeAutomaton,AlignmentMapper> p = rtf.getRuleOptions(new Pair<>("a b c d","e f g"),"string");
        
        
        String one = "a:1:4 b";
        String two = "c d:1 e:4";
        
        p = rtf.getRuleOptionsReweighted(new Pair<>(one,two),"string");
        
        TreeAutomaton ta = p.getLeft();
        Tree<String> t = ta.languageIterator().next();
        
        AlignmentMapper am = p.getRight();
        
        List<Pair<String,String>> list = am.getPairings(t);
        
        assertEquals(list.size(),3);
        assertTrue(list.contains(new Pair<Tree<String>>(pt("a"),pt("*(d,e)"))));
        assertTrue(list.contains(new Pair<Tree<String>>(pt("b"),pt("c"))));
        assertTrue(list.contains(new Pair<Tree<String>>(pt("*(x_1,x_2)"),pt("*(x_2,x_1)"))));
    }
    
}
