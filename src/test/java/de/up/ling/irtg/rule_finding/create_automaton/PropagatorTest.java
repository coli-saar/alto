/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
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
        
        Iterator<Tree<String>> it = t.getTrees().languageIterator();
        System.out.println(t.getTrees());
        System.out.println(t.getTrees().languageIterator().next());
    }    
}
