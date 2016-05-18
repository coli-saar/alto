/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.Propagator;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class MaxSizeTest {
    
    
    /**
     *
     */
    private IntersectionPruner ip1;
    
    /**
     *
     */
    private TreeAutomaton tq;
    
    
    
    @Before
    public void setUp() throws ParserException {
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        TreeAutomaton ta = mta.decompose(mta.parseString("a(b,c(d))"));

        ip1 = new IntersectionPruner(new IntersectionOptions[]
                {IntersectionOptions.MAX_SIZE, IntersectionOptions.NO_LEFT_INTO_RIGHT, IntersectionOptions.ENSURE_ARITIES.NO_EMPTY}, new String[] {"  1    ","",""});
        
        Propagator po = new Propagator();
        SpecifiedAligner spal = new SpecifiedAligner(ta);
        Set<String> counter = new HashSet<>();
        counter.add("");

        tq = po.convert(ta, po.propagate(ta, spal), counter);
    }

    @Test
    public void testSomeMethod() throws ParseException {
        TreeAutomaton ta = ip1.apply(tq);
        ta.normalizeRuleWeights();
        
        assertFalse(ta.isEmpty());
        
        Set<Tree<String>> s = ta.language();
        
        assertEquals(s.size(),5);
        
        assertTrue(s.contains(TreeParser.parse("__RL__(__RL__(a,b),__RL__(c,d))")));
        assertTrue(s.contains(TreeParser.parse("__RL__('__X__{ _@_ 0-1-0}'(__RL__(a,b)),'__X__{ _@_ 0-1-0-1}'(__RL__('__X__{ _@_ 0-0-0-1}'(c),'__X__{ _@_ 0-0-0-1-0}'(d))))")));
        assertTrue(s.contains(TreeParser.parse("__RL__('__X__{ _@_ 0-1-0}'(__RL__('__X__{ _@_ 0-0-0}'(a),'__X__{ _@_ 0-0-0-0}'(b))),'__X__{ _@_ 0-1-0-1}'(__RL__(c,d)))")));
        assertTrue(s.contains(TreeParser.parse("__RL__('__X__{ _@_ 0-1-0}'(__RL__('__X__{ _@_ 0-0-0}'(a),'__X__{ _@_ 0-0-0-0}'(b))),'__X__{ _@_ 0-1-0-1}'(__RL__('__X__{ _@_ 0-0-0-1}'(c),'__X__{ _@_ 0-0-0-1-0}'(d))))")));
        assertTrue(s.contains(TreeParser.parse("__RL__('__X__{ _@_ 0-1-0}'(__RL__(a,b)),'__X__{ _@_ 0-1-0-1}'(__RL__(c,d)))")));
    }
}
