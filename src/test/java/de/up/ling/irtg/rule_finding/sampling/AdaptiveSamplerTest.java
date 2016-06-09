/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.rule_finding.sampling.rule_weighters.AutomatonWeighted;
import de.up.ling.irtg.rule_finding.sampling.statistic_tracking.RuleConvergence;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class AdaptiveSamplerTest {
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
    
    /**
     * 
     */
    private AdaptiveSampler adaSamp;
    
    /**
     * 
     */
    private int startState;
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        tau = sal.decompose(sal.parseString("a a b b a a"));

        AdaGrad ada = new AdaGrad();
        auw = new AutomatonWeighted(tau, 2, 10000.0, ada);      
        adaSamp = new AdaptiveSampler(9883498483484L);
        
        startState = tau.getFinalStates().iterator().nextInt();
    }

    /**
     * Test of adaSample method, of class AdaptiveSampler.
     */
    @Test
    public void testAdaSample() {
        int rounds = 20;
        
        List<TreeSample<Rule>> ts = adaSamp.adaSample(rounds, 1500, auw, true, true);
        
        assertEquals(ts.size(),rounds);
        for(int i=0;i<ts.size();++i) {
            assertTrue(ts.get(i).populationSize() <= 1500);
        }
        
        for(Rule r : (Iterable<Rule>) this.tau.getRulesTopDown(startState)) {
            switch (r.toString(tau)) {
                case "'0-6'! -> *('0-1', '1-6') [1.0]":
                    assertTrue(Math.exp(auw.getLogProbability(r)) > 0.3);
                    Int2ObjectMap<Double> inside = auw.getAutomaton().inside();
                    double d = inside.get(r.getChildren()[0]);
                    d += inside.get(r.getChildren()[1]);
                    assertEquals(d / inside.get(r.getParent()),Math.exp(auw.getLogProbability(r)),0.05);
                    break;
                case "'0-6'! -> *('0-3', '3-6') [1.0]":
                    assertTrue(Math.exp(auw.getLogProbability(r)) < 0.1);
                    break;
            }
        }
    }
}
