/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.rule_weighting;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import de.up.ling.tree.Tree;
import java.util.Arrays;
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
        auw.prepareStartProbability();
        
        Rule r = tr.getLabel();
        auw.prepareProbability(r.getParent());
        auw.prepareProbability(r.getParent());
        
        assertEquals(auw.getLogProbability(r), -Math.log(5.0), 0.0000001);

        int[] ruleNums = new int[5];
        ruleNums[0] = auw.getRuleNumber(r.getParent(), 0.01);
        ruleNums[1] = auw.getRuleNumber(r.getParent(), 0.21);
        ruleNums[2] = auw.getRuleNumber(r.getParent(), 0.41);
        ruleNums[3] = auw.getRuleNumber(r.getParent(), 0.61);
        ruleNums[4] = auw.getRuleNumber(r.getParent(), 0.81);
        
        Rule[] rs = new Rule[5];

        rs[0] = auw.getRuleByNumber(r.getParent(), ruleNums[0]);
        rs[1] = auw.getRuleByNumber(r.getParent(), ruleNums[1]);
        rs[2] = auw.getRuleByNumber(r.getParent(), ruleNums[2]);
        rs[3] = auw.getRuleByNumber(r.getParent(), ruleNums[3]);
        rs[4] = auw.getRuleByNumber(r.getParent(), ruleNums[4]);

        assertArrayEquals(ruleNums, new int[] {0,1,2,3,4});
        
        for (int i = 1; i < rs.length; ++i) {
            assertEquals(rs[i - 1].compareTo(rs[i]), -1);
        }
        
        assertEquals(auw.getLogProbability(rs[0]),Math.log(1.0 / 5.0),0.000001);
        assertEquals(auw.getLogProbability(r.getParent(), 0),auw.getLogProbability(rs[0]),0.000001);
        
        TreeSample<Rule> ts = new TreeSample();
        ts.addSample(tr);
        ts.addSample(tr);
        
        ts.setLogPropWeight(0, -10.0);
        ts.setLogPropWeight(1, -2.0);
        
        ts.setLogSumWeight(0, -5.0);
        ts.setLogSumWeight(1, -1.0);
        
        ts.setLogTargetWeight(0, 4.0);
        ts.setLogTargetWeight(1, 2.0);
        
        double prob1 = computeProb(auw,tr);
        auw.adapt(ts,true);
        double prob2 = computeProb(auw,tr);
        assertTrue(prob2 > prob1);
        auw.adapt(ts,false);
        double prob3 = computeProb(auw,tr);
        assertTrue(prob3 < prob2);
        auw.adapt(ts, true);
        double prob4 = computeProb(auw,tr);
        assertTrue(prob4 > prob2);
        
        auw.prepareStartProbability();
        double d = auw.getLogTargetProbability(tr);
        
        Tree tstring =  tr.map((Rule rule) -> auw.getAutomaton().getSignature().resolveSymbolId(rule.getLabel()));
        
        assertEquals(d,Math.log(auw.getAutomaton().getWeight(tstring)),0.000001);
    }

    /**
     * Test of getLogTargetProbability method, of class AutomatonWeighted.
     */
    @Test
    public void testGetAutomaton() {
        assertEquals(this.tau, this.auw.getAutomaton());
        
        this.auw.prepareStartProbability();
        assertEquals(this.auw.getNumberOfStartStates(),1);
        
        int number = this.auw.getStartStateNumber(0.9);
        assertEquals(number,0);
        
        int startState = this.tau.getFinalStates().iterator().nextInt();
        assertEquals(startState,this.auw.getStartStateByNumber(number));
        
        assertEquals(this.auw.getStateStartLogProbability(number),0.0,0.0000001);
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
