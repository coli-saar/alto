/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.siblingfinder

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*
import de.up.ling.irtg.algebra.StringAlgebra
import de.up.ling.irtg.algebra.graph.GraphAlgebra
import de.up.ling.irtg.algebra.StringAlgebra.Span
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.signature.Signature

/**
 *
 * @author groschwitz
 */
class SiblingFinderTest {
    
    
    
    @Test
    public void StringSiblingFinderTest() {
        
        TreeAutomaton decomp = (new StringAlgebra()).decompose((new StringAlgebra()).parseString("John loves Mary"));
        decomp.makeAllRulesExplicit();
        SiblingFinder test = decomp.makeNewPartnerFinder(decomp.getSignature().getIdForSymbol("*"));
        
        int span1 = decomp.getIdForState(new Span(0,1));
        int span2 = decomp.getIdForState(new Span(1,2));
        int span3 = decomp.getIdForState(new Span(2,3));
        
        Iterable<int[]> res = test.getPartners(span2, 1);
        assert !res.iterator().hasNext();
        test.addState(span1, 0);
        res = test.getPartners(span2, 1);
        assert res.iterator().hasNext();
        res = test.getPartners(span3, 1);
        assert !res.iterator().hasNext();
        
        res = test.getPartners(span1, 0);
        assert !res.iterator().hasNext();
        test.addState(span2, 1);
        res = test.getPartners(span1, 0);
        assert res.iterator().hasNext();
    }
    
    @Test
    public void GraphSiblingFinderTest() {
        
        Signature graphSig = new Signature();
        graphSig.addSymbol(GraphAlgebra.OP_MERGE, 2);
        int leftS = graphSig.addSymbol("(l / sleep-01 :ARG0 (j <0>))", 0);
        int right0S = graphSig.addSymbol("(j <0> / john )", 0);
        int rightWrongNodeS = graphSig.addSymbol("(l <0> / sleep-01 )", 0);
        int rightWithEdgesS = graphSig.addSymbol("(l / sleep-01 :ARG0 (j <0> / john ))", 0);
        GraphAlgebra alg = new GraphAlgebra(graphSig);
        TreeAutomaton decomp = alg.decompose(alg.parseString("(l / sleep-01 :ARG0 (j / john))"));
        decomp.makeAllRulesExplicit();
        SiblingFinder test = decomp.makeNewPartnerFinder(decomp.getSignature().getIdForSymbol(GraphAlgebra.OP_MERGE));
        
        int left = decomp.getRulesBottomUp(leftS, new int[0]).iterator().next().getParent();
        int right0 = decomp.getRulesBottomUp(right0S, new int[0]).iterator().next().getParent();
        int rightWrongNode = decomp.getRulesBottomUp(rightWrongNodeS, new int[0]).iterator().next().getParent();
        int rightWithEdges = decomp.getRulesBottomUp(rightWithEdgesS, new int[0]).iterator().next().getParent();
        
        
        Iterable<int[]> res = test.getPartners(right0, 1);
        assert !res.iterator().hasNext();
        test.addState(left, 0);
        res = test.getPartners(right0, 1);
        assert res.iterator().hasNext();
        res = test.getPartners(rightWrongNode, 1);
        assert !res.iterator().hasNext();
        res = test.getPartners(rightWithEdges, 1);
        assert !res.iterator().hasNext();
        
        res = test.getPartners(right0, 0);
        assert !res.iterator().hasNext();
        test.addState(left, 1);
        res = test.getPartners(right0, 0);
        assert res.iterator().hasNext();
    }
    
    @Test
    public void TAGSiblingFinderTest() {
        
        // TODO
    }
    
}
