/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.process_align;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class TranslateAlignmentsTest {
    
    /**
     * 
     */
    private TranslateAlignments tal;
    
    @Before
    public void setUp() {
        tal = new TranslateAlignments(new GizaStringAlignments(true), new GizaStringAlignments(false));
    }

    /**
     * Test of transform method, of class TranslateAlignments.
     */
    @Test
    public void testTransform() {
        String firstSentence = "this is the first sentence .";
        String translation = "der erste satz ist dieser .";
        String alignments = "0-4 1-3 2-0 3-1 4-2 5-5";
        
        String goal = "this is the first sentence .\n" +
                      "der erste satz ist dieser .\n" +
                      "0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 5:6:6\n" +
                      "4:5:1 3:4:2 0:1:3 1:2:4 2:3:5 5:6:6";
        
        String result = this.tal.transform(firstSentence, translation, alignments);
        assertEquals(result,goal);
    }
}
