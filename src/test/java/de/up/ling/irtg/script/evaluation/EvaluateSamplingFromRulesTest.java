/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.random_rtg.RandomTreeAutomaton;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler.Configuration;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighters.AutomatonWeighted;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighting;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.math3.random.Well44497a;
import org.apache.commons.math3.random.Well44497b;
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
        RandomTreeAutomaton rta = new RandomTreeAutomaton(new Well44497a(9327499298347L), new String[] {"a","b","c"}, 2.0);
        for(int i=0;i<2;++i) {
            lta.add(rta.getRandomAutomaton(1000, 3, 3.0));
        }
        
        
        
        
    }

    /**
     * Test of makeSmoothedKL method, of class EvaluateSamplingFromRules.
     */
    @Test
    public void testMakeSmoothedKL() throws Exception {
        Function<TreeAutomaton,RuleWeighting> make = new Function<TreeAutomaton, RuleWeighting>() {

            @Override
            public RuleWeighting apply(TreeAutomaton t) {
                AdaGrad ada = new AdaGrad(0.2);
                AutomatonWeighted aw = new AutomatonWeighted(t, 2, 0.5, ada);
                
                return aw;
            }
        };
        
        Configuration conf = new AdaptiveSampler.Configuration(make);
        conf.setDeterministic(true);
        conf.setPopulationSize(200);
        conf.setRounds(10);
        
        EvaluateSamplingFromRules.Measurements<Integer> meas = EvaluateSamplingFromRules.makeSmoothedKL(lta, 3, new EvaluateSamplingFromRules.StatePicker<>(5), conf);
        for(int i=0;i<20;++i) {
            System.out.println("---------");
            
            double d = 0.0;
            for(int k=0;k<meas.getNumberOfRounds(1, 0, 0);++k) {
                d += meas.getValue(1, 0, 0, i);
            }
            
            System.out.println(d / meas.getNumberOfRepetitions(1, 0));
            
            
            System.out.println("---------");
            
            System.out.println("+++++++++");
            
            d = 0.0;
            for(int k=0;k<meas.getNumberOfRounds(1, 1, 0);++k) {
                d += meas.getValue(1, 1, 0, i);
            }
            
            System.out.println(d / meas.getNumberOfRepetitions(1, 1));
            
            
            System.out.println("+++++++++");
        }
        
        
        //TODO
    }
    
    @Test
    public void testPick() throws Exception {
        RandomTreeAutomaton rta = new RandomTreeAutomaton(new Well44497b(9398734589L), new String[] {"a","b","c"}, 2.0);
        
        TreeAutomaton<Integer> ta = rta.getRandomAutomaton(20, 2, 2.0);
        
        Tree<Integer> ti = ta.viterbiRaw().getTree();
        
        Tree<Rule> rules = ta.getRuleTree(ti);
        
        EvaluateSamplingFromRules.StatePicker<Integer> st = new EvaluateSamplingFromRules.StatePicker<>(4);
        
        List<Integer> choices = st.pick(ta);
        
        assertEquals(choices.size(),2);
        assertEquals(ta.getIdForState(choices.get(0)),rules.getLabel().getParent());
        assertEquals(ta.getIdForState(choices.get(1)),ta.getIdForState(13));
    }
    
    
    @Test
    public void testMeasurement() {
        EvaluateSamplingFromRules.Measurements<Integer> meas = new EvaluateSamplingFromRules.Measurements<>();
        
        meas.addMeasured(3, 2, 5);
        meas.addMeasured(3, 0, 3);
        meas.addMeasured(1, 5, 3);
        
        meas.addMeasurement(3, 2, 10, 4, 1.5);
        meas.addMeasurement(3, 0, 10, 2, -2.5);
        meas.addMeasurement(1, 5, 2, 2, 0.2);
        
        assertEquals((Integer) 5, meas.getMeasured(3, 2));
        assertEquals((Integer) 3, meas.getMeasured(3, 0));
        assertEquals((Integer) 3, meas.getMeasured(1, 5));
        assertEquals(null, meas.getMeasured(1, 4));
        
        assertEquals(meas.getNumberOfTypes(),4);
        assertEquals(meas.getNumberOfDataSetsForTypeEntry(3),3);
        
        assertEquals(1.5,meas.getValue(3, 2, 10, 4),0.000001);
        assertEquals(-2.5,meas.getValue(3, 0, 10, 2),0.000001);
        assertEquals(0.2,meas.getValue(1, 5, 2, 2),0.000001);
        assertEquals(Double.POSITIVE_INFINITY,meas.getValue(3, 2, 10, 2),0.000001);
        
        assertEquals(5,meas.getNumberOfRounds(3, 2, 10));
        assertEquals(11,meas.getNumberOfRepetitions(3, 2));
        assertEquals(3,meas.getNumberOfDataSets(3));
    }
    
}
