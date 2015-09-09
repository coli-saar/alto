/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.RuleFinder;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFindingIntersectionAutomatonTest {
    
    /**
     * 
     */
    private RuleFindingIntersectionAutomaton rfi;
    
    /**
     * 
     */
    private HomomorphismManager hm;
    
    @Before
    public void setUp() {
        StringAlgebra alg1 = new StringAlgebra();
        StringAlgebra alg2 = new StringAlgebra();
        
        hm = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
        
        TreeAutomaton t1 = alg1.decompose(alg1.parseString("a1 a2"));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString("a1 a2 a3 a4"));
        
        SpanAligner spa1 = new SpanAligner("0:1:1 0:1:2 1:2:3 1:2:4", t1);
        SpanAligner spa2 = new SpanAligner("0:1:3 1:2:4 2:3:1 3:4:2", t2);
        
        Propagator prop = new Propagator();
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        hm.update(t1.getAllLabels(), t2.getAllLabels());
        
        this.rfi = new RuleFindingIntersectionAutomaton(t1, t2, hm.getHomomorphism1(), hm.getHomomorphism2());
    }

    /**
     * Test of supportsBottomUpQueries method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testSupportsBottomUpQueries() {
        assertFalse(this.rfi.supportsBottomUpQueries());
    }

    /**
     * Test of supportsTopDownQueries method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testSupportsTopDownQueries() {
        assertTrue(this.rfi.supportsTopDownQueries());
    }

    /**
     * Test of getRulesBottomUp method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testGetRulesBottomUp() {
        boolean sawError = false;
        
        try{
            this.rfi.getRulesBottomUp(1, new int[0]);
        }catch(UnsupportedOperationException unsup){
            sawError = true;
        }
        
        assertTrue(sawError);
    }

    /**
     * Test of getRulesTopDown method, of class RuleFindingIntersectionAutomaton.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRulesTopDown() throws Exception {
        TopDownIntersectionAutomaton tdi = new TopDownIntersectionAutomaton(rfi, hm.getRestriction());
        
        Set<Pair<Tree<String>,Tree<String>>> expected = new HashSet<>();
        
        expected.add(new Pair<>(pt("*(a1,a2)"),pt("*(*(*(a1,a2),a3),a4)")));
        expected.add(new Pair<>(pt("*(a1,a2)"),pt("*(a1,*(*(a2,a3),a4))")));
        expected.add(new Pair<>(pt("*('XX_{1, 2}'(a1),a2)"),pt("*(*(a1,a2),'XX_{1, 2}'(*(a3,a4)))")));
        expected.add(new Pair<>(pt("*(a1,a2)"),pt("*(a1,*(a2,*(a3,a4)))")));
        expected.add(new Pair<>(pt("*('XX_{1, 2}'(a1),'XX_{3, 4}'(a2))"),pt("*('XX_{3, 4}'(*(a1,a2)),'XX_{1, 2}'(*(a3,a4)))")));
        expected.add(new Pair<>(pt("*(a1,a2)"),pt("*(*(a1,a2),*(a3,a4))")));
        expected.add(new Pair<>(pt("*(a1,'XX_{3, 4}'(a2))"),pt("*('XX_{3, 4}'(*(a1,a2)),*(a3,a4))")));
        expected.add(new Pair<>(pt("*(a1,a2)"),pt("*(*(a1,*(a2,a3)),a4)")));
        expected.add(new Pair<>(pt("*(a1,'XX_{3, 4}'(a2))"),pt("*(*('XX_{3, 4}'(*(a1,a2)),a3),a4)")));
        expected.add(new Pair<>(pt("*('XX_{1, 2}'(a1),a2)"),pt("*(a1,*(a2,'XX_{1, 2}'(*(a3,a4))))")));
        
        
        for(Tree<String> t : tdi.language()){
            assertTrue(hm.getRestriction().accepts(t));
            
            Pair<Tree<String>,Tree<String>> p = new Pair<>(hm.getHomomorphism1().apply(t),hm.getHomomorphism2().apply(t));
            assertTrue(expected.contains(p));
        }
        
        assertEquals(tdi.language().size(),10);
        
        StringAlgebra alg1 = new StringAlgebra();
        StringAlgebra alg2 = new StringAlgebra();
        
        hm = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
        
        String one = "a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 "
                + "a16 a17 a18 a19 a20 a21 a22 a23 a24 a25 "
                + "a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15";
       
        String two = "a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 "
                + "a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16"
                + " a17 a18 a19 a20";
        
        TreeAutomaton t1 = alg1.decompose(alg1.parseString(one));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString(two));
        
        SpanAligner spa1 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 5:6:6", t1);
        SpanAligner spa2 = new SpanAligner("0:1:1 0:1:2 0:1:3 1:2:4 1:2:5 1:2:6", t2);
        
        Propagator prop = new Propagator();
        
        t1 = makeSample(t1, 10);
        
        t2 = makeSample(t2, 10);
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        hm.update(t1.getAllLabels(), t2.getAllLabels());
        
        this.rfi = new RuleFindingIntersectionAutomaton(t1, t2, hm.getHomomorphism1(), hm.getHomomorphism2());
        tdi = new TopDownIntersectionAutomaton(rfi, hm.getRestriction().asConcreteTreeAutomaton());
        
        for(int i=0;i<50;++i){
            Tree<String> t = tdi.getRandomTree();
            if(containsNull(t)){
                continue;
            }
            
            SingletonAutomaton st = new SingletonAutomaton(t);
            
            RuleFinder rf = new RuleFinder();
            InterpretedTreeAutomaton ita = rf.getInterpretation(st, hm, alg1, alg2);
            
            Interpretation pret1 = ita.getInterpretation("left");
            Interpretation pret2 = ita.getInterpretation("right");
            
            assertEquals(alg1.parseString(one),pret1.interpret(t));
            assertEquals(alg2.parseString(two),pret2.interpret(t));
        }
    }

    /**
     * 
     * @param t
     * @return 
     */
    private TreeAutomaton makeSample(TreeAutomaton t, int samps) {
        FromRuleTreesAutomaton sample = new FromRuleTreesAutomaton(t);
        for(int i=0;i<samps;++i){
            Tree<Rule> samp = t.getRandomRuleTreeFromInside();
            sample.addRules(samp);
        }
        
        return sample;
    }

    /**
     * Test of isBottomUpDeterministic method, of class RuleFindingIntersectionAutomaton.
     */
    @Test
    public void testIsBottomUpDeterministic() {        
        assertFalse(this.rfi.isBottomUpDeterministic());
    }   

    private boolean containsNull(Tree<String> t) {
        if(t== null || t.getLabel() == null){
            return true;
        }
        
        for(Tree<String> q : t.getChildren()){
            if(containsNull(q)){
                return true;
            }
        }
        
        return false;
    }
}