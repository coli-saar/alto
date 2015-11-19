/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.Empty;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.irtg.rule_finding.variable_introduction.VariableIntroduction;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class PruneOneSideTerminatingTest {
    /**
     * 
     */
    private PruneOneSideTerminating pruner;
    
    /**
     * 
     */
    private TreeAutomaton<StringAlgebra.Span> noVar;
    
    /**
     * 
     */
    private TreeAutomaton withVar;
    
    /**
     * 
     */
    private List<AlignedTrees> list;
    
    @Before
    public void setUp() {
        pruner = new PruneOneSideTerminating<>();
        
        StringAlgebra sal = new StringAlgebra();
        noVar = sal.decompose(sal.parseString("a b c d e"));
        
        VariableIntroduction vi = new LeftRightXFromFinite<>();
        SpecifiedAligner spa = new SpecifiedAligner(noVar);
        IntSet set = new IntAVLTreeSet();
        set.add(4);
        set.add(1);
        spa.put(new StringAlgebra.Span(0, 2), set);
        AlignedTrees at = vi.apply(new AlignedTrees(noVar, spa));
        withVar = at.getTrees();
        
        list = new ArrayList<>();
        list.add(at);
        list.add(new AlignedTrees(noVar, spa));
    }

    /**
     * Test of prePrune method, of class PruneOneSideTerminating.
     */
    @Test
    public void testPrePrune() throws Exception {
        Iterable<AlignedTrees> pruned = this.pruner.prePrune(list);
        
        TreeAutomaton ta = pruned.iterator().next().getTrees();
        
        assertTrue(ta.accepts(pt("*(a,*(b,*(c,*(d,e))))")));
        assertTrue(ta.accepts(pt("Xa_e(*(a,*(b,*(c,*(d,e)))))")));
        assertFalse(ta.accepts(pt("*(*(a,b),*(c,*(d,e)))")));
        assertFalse(ta.accepts(pt("*(*(*(a,b),c),*(d,e))")));
        assertTrue(ta.accepts(pt("*(*(a,*(*(b,c),d)),e)")));
        
        assertTrue(ta.accepts(pt("*(a,Xb_e(*(b,*(c,*(d,e)))))")));
        assertTrue(ta.accepts(pt("Xa_e(*(a,Xb_e(*(b,*(c,*(d,e))))))")));
        
        boolean seen = false;
        for(Integer i : ta.getAllStates()){
            Object state  = ta.getStateForId(i);
            
            String s = state.toString();
            if(s.equals("0-2,a,b")){
                seen = true;
                IntSet ins = pruned.iterator().next().getAlignments().getAlignmentMarkers(state);
                assertEquals(ins.size(),2);
                assertTrue(ins.contains(1));
                assertTrue(ins.contains(4));
            }else{
                assertTrue(pruned.iterator().next().getAlignments().getAlignmentMarkers(state).isEmpty());
            }
            
        }
        assertTrue(seen);
        
        Iterator<AlignedTrees> iterator = pruned.iterator();
        iterator.next();
        
        TreeAutomaton t = iterator.next().getTrees();
        assertTrue(t.accepts(pt("*(a,*(*(b,*(c,d)),e))")));
        assertFalse(t.accepts(pt("*(a,*(*(b,c),*(d,e)))")));
        assertTrue(t.accepts(pt("*(*(*(*(a,b),c),d),e)")));
        assertTrue(t.accepts(pt("*(a,*(b,*(c,*(d,e))))")));
    }

    /**
     * Test of postPrune method, of class PruneOneSideTerminating.
     */
    @Test
    public void testPostPrune() {
        assertEquals(this.pruner.postPrune(null, null),null);
        assertEquals(this.pruner.postPrune(list, list),list);
    }
    
}
