/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection.arities;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.pruning.intersection.NoEmpty;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import static de.up.ling.irtg.util.TestingTools.pt;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.io.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class EnsureMTAAritiesTest {
    /**
     * 
     */
    private final static String TREES = "p(a,m,b)\n\np(is(where,waldo))";
    
    /**
     * 
     */
    private Object2ObjectMap<String, IntSet> arities;
    
    /**
     * 
     */
    private TreeAutomaton decomp1;
    
    /**
     * 
     */
    private TreeAutomaton decomp2;
    
    @Before
    public void setUp() throws Exception {
        arities = FindArities.find(new ByteArrayInputStream(TREES.getBytes()), 0);
       
        JustXEveryWhere xs = new JustXEveryWhere();
        
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        decomp1 = mta.decompose(mta.parseString("p(a,m,b)"));
        
        AlignedTrees at = new AlignedTrees(decomp1, new SpecifiedAligner(decomp1));
        at = xs.apply(at);
        decomp1 = at.getTrees();
        
        
        decomp2 = mta.decompose(mta.parseString("p(is(where,waldo))"));
        at = new AlignedTrees(decomp2, new SpecifiedAligner(decomp2));
        at = xs.apply(at);
        decomp2 = at.getTrees();
    }

    @Test
    public void testSomeMethod() throws Exception {
        EnsureMTAArities ens1 = new EnsureMTAArities(decomp1.getSignature(), decomp1.getAllLabels(), 3, arities);
        EnsureMTAArities ens2 = new EnsureMTAArities(decomp2.getSignature(), decomp2.getAllLabels(), 3, arities);
        
        assertEquals(ens1.getSignature(),decomp1.getSignature());
        assertEquals(ens2.getSignature(),decomp2.getSignature());
        
        TreeAutomaton ta1 = new IntersectionAutomaton(decomp1, ens1);
        TreeAutomaton ta2 = new IntersectionAutomaton(decomp2, ens2);
        
        ta1 = new IntersectionAutomaton(ta1, new NoEmpty(ta1.getSignature(), ta1.getAllLabels()));
        ta2 = new IntersectionAutomaton(ta2, new NoEmpty(ta2.getSignature(), ta2.getAllLabels()));
        
        assertTrue(ta1.accepts(pt("__RL__(__LR__(a,__LR__(X(m),p)),X(b))")));
        assertFalse(ta1.accepts(pt("__RL__(__LR__(a,X(__LR__(X(m),p))),X(b))")));
        
        
        assertTrue(ta2.accepts(pt("__LR__(__RL__(__LR__(where,is),X(waldo)),p)")));
        assertFalse(ta2.accepts(pt("__LR__(__RL__(__LR__(where,is),X(waldo)),X(p))")));
        assertFalse(ta2.accepts(pt("__LR__(__RL__(X(__LR__(where,is)),X(waldo)),p)")));
        assertTrue(ta2.accepts(pt("__LR__(X(__RL__(__LR__(where,is),X(waldo))),p)")));
    }
    
}
