/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.Propagator;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import static de.up.ling.irtg.util.TestingTools.pt;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class IntersectionPrunerTest {
    
    /**
     * 
     */
    private AlignedTrees at;
    
    /**
     * 
     */
    private IntersectionPruner rbp;
    
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        
        TreeAutomaton aut = sal.decompose(sal.parseString("a b c d e"));
        SpanAligner spal = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4 4:5:5", aut);
        
        at = new AlignedTrees(aut, spal);
        
        rbp = new IntersectionPruner<>((TreeAutomaton ta) -> new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels()));
    }

    /**
     * Test of prePrune method, of class RightbranchingPuner.
     */
    @Test
    public void testPrePrune() {
        List<AlignedTrees<Object>> l = new ArrayList<>();
        l.add(at);
        
        assertEquals(l,this.rbp.prePrune(l));
    }

    /**
     * Test of postPrune method, of class RightbranchingPuner.
     * @throws java.lang.Exception
     */
    @Test
    public void testPostPrune() throws Exception {
        Propagator pg = new Propagator();
        at= new JustXEveryWhere().apply(at);
        AlignedTrees ab = pg.convert(at);
        
        List<AlignedTrees<Object>> l = new ArrayList<>();
        l.add(ab);
        
        Iterable<AlignedTrees<Object>> result = rbp.postPrune(l,null);
        assertEquals(l.size(),1);
        
        for(Rule r : (Iterable<Rule>) result.iterator().next().getTrees().getAllRulesTopDown()){
            Pair<Object,Object> p = (Pair<Object,Object>) result.iterator().next().getTrees().getStateForId(r.getParent());
            assertEquals(result.iterator().next().getAlignments().getAlignmentMarkers(p),ab.getAlignments().getAlignmentMarkers(p.getLeft()));
        }
        
        assertTrue(result.iterator().next().getTrees().accepts(pt("*('X{1, 2}_X'('X{1, 2}_X'(*(a,b))),*(c,*(d,e)))")));
        assertFalse(result.iterator().next().getTrees().accepts(pt("*(*(a,b),*(c,*(d,e)))")));
    }
    
}
