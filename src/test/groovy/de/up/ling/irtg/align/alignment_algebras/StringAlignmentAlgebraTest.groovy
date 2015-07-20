/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.alignment_algebras;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.align.RuleMarker
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntIterator
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import de.saar.basic.Pair

/**
 *
 * @author christoph_teichmann
 */
public class StringAlignmentAlgebraTest {
    
    /**
     * 
     */
    private StringAlignmentAlgebra saa;
    
    
    @Before
    public void setUp() {
        saa = new StringAlignmentAlgebra();
    }
 
    /**
     * Test of addPair method, of class SimpleRuleMarker.
     */
    @Test
    public void testDecompose() {
        String one = "a:17 b:3 c:4 d:2";
        String two = "c:4 d:2 a:17 b:3 h";
        
        Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> result = saa.decomposePair(one, two);
        
        RuleMarker rlm = result.getLeft();
        
        TreeAutomaton t1 = result.getRight().getLeft();
        TreeAutomaton t2 = result.getRight().getRight();
        
        for(Rule r : t1.getRuleSet()){
                       
            if(r.getLabel(t1).equals("*") || r.getLabel(t1).equals("h")){
                assertTrue(rlm.getMarkings(0,r).isEmpty());
            }else{
                String label = r.getLabel(t1);
                int code = t2.getSignature().getIdForSymbol(label);
                Rule r2 = t2.getRulesBottomUp(code,[]).iterator().next();
                
                IntSet is1 = rlm.getMarkings(0,r);
                IntSet is2 = rlm.getMarkings(1,t2.getRulesBottomUp(code,[]).iterator().next());
                
                assertEquals(is1.size(),1);
                assertEquals(is2.size(),1);
                assertTrue(is1.contains(is2.iterator().nextInt()));
            }
        }
    }
}