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
        /*
        sample.addSample(t1, 2.3);
        sample.addSample(t2, 0.7);
        sample.addSample(t3, 2);
        sample.addSample(t2, -1.2);
        
        assertEquals(sample.getSample(0),t1);
        assertEquals(sample.getSample(1),t2);
        assertEquals(sample.getSample(2),t3);
        assertEquals(sample.getSample(3),t2);
        
        assertEquals(sample.getNormalized(0),2.3,0.000001);
        assertEquals(sample.getNormalized(1),0.7,0.000001);
        assertEquals(sample.getNormalized(2),2,0.000001);
        assertEquals(sample.getNormalized(3),-1.2,0.000001);
        
        sample.addWeight(1, -4.0);
        assertEquals(sample.getNormalized(1),-3.3,0.000001);
        
        sample.multiplyWeight(0, 2);
        assertEquals(sample.getNormalized(0),4.6,0.0000001);
        
        sample.multiplyWeight(2, 2);
        
        assertEquals(sample.populationSize(),4);
        
        RandomGenerator rg = new Well44497b(91919191919191L);
        sample.resampleWithNormalize(rg,20);
        
        assertEquals(sample.getSample(0),t1);
        assertEquals(sample.getSample(1),t3);
        assertEquals(sample.getSample(2),t1);
        
        assertEquals(sample.getNormalized(0),0.6,0.00000001);
        assertEquals(sample.getNormalized(1),0.35,0.00000001);
        assertEquals(sample.getNormalized(2),0.05,0.00000001);
        
        assertEquals(sample.populationSize(),3);
        */
    }
}
