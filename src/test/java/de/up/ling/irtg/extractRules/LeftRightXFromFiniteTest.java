/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules;

import de.up.ling.irtg.rule_finding.create_automaton.Variables;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.Iterator;
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
     */
    @Test
    public void testApply() throws Exception {
        Pair<TreeAutomaton<Pair<StringAlgebra.Span,Pair<String,String>>>,
                StateAlignmentMarking<Pair<StringAlgebra.Span,Pair<String,String>>>> result;
        result = funct.apply(new Pair<>(this.ta,this.spal));
        
        TreeAutomaton<Pair<StringAlgebra.Span,Pair<String,String>>> aRes = result.getLeft();
        StateAlignmentMarking<Pair<StringAlgebra.Span,Pair<String,String>>> sRes = result.getRight();
        
        Iterable<Rule> it = aRes.getAllRulesTopDown();
        for(Rule r : it){
            String label = aRes.getSignature().resolveSymbolId(r.getLabel());
            Pair<Span,Pair<String,String>> stat = aRes.getStateForId(r.getParent());
            
            
            if(Variables.IS_VARIABLE.test(label)){
                assertEquals(r.getWeight(),1.0,0.0000001);
                assertEquals(label,Variables.makeVariable(stat.getRight().getLeft(),stat.getRight().getRight()));
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
        assertTrue(aRes.accepts(pt("*(X_a_b(X_a_b(*(X_a_a(a),X_b_b(b)))),X_c_d(*(X_c_c(c),X_d_d(X_d_d(d)))))")));
        assertTrue(aRes.accepts(pt("*(a,*(b,*(c,d)))")));
        assertTrue(aRes.accepts(pt("*(a,X_b_d(*(b,*(c,d))))")));
        assertTrue(aRes.accepts(pt("X_a_d(*(a,X_b_d(*(b,*(c,d)))))")));
    }
}
