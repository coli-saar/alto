/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import com.google.common.base.Function;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntDoubleFunction;
import de.up.ling.tree.Tree;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class TreeAddingAutomatonTest {
    
    /**
     * 
     */
    private TreeAddingAutomaton test;
    
    /**
     * 
     */
    private TreeAutomaton base;
    
    /**
     * 
     */
    private DefaultVariableMapping dvm;
    
    /**
     * 
     */
    private VariableIndicationByLookUp vib;
    
    /**
     * 
     */
    private HomomorphismManager homa;
    
    @Before
    public void setUp() {
        StringAlgebra alg1 = new StringAlgebra();
        StringAlgebra alg2 = new StringAlgebra();
        
        TreeAutomaton t1 = alg1.decompose(alg1.parseString("the first Example"));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString("Example the first"));
        
        
        SpanAligner spa1 = new SpanAligner("0:1:1 1:2:2 2:3:3", t1);
        SpanAligner spa2 = new SpanAligner("0:1:3 1:2:1 2:3:2", t2);
        
        
        Propagator prop = new Propagator(); 
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
        homa.update(t1.getAllLabels(), t2.getAllLabels());
        
        RuleFindingIntersectionAutomaton rfa = new RuleFindingIntersectionAutomaton(t1, t2, homa.getHomomorphism1(), homa.getHomomorphism2());
        base = new TopDownIntersectionAutomaton(rfa, homa.getRestriction());
        
        dvm = new DefaultVariableMapping(homa);
        
        vib = new VariableIndicationByLookUp(homa);
        IntDoubleFunction smooth = (int value) -> {
            if(homa.isVariable(value)){
                return 2.0;
            }else{
                return 1.0;
            }
        };
        
        test = new TreeAddingAutomaton(homa.getSignature(), smooth, vib);
    }

    /**
     * Test of addVariableTree method, of class TreeAddingAutomaton.
     */
    @Test
    public void testAddVariableTree() {
        //System.out.println("+++++++++++++++++++++++");
        //System.out.println(test);
        //System.out.println("+++++++++++++++++++++++");
        
        Iterable<Rule> it = this.base.getAllRulesTopDown();
        for(Rule r : it){
            if(vib.isVariable(r.getLabel())){
                r.setWeight(10.0);
            }else{
                r.setWeight(0.1);
            }
        }
        
        Tree<Integer> choice =  base.viterbiRaw().getTree();
        Function<Integer,Integer> map = (Integer input) -> {
            if(vib.isVariable(input)){
                return homa.getDefaultVariable();
            }else{
                return input;
            }
        };
        
        Tree<Integer> view = choice.map(map);
        
        assertEquals(-40.837867789705705,Math.log(test.getWeightRaw(view)),1.0);
        int size1 = makeSize(test);
        test.addVariableTree(view);
        int size2 = makeSize(test);
        assertEquals(1.0008153942871767,test.getWeightRaw(view),0.0000000001);
        test.addVariableTree(view);
        test.addVariableTree(view);
        test.addVariableTree(view);
        test.addVariableTree(view);
        test.addVariableTree(view);
        test.addVariableTree(view);
        test.addVariableTree(view);
        int size3 = makeSize(test);
        assertEquals(32771.3374752777,test.getWeightRaw(view),0.000000001);
        test.normalizeStart();
        int size4 = makeSize(test);
        assertEquals(2.8289098319980916E-4,test.getWeightRaw(view),0.000000001);
        assertTrue(size1 < size2);
        assertEquals(size2,size3);
        assertEquals(size3,size4);
        //TODO
    }

    /**
     * 
     * @param test
     * @return 
     */
    private int makeSize(TreeAddingAutomaton test) {
        Iterable<Rule> ru = test.getAllRulesTopDown();
        int sum = 0;
        for(Rule r : ru){
            ++sum;
        }
        
        return sum;
    }
}