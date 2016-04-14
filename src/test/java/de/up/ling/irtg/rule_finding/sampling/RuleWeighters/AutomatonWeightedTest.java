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
     * @throws java.lang.Exception
     */
    @Test
    public void testGetLogProbability() throws Exception {
        /*
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
        
        double prob1 = computeProb(auw,tr);
        auw.adaptNormalized(ts);
        double prob2 = computeProb(auw,tr);
        assertTrue(prob2 > prob1);
        auw.adaptNormalized(ts);
        double prob3 = computeProb(auw,tr);
        
        assertTrue(prob3 > prob2);
        assertEquals(prob3,-1.2771627272667416,0.000001);
        
        Iterator<Tree<Integer>> derivs = tau.languageIteratorRaw();
        derivs.next();
        derivs.next();
        derivs.next();
        derivs.next();
        derivs.next();
        
        Tree<Integer> pick = derivs.next();
        Tree<Rule> trule = tau.getRuleTree(pick);
        ts.addSample(trule, 0.8);
        
        auw.adaptNormalized(ts);
        double prob4 = computeProb(auw,tr);
        assertEquals(prob4,-1.2962363502918266,0.00001);
        
        double prob5 = computeProb(auw,trule);
        assertEquals(prob5,-2.040289887194336,0.00001);
                
        auw.reset();
        double prob6 = computeProb(auw,tr);
        assertEquals(prob6,prob1,0.0000000001);
        
        auw.adaptNormalized(ts);
        auw.adaptNormalized(ts);
        auw.adaptNormalized(ts);
        auw.adaptNormalized(ts);
        
        Rule r1 = auw.getRule(startState, 0.1);
        Rule r2 = auw.getRule(startState, 0.3);
        Rule r3 = auw.getRule(startState, 0.5);
        Rule r4 = auw.getRule(startState, 0.7);
        Rule r5 = auw.getRule(startState, 0.9);
        Rule r6 = auw.getRule(startState, 1.0);
        
        assertEquals(r1,r2);
        assertEquals(r3,r4);
        assertFalse(r1.equals(r3));
        assertTrue(r5.compareTo(r4) > 0);
        assertTrue(r4.compareTo(r2) > 0);
        assertTrue(r6.compareTo(r5) > 0);
        */
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
    
    /**
     * 
     * @param auw
     * @param tr
     * @return 
     */
    private double computeProb(AutomatonWeighted auw, Tree<Rule> tr) {
        double sum = 0.0;
        
        for(Tree<Rule> node : tr.getAllNodes()) {
            auw.prepareProbability(node.getLabel().getParent());
            sum += auw.getLogProbability(node.getLabel());
        }
        
        return sum;
    }
}
