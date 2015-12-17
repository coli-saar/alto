/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class TreeTopTest {
    
    /**
     * 
     */
    private TreeTop tt;
    
    /**
     * 
     */
    private AlignedTrees at;
    
    
    @Before
    public void setUp() throws ParserException {
        tt = new TreeTop();
        
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        TreeAutomaton ta = mta.decompose(mta.parseString("al(hj_2(t),q)"));
        
        SpecifiedAligner spec = new SpecifiedAligner(ta);
        Object state = ta.getStateForId(ta.getFinalStates().iterator().next());
        IntSet al = new IntOpenHashSet();
        al.add(5);
        al.add(7);
        spec.put(state, al);
        
        at = new AlignedTrees(ta, spec);
    }

    /**
     * Test of apply method, of class TreeTop.
     */
    @Test
    public void testApply() throws Exception {
        AlignedTrees qt = this.tt.apply(at);
        
        IntIterator iit = qt.getTrees().getAllStates().iterator();
        while(iit.hasNext()) {
            int state = iit.nextInt();
            Object stat = qt.getTrees().getStateForId(state);
            
            IntSet isOne = qt.getAlignments().getAlignmentMarkers(stat);
            IntSet isTwo = at.getAlignments().getAlignmentMarkers(stat);
            assertTrue(isOne == isTwo);
        }
        
        System.out.println(qt.getTrees());
        assertTrue(qt.getTrees().accepts(pt("__LR__(__LR__(t,hj_2),'Xal||treeType'(__LR__('Xq||term'(q),al)))")));
        assertTrue(qt.getTrees().accepts(pt("'Xal||treeType'(__LR__(__LR__(t,'hj_2'),'Xal||treeType'(__LR__('Xq||term'(q),al))))")));
        assertTrue(qt.getTrees().accepts(pt("'Xal||treeType'(__LR__('Xhj_2||treeType'(__RL__('hj_2',t)),'Xal||treeType'(__LR__('Xq||term'(q),al))))")));
    }
    
}
