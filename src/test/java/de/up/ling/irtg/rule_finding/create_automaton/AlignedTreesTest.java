/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class AlignedTreesTest {
    
    /**
     * 
     */
    private AlignedTrees<StringAlgebra.Span> align;
    
    @Before
    public void setUp() throws ParserException {
        Algebra<List<String>> alg = new StringAlgebra();
        TreeAutomaton<StringAlgebra.Span> aut = alg.decompose(alg.parseString("John goes ."));
        
        StateAlignmentMarking<StringAlgebra.Span> mark =
                    new SpanAligner("0:1:1 1:2:2 2:3:3", aut);
        
        this.align = new AlignedTrees<>(aut,mark);
    }

    /**
     * Test of getTrees method, of class AlignedTrees.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetTrees() throws Exception {
        TreeAutomaton<StringAlgebra.Span> tr = this.align.getTrees();
        
        Set<Tree<String>> trees = tr.language();
        
        assertEquals(trees.size(),2);
        assertTrue(trees.contains(pt("*(*(John,goes),'.')")));
        assertTrue(trees.contains(pt("*(John,*(goes,'.'))")));
        
        IntSet states = tr.getAllLabels();
        IntIterator iit = states.iterator();
        
        StateAlignmentMarking<StringAlgebra.Span> marking = this.align.getAlignments();
        
        while(iit.hasNext()){
            int state = iit.nextInt();
            StringAlgebra.Span span = tr.getStateForId(state);
            IntSet ins = marking.getAlignmentMarkers(span);
            
            if(span.start == 0 && span.end == 1){
                assertEquals(ins.size(),1);
                assertTrue(ins.contains(1));
            }else if(span.start == 1 && span.end == 2){
                assertEquals(ins.size(),1);
                assertTrue(ins.contains(2));
            }else if(span.start == 2 && span.end == 3){
                assertEquals(ins.size(),1);
                assertTrue(ins.contains(3));
            }else{
                assertTrue(ins.isEmpty());
            }
        }
    }
}
