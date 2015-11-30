/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.Iterator;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RulePostProcessingTest {
    
    /**
     * 
     */
    private Homomorphism im1;
    
    /**
     * 
     */
    private Homomorphism im2;
    
    /**
     * 
     */
    private Tree<String> input1;
    
    /**
     * 
     */
    private StringSubtreeIterator.VariableMapping vm;
    
    /**
     * 
     */
    private TreeAlgebra alg1;
    
    /**
     * 
     */
    private TreeAlgebra alg2;
    
    @Before
    public void setUp() throws Exception {
        alg1 = new TreeAlgebra();
        alg2 = new TreeAlgebra();
        
        Signature source = new Signature();
        im1 = new Homomorphism(source, alg1.getSignature());
        im2 = new Homomorphism(source, alg2.getSignature());
        
        
        String conCon = "* / *";
        source.addSymbol(conCon, 2);
        im1.add(conCon, pt("con(u(?2),v(?1))"));
        im2.add(conCon, pt("cat(?1,?2,?2)"));
        
        String x1 = "X943";
        source.addSymbol(x1, 1);
        String x2 = "Xjjkk";
        source.addSymbol(x2, 1);
        
        String ab = "a / b";
        source.addSymbol(ab,0);
        im1.add(ab, pt("a"));
        im2.add(ab, pt("b"));
        
        String sing = "uni / sing";
        source.addSymbol(sing,1);
        im1.add(sing, pt("JJ(UU(?1))"));
        im2.add(sing, pt("k(l)"));
        
        Tree<String> term = Tree.create(ab);
        Tree<String> var1Term = Tree.create(x1, term);
        Tree<String> loopTerm = Tree.create(sing, term);
        Tree<String> var2loopTerm = Tree.create(x2,loopTerm);
        Tree<String> con1 = Tree.create(conCon, var2loopTerm,var1Term);
        Tree<String> varCon = Tree.create(x1, con1);
        input1 = Tree.create(conCon, varCon,var1Term);
        
        
        vm = new StringSubtreeIterator.VariableMapping() {

            @Override
            public String getRoot(Tree<String> whole) {
                return "START";
            }

            @Override
            public String get(Tree<String> child, Tree<String> whole) {
                return Variables.makeVariable(child.getLabel().substring(1,3));
            }
        };
    }

    /**
     * Test of addRule method, of class RulePostProcessing.
     * @throws java.lang.Exception
     */
    @Test
    public void testAddRule() throws Exception {
        Iterator<Tree<String>> it = StringSubtreeIterator.getSubtrees(input1, vm);
        RulePostProcessing rpp = new RulePostProcessing(alg1, alg2);
        
        boolean first = true;
        while(it.hasNext()){
            Tree<String> t = it.next();
            rpp.addRule(t, im1, im2, first);
            first = false;
        }
        
        TreeAutomaton ta = rpp.getAutomaton();
        ta.normalizeRuleWeights();
        Tree<String> best = ta.viterbi();
        assertEquals(best,pt("'START_1_[X94, X94]'('X94_2_[]','X94_2_[]')"));
        assertEquals(rpp.getFirstImage().apply(best),pt("con(u(a),v(a))"));
        assertEquals(rpp.getSecondImage().apply(best),pt("cat(b,b,b)"));
        
        int parent = ta.getIdForState("X94");
        Iterable<Rule> rules = ta.getRulesTopDown(parent);
        
        boolean twoThirds = false;
        boolean oneThird = false;
        int count = 0;
        for(Rule r : rules){
            if(Math.abs(r.getWeight() - (2.0/3.0)) - 0.0000001 < 0){
                twoThirds = true;
            }else if(Math.abs(r.getWeight() - (1.0/3.0)) - 0.0000001 < 0){
                oneThird = true;
            }
            
            ++count;
        }
        
        assertEquals(count,2);
        assertTrue(twoThirds);
        assertTrue(oneThird);
        
        InterpretedTreeAutomaton ita = rpp.getIRTG("tree one", "tree two");
        Map<String,Object> interpretations =  ita.interpret(best);
        
        assertEquals(interpretations.get("tree one"),pt("con(u(a),v(a))"));
        assertEquals(interpretations.get("tree two"),pt("cat(b,b,b)"));
    }
}
