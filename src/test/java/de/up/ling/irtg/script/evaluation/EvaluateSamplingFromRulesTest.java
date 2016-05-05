/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.random_rtg.RandomTreeAutomaton;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler.Configuration;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighters.AutomatonWeighted;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighting;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class EvaluateSamplingFromRulesTest {
    /**
     * 
     */
    private List<TreeAutomaton<Integer>> lta;
    
    @Before
    public void setUp() {
        lta = new ArrayList<>();
        RandomTreeAutomaton rta = new RandomTreeAutomaton(new Date().getTime(),0.5);
        for(int i=0;i<2;++i) {
            lta.add(rta.getRandomAutomaton(10));
            lta.get(i).normalizeRuleWeights();
        }
    }

    /**
     * Test of makeSmoothedKL method, of class EvaluateSamplingFromRules.
     * @throws java.lang.Exception
     */
    @Test
    public void testMakeInside() throws Exception {
        Function<TreeAutomaton,RuleWeighting> make = (TreeAutomaton t) -> {
            AdaGrad ada = new AdaGrad(1.0);
            AutomatonWeighted aw = new AutomatonWeighted(t, 2, 1000, ada);
            
            return aw;
        };
        
        Configuration conf = new AdaptiveSampler.Configuration(make);
        conf.setDeterministic(true);
        conf.setPopulationSize(5);
        conf.setRounds(10);
        
        Pair<DoubleList,List<DoubleList>> p = EvaluateSamplingFromRules.makeInside(lta.get(0), conf, 20);
        
        assertEquals(p.getLeft().size(),10);
        assertEquals(p.getRight().size(),20);
    }
    
}
