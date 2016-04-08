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
import de.up.ling.irtg.rule_finding.sampling.RuleWeighters.AutomatonWeighted;
import de.up.ling.irtg.rule_finding.sampling.statistic_tracking.RuleConvergence;
import de.up.ling.tree.Tree;
import java.util.List;
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
    private RuleConvergence ruC;
    
    /**
     * 
     */
    private int startState;
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        tau = sal.decompose(sal.parseString("a a b b a a"));

        AdaGrad ada = new AdaGrad();
        auw = new AutomatonWeighted(tau, 2, 4.0, ada);      
        adaSamp = new AdaptiveSampler(9883498483484L);
        
        startState = tau.getFinalStates().iterator().nextInt();
        int[] tracked = new int[] {startState};
        ruC = new RuleConvergence(tau, tracked);
    }

    /**
     * Test of getKeptStats method, of class AdaptiveSampler.
     */
    @Test
    public void testGetKeptStats() {
        adaSamp.setKeptStats(ruC);
        assertEquals(adaSamp.getKeptStats(),ruC);
        
        adaSamp.setKeptStats(null);
        assertEquals(adaSamp.getKeptStats(),null);
    }

    /**
     * Test of adaSample method, of class AdaptiveSampler.
     */
    @Test
    public void testAdaSample() {
        //TODO
        adaSamp.setKeptStats(ruC);
        List<TreeSample<Rule>> ts = adaSamp.adaSample(10, 5000, 50, auw);
        
        for(TreeSample<Rule> t : ts) {
            System.out.println(t);
        }
        
    }    
}
