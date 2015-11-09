/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import static de.up.ling.irtg.util.TestingTools.pt;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class PropagatorTest {
    
    /**
     * 
     */
    private AlignedTrees pair;
    
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        
        List<AlignedTrees> l = new ArrayList<>();
        
        TreeAutomaton decomp = sal.decompose(sal.parseString("This is a test case ."));
        
        StateAlignmentMarking spa = new SpanAligner("1:2:14 0:6:44 5:6:2", decomp);
        
        LeftRightXFromFinite lrf = new LeftRightXFromFinite();
        l.add(new AlignedTrees(decomp,spa));
        
        this.pair = lrf.apply(l.get(0));
    }

    /**
     * Test of propagate method, of class Propagator.
     * @throws java.lang.Exception
     */
    @Test
    public void testPropagate() throws Exception {
        Propagator prop = new Propagator();
        
        AlignedTrees t = prop.convert(pair);
        
        TreeAutomaton ta = t.getTrees();
        assertTrue(ta.accepts(pt("*(*(*('X{}_XThis_This'(This),is),a),*(*(test,case),'.'))")));
        assertFalse(ta.accepts(pt("*(*(*('X{1}_XThis_This'(This),is),a),*(*(test,case),'.'))")));
        assertTrue(ta.accepts(pt("*(*('X{14}_XThis_is'(*(This,is)),a),*(*(test,case),'.'))")));
        assertTrue(ta.accepts(pt("'X{2, 14, 44}_XThis_.'(*(*('X{14}_XThis_is'(*(This,is)),a),*(*(test,case),'.')))")));
        
        int thi = ta.getSignature().getIdForSymbol("This");
        int left = ((Rule) ta.getRulesBottomUp(thi, new int[0]).iterator().next()).getParent();
        
        int is = ta.getSignature().getIdForSymbol("is");
        int right = ((Rule) ta.getRulesBottomUp(is, new int[0]).iterator().next()).getParent();
        
        int star = ta.getSignature().getIdForSymbol("*");
        int both = ((Rule) ta.getRulesBottomUp(star, new int[] {left,right}).iterator().next()).getParent();
        Object o = ta.getStateForId(both);
        
        IntSet set = t.getAlignments().getAlignmentMarkers(o);
        assertEquals(set.size(),1);
        assertTrue(set.contains(14));
        
        o = ta.getStateForId(ta.getFinalStates().iterator().nextInt());
        set = t.getAlignments().getAlignmentMarkers(o);
        assertEquals(set.size(),3);
        assertTrue(set.contains(14));
        assertTrue(set.contains(44));
        assertTrue(set.contains(2));
    }
    
    @Test
    public void getOriginalInformation(){
        IntSet ins1 = new IntAVLTreeSet();
        IntSet ins2 = new IntAVLTreeSet();
        
        ins1.add(5);
        ins1.add(2);
        
        String s1 = Propagator.makeExtendedVariable("Xa_b", ins1);
        String s2 = Propagator.makeExtendedVariable("Xff32", ins2);
        
        assertEquals(s1,"X{2, 5}_Xa_b");
        assertEquals(s2,"X{}_Xff32");
        
        assertEquals(Propagator.getAlignments(s1),"{2, 5}");
        assertEquals(Propagator.getAlignments(s2),"{}");
        
        assertEquals(Propagator.getOriginalVariable(s1),"Xa_b");
        assertEquals(Propagator.getOriginalVariable(s2),"Xff32");
    }
}
