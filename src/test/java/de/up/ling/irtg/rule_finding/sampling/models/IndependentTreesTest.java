/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class IndependentTreesTest {
    /**
     * 
     */
    private Tree<Rule> imp1;
    
    /**
     * 
     */
    private Tree<Rule> imp2;
    
    /**
     * 
     */
    private IndependentTrees ins;
    
    /**
     * 
     */
    private InterpretedTreeAutomaton ita;

    /**
    *
    */
    private final static String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "a very bad example";

    /**
     *
     */
    private final static String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "ein sehr schlechtes Beispiel";

    /**
     *
     */
    private final static String alignments = "0-0 1-1 2-2 3-3";
       
    @Before
    public void setUp() throws Exception {
        Pruner p1 = new IntersectionPruner(IntersectionOptions.LEXICALIZED,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        Pruner p2 = new IntersectionPruner(IntersectionOptions.LEXICALIZED,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        
        Iterable<String> grammars = ExtractionHelper.getStringIRTGs(leftTrees, rightTrees, alignments, p1, p2);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        this.ita = iic.read(new ByteArrayInputStream(grammars.iterator().next().getBytes()));

        TreeAutomaton solution = ita.getAutomaton();
        Iterable<Rule> rules = solution.getAllRulesTopDown();
        for(Rule r : rules) {
            r.setWeight(0.9);
        }

        Iterator<Tree<Integer>> lang = solution.languageIteratorRaw();
        Tree<Integer> t1 = lang.next();
        Tree<Integer> t2 = lang.next();
        
        imp1 = solution.getRuleTree(t1);
        imp2 = solution.getRuleTree(t2);
        
        List<Signature> list = new ArrayList<>();
        list.add(ita.getAutomaton().getSignature());
        
        this.ins = new IndependentTrees(0.5, list);
    }

    /**
     * Test of getLogWeight method, of class IndependentSides.
     */
    @Test
    public void testGetLogWeight() {
        double d1;
        double d2;
        
        d1 = this.ins.getLogWeight(imp1,this.ita);
        d2 = this.ins.getLogWeight(imp2,this.ita);
        
        this.ins.add(this.imp1, ita, 200.0);
        
        double d3;
        double d4;
        d3 = this.ins.getLogWeight(imp1, ita);
        d4 = this.ins.getLogWeight(imp2, ita);
        
        assertTrue(d3 > d1);
        
        this.ins.add(this.imp1, ita, -200.0);
        
        assertEquals(d1,this.ins.getLogWeight(imp1, ita),0.00001);
        assertEquals(this.ins.getLogWeight(imp2, ita),d2,0.00001);
    }
}
