/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.up.ling.irtg.rule_finding.Variables;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import static de.up.ling.irtg.util.TestingTools.pt;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class LeftRightXFromFiniteTest {
    
    /**
     * 
     */
    private TreeAutomaton<StringAlgebra.Span> ta;
    
    /**
     * 
     */
    private SpanAligner spal;
    
    /**
     * 
     */
    private LeftRightXFromFinite<StringAlgebra.Span> funct;
    
    /**
     * 
     */
    private List<String> words;
    
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        words = sal.parseString("a b c d");
        ta = sal.decompose(words);
        Iterable<Rule> it = ta.getAllRulesTopDown();
        for(Rule r : it){
            StringAlgebra.Span sp = ta.getStateForId(r.getParent());
            r.setWeight(1.0 / (sp.end-sp.start));
        }
        
        
        spal = new SpanAligner("0:1:1 0:4:6 1:2:2 1:2:3 2:3:4 3:4:5", ta);
        
        funct = new LeftRightXFromFinite<>();
    }

    /**
     * Test of apply method, of class LeftRightXFromFinite.
     * @throws java.lang.Exception
     */
    @Test
    public void testApply() throws Exception {
        AlignedTrees<Pair<StringAlgebra.Span,Pair<String,String>>> result;
        AlignedTrees input = new AlignedTrees(ta, spal);
        
        result = funct.apply(input);
        
        TreeAutomaton<Pair<StringAlgebra.Span,Pair<String,String>>> aRes = result.getTrees();
        StateAlignmentMarking<Pair<StringAlgebra.Span,Pair<String,String>>> sRes = result.getAlignments();
        
        Iterable<Rule> it = aRes.getAllRulesTopDown();
        for(Rule r : it){
            String label = aRes.getSignature().resolveSymbolId(r.getLabel());
            Pair<Span,Pair<String,String>> stat = aRes.getStateForId(r.getParent());
            
            if(Variables.IS_VARIABLE.test(label)){
                assertEquals(r.getWeight(),1.0,0.0000001);
                assertEquals(label,Variables.makeVariable(stat.getRight().getLeft()+"_"+stat.getRight().getRight()));
            }else{
                assertEquals(r.getWeight(),1.0/(stat.getLeft().end-stat.getLeft().start),0.00000001);
                
                assertEquals(sRes.getAlignmentMarkers(stat),spal.getAlignmentMarkers(stat.getLeft()));
                
                if(r.getArity() == 0){
                    assertEquals(label,stat.getRight().getLeft());
                    assertEquals(label,stat.getRight().getRight());
                }else{
                    assertEquals(words.get(stat.getLeft().start),stat.getRight().getLeft());
                    assertEquals(words.get(stat.getLeft().end-1),stat.getRight().getRight());
                }
            }
        }
        
        assertTrue(aRes.accepts(pt("*(*(a,b),*(c,d))")));
        assertTrue(aRes.accepts(pt("*(Xa_b(Xa_b(*(Xa_a(a),Xb_b(b)))),Xc_d(*(Xc_c(c),Xd_d(Xd_d(d)))))")));
        assertTrue(aRes.accepts(pt("*(a,*(b,*(c,d)))")));
        assertTrue(aRes.accepts(pt("*(a,Xb_d(*(b,*(c,d))))")));
        assertTrue(aRes.accepts(pt("Xa_d(*(a,Xb_d(*(b,*(c,d)))))")));
    }
}
