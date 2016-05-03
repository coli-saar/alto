/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.random_rtg.RandomTreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.List;
import org.apache.commons.math3.random.Well44497b;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class EvaluateSamplingFromRulesTest {
    
    public EvaluateSamplingFromRulesTest() {
    }
    
    @Before
    public void setUp() {
    }

    /**
     * Test of makeSmoothedKL method, of class EvaluateSamplingFromRules.
     */
    @Test
    public void testMakeSmoothedKL() throws Exception {
        //TODO
    }
    
    @Test
    public void testPick() throws Exception {
        RandomTreeAutomaton rta = new RandomTreeAutomaton(new Well44497b(9398734589L), new String[] {"a","b","c"}, 2.0);
        
        TreeAutomaton<Integer> ta = rta.getRandomAutomaton(20, 2, 2.0);
        
        Tree<Integer> ti = ta.viterbiRaw().getTree();
        
        Tree<Rule> rules = ta.getRuleTree(ti);
        System.out.println(rules);
        
        EvaluateSamplingFromRules.StatePicker<Integer> st = new EvaluateSamplingFromRules.StatePicker<>(4);
        
        List<Integer> choices = st.pick(ta);
        System.out.println(choices);
        
        assertEquals(choices.size(),2);
        assertEquals(ta.getIdForState(choices.get(0)),rules.getLabel().getParent());
        assertEquals(ta.getIdForState(choices.get(1)),ta.getIdForState(13));
        
    }
    
}
