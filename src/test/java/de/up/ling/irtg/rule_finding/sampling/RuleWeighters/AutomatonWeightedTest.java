/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.RuleWeighters;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighting;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import de.up.ling.tree.Tree;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class AutomatonWeightedTest {

    /**
     *
     */
    private AutomatonWeighted auw;

    /**
     *
     */
    private TreeAutomaton tau;

    /**
     *
     */
    private Tree<Rule> tr;

    @Before
    public void setUp() throws Exception {
        StringAlgebra sal = new StringAlgebra();
        tau = sal.decompose(sal.parseString("a a b b a a"));

        tau.normalizeRuleWeights();
        AdaGrad ada = new AdaGrad();

        auw = new AutomatonWeighted(tau, 2, 4.0, ada);

        Tree<Integer> deriv = (Tree<Integer>) tau.languageIteratorRaw().next();
        tr = tau.getRuleTree(deriv);
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testGetLogTargetProbability() {
        double d = auw.getLogTargetProbability(tr);

        assertEquals(d, -2.70805020110221, 0.0000001);
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testGetLogProbability() {
        auw.prepareStateStartProbability();
        int startState = tr.getLabel().getParent();

        assertEquals(auw.getStartState(0.1), startState);
        assertEquals(auw.getStartState(0.4), startState);
        assertEquals(auw.getStartState(0.7), startState);
        assertEquals(auw.getStartState(1.0), startState);

        assertEquals(0.0, auw.getStateStartLogProbability(startState), 0.0000001);

        Rule r = tr.getLabel();
        auw.prepareProbability(r.getParent());
        auw.prepareProbability(r.getParent());

        assertEquals(auw.getLogProbability(r), -Math.log(5.0), 0.0000001);

        Rule[] rs = new Rule[5];

        rs[0] = (auw.getRule(r.getParent(), 0.01));
        rs[1] = (auw.getRule(r.getParent(), 0.21));
        rs[2] = (auw.getRule(r.getParent(), 0.41));
        rs[3] = (auw.getRule(r.getParent(), 0.61));
        rs[4] = (auw.getRule(r.getParent(), 0.81));

        for (int i = 1; i < rs.length; ++i) {
            assertEquals(rs[i - 1].compareTo(rs[i]), -1);
        }
        
        TreeSample<Rule> ts = new TreeSample();
        ts.addSample(tr, 0.6);
        ts.addSample(tr, 0.1);
        
        auw.adaptNormalized(ts);
        
        //auw.prepareProbability(tr.getLabel().getParent());
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testGetRule() {
        System.out.println("getLogTargetProbability");
        Tree<Rule> sample = null;
        AutomatonWeighted instance = null;
        double expResult = 0.0;
        double result = instance.getLogTargetProbability(sample);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testReset() {
        System.out.println("getLogTargetProbability");
        Tree<Rule> sample = null;
        AutomatonWeighted instance = null;
        double expResult = 0.0;
        double result = instance.getLogTargetProbability(sample);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testAdaptNormalized() {
        System.out.println("getLogTargetProbability");
        Tree<Rule> sample = null;
        AutomatonWeighted instance = null;
        double expResult = 0.0;
        double result = instance.getLogTargetProbability(sample);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testGetAutomaton() {
        assertEquals(this.tau, this.auw.getAutomaton());
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testAdaptsNormalized() {
        assertTrue(this.auw.adaptsNormalized());
    }
}
