/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class TreeSampleTest {
    /**
     * 
     */
    private TreeSample<String> sample;
    
    /**
     * 
     */
    private Tree<String> t1;
    
    /**
     * 
     */
    private Tree<String> t2;
    
    /**
     * 
     */
    private Tree<String> t3;
    
    @Before
    public void setUp() throws ParseException {
        sample = new TreeSample<>();
        
        t1 = null;
        t2 = TreeParser.parse("ex(am(ple))");
        t3 = TreeParser.parse("oth(e,r)");
    }

    /**
     * Test of addSample method, of class TreeSample.
     */
    @Test
    public void testAddSample() {
        sample.addSample(t1);
        sample.addSample(t2);
        sample.addSample(t3);
        sample.addSample(t2);
        
        assertEquals(sample.getSample(0),t1);
        assertEquals(sample.getSample(1),t2);
        assertEquals(sample.getSample(2),t3);
        assertEquals(sample.getSample(3),t2);
        
        assertEquals(sample.populationSize(),4);
        
        sample.setLogPropWeight(0, -2.0);
        sample.setLogPropWeight(1, -4.0);
        sample.setLogPropWeight(2, -15.0);
        sample.setLogPropWeight(3, -7.0);
        
        assertEquals(sample.getLogPropWeight(0),-2.0,0.000001);
        assertEquals(sample.getLogPropWeight(1),-4.0,0.000001);
        assertEquals(sample.getLogPropWeight(2),-15.0,0.000001);
        assertEquals(sample.getLogPropWeight(3),-7.0,0.000001);
        
        sample.setLogTargetWeight(0, -1.0);
        sample.setLogTargetWeight(1, -0.5);
        sample.setLogTargetWeight(2, -1.0);
        sample.setLogTargetWeight(3, -0.1);
        
        assertEquals(sample.getLogTargetWeight(0),-1.0,0.00001);
        assertEquals(sample.getLogTargetWeight(1),-0.5,0.00001);
        assertEquals(sample.getLogTargetWeight(2),-1.0,0.00001);
        assertEquals(sample.getLogTargetWeight(3),-0.1,0.00001);
        
        sample.setLogSumWeight(0, -1.5);
        sample.setLogSumWeight(1, -3.0);
        sample.setLogSumWeight(2, -12.0);
        sample.setLogSumWeight(3, -6.8);
        
        assertEquals(sample.getLogSumWeight(0),-1.5,0.0000001);
        assertEquals(sample.getLogSumWeight(1),-3,0.0000001);
        assertEquals(sample.getLogSumWeight(2),-12.0,0.0000001);
        assertEquals(sample.getLogSumWeight(3),-6.8,0.0000001);
 
        double d = sample.makeMaxBase(true, 0.0);
        assertEquals(d,14.0,0.000001);
        
        assertEquals(sample.getSelfNormalizedWeight(2),1.0,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(0),2.2603294069810542E-6,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(1),2.7536449349747158E-5,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(3),8.251049232659046E-4,0.00000001);
        
        
        d = sample.makeMaxBase(false, 20.0);
        assertEquals(d,20.0,0.00000001);
        
        assertEquals(sample.getSelfNormalizedWeight(2),1.2340980408667956E-4,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(0),3.398267819495071E-9,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(1),2.510999155743982E-8,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(3),1.674493209434266E-6,0.00000001);
        
        sample.expoNormalize(true);
        
        assertEquals(sample.getSelfNormalizedWeight(0),2.258398698090211E-6,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(1),2.7512928500344583E-5,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(2),0.9991458285306203,0.00000001);
        assertEquals(sample.getSelfNormalizedWeight(3),8.244001421812061E-4,0.00000001);
        
        sample.resampleWithNormalize(new Well44497b(879479853234234L), 20, false);
        
        assertEquals(sample.getSample(0),t3);
        assertEquals(sample.getSample(1),t3);
        assertEquals(sample.populationSize(),2);
        
        assertEquals(sample.getSelfNormalizedWeight(0),0.95,0.000000001);
        assertEquals(sample.getSelfNormalizedWeight(1),0.05,0.000000001);
    }
}
