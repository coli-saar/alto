/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.process_align;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class GizaTreePreOrderAlignmentsTest {
    /**
     * 
     */
    private GizaTreePreOrderAlignments gtp;
    
    
    @Before
    public void setUp() {
        gtp = new GizaTreePreOrderAlignments(false);
    }

    /**
     * Test of apply method, of class GizaTreePreOrderAlignments.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testApply() throws ParserException {
        String line = "answer(count(intersection(city(cityid('austin', _)), loc_2(countryid('usa')))))";
        String align = "0-0 1-1 1-2 2-3 3-4 4-5 10-6 7-7 9-8 9-9";
        
        String result = this.gtp.apply(line, align);
        assertEquals(result,"0-0-0:1 0-0-0-0:2 0-0-0-0-0:3 0-0-0-0-0-0:4 0-0-0-0-0-0-0:5 0-0-0-0-0-0-0-0:6 0-0-0-0-0-0-0-1:7 0-0-0-0-0-1:8 0-0-0-0-0-1-0:9 0-0-0-0-0-1-0-0:10");

        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        TreeAutomaton ta = mta.decompose(mta.parseString("answer(population_1(cityid('austin', 'tx')))"));
        AddressAligner al = new AddressAligner(ta, this.gtp.apply("answer(population_1(cityid('austin', 'tx')))", "0-0 2-1 3-2 5-3 5-4"));
        
        for(Rule r : (Iterable<Rule>) ta.getAllRulesTopDown()){
            Object state = ta.getStateForId(r.getParent());
            IntSet ins = (al.getAlignmentMarkers((String) state));
            
            if(r.getArity() != 0){
                assertTrue(ins.isEmpty());
            }else{
                switch(ta.getSignature().resolveSymbolId(r.getLabel())){
                    case "austin":
                        assertTrue(ins.contains(4));
                        assertEquals(ins.size(),1);
                        break;
                    case "tx":
                        assertTrue(ins.contains(5));
                        assertEquals(ins.size(),1);
                        break;
                }
            }
        }
    }
}
