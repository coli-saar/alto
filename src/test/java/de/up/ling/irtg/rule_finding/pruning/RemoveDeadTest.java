/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveDeadTest {
    
    /**
     * 
     */
    private TreeAutomaton ta;
    
    @Before
    public void setUp() {
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>();
        cta.getFinalStates().add(cta.addState("START"));
        
        cta.addRule(cta.createRule("START","1",new String[] {"A","B"}));
        cta.addRule(cta.createRule("A", "2", new String[] {}));
        cta.addRule(cta.createRule("B", "3", new String[] {}));
        
        cta.addRule(cta.createRule("A","4",new String[] {"C","C"}));
        cta.addRule(cta.createRule("C","5",new String[] {"B","D"}));
        cta.addRule(cta.createRule("C","6",new String[] {"D"}));
        
        cta.addRule(cta.createRule("D","7",new String[] {"E"}));
        
        ta = cta;
    }

    /**
     * Test of reduce method, of class RemoveDead.
     */
    @Test
    public void testReduce() {
        TreeAutomaton red = RemoveDead.reduce(this.ta);
        
        assertEquals(red.language(),ta.language());
    }
    
}
