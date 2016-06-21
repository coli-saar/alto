/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.rule_weighting;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class SubtreeCountingTest {
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
    
    /**
     * 
     */
    private SubtreeCounting.CentralCounter cc;
    
    /**
     * 
     */
    private SubtreeCounting sc;
    
    /**
     * 
     */
    private Tree imp1;
    
    /**
     * 
     */
    private Tree imp2;
    
    @Before
    public void setUp() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException, Exception {
        Pruner p1 = new IntersectionPruner(IntersectionOptions.LEXICALIZED,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        Pruner p2 = new IntersectionPruner(IntersectionOptions.LEXICALIZED,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        
        Iterable<String> grammars = ExtractionHelper.getStringIRTGs(leftTrees, rightTrees, alignments, p1, p2);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        
        Signature sig = new Signature();
        this.ita = iic.read(new ByteArrayInputStream(grammars.iterator().next().getBytes()),sig);

        assertTrue(ita.getAutomaton().getSignature() == sig);
        
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
        
        this.cc = new SubtreeCounting.CentralCounter(3.0, 1.0, sig);
        
        this.sc = new SubtreeCounting(ita, 3, 100, new AdaGrad(0.5), cc);
    }

    /**
     * Test of getLogTargetProbability method, of class SubtreeCounting.
     */
    @Test
    public void testGetLogTargetProbability() {
        double d1 = this.cc.getLogProbability(this.imp1, ita);
        double c1 = this.sc.getLogTargetProbability(imp1);
        
        double e1 = this.cc.getLogProbability(imp2, ita);
        
        assertEquals(d1,-67.60573108918678,0.00000001);
        assertEquals(d1,c1,0.00000001);
        
        this.cc.add(imp1, ita.getAutomaton().getSignature(), 1);
        double d2 = this.cc.getLogProbability(imp1, ita);
        
        double e2 = this.cc.getLogProbability(imp2, ita);
        
        assertTrue(e2 != e1);
        assertEquals(d2,-5.544811255891985,0.0000000001);
        
        this.cc.add(imp1, ita.getAutomaton().getSignature(), -1);
        double d3 = this.cc.getLogProbability(imp1, ita);
        double e3 = this.cc.getLogProbability(imp2, ita);
        
        assertEquals(d1,d3,0.000000001);
        assertEquals(e3,e1,0.000000001);
    }
}
