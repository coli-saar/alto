/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.Lexicalized;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;
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
    private TreeAutomaton solution;
    
    /**
     * 
     */
    private HomomorphismManager homm;
    
    /**
     * 
     */
    private IntPredicate choices;
    
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
    
    @Before
    public void setUp() throws Exception {
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            TreeAutomaton a = new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());

            Lexicalized lexicalized = new Lexicalized(a.getSignature(), a.getAllLabels());

            return new IntersectionAutomaton(a, lexicalized);
        }));
        fact.setSecondPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            TreeAutomaton a = new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());

            Lexicalized lexicalized = new Lexicalized(a.getSignature(), a.getAllLabels());

            return new IntersectionAutomaton(a, lexicalized);
        }));
        
        fact.setFirstVariableSource(new JustXEveryWhere());
        fact.setSecondVariableSource(new JustXEveryWhere());

        CorpusCreator<String, String> corp = fact.getInstance(null, null, null, null);

        List<AlignedTrees> stringList1 = new ArrayList<>();
        List<AlignedTrees> stringList2 = new ArrayList<>();

        StringAlgebra string1 = new StringAlgebra();
        StringAlgebra string2 = new StringAlgebra();
        
        TreeAutomaton stringAut1
                = string1.decompose(string1.parseString("a very bad example"));
        SpanAligner span1 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4", stringAut1);

        TreeAutomaton stringAut2
                = string2.decompose(string2.parseString("ein sehr schlechtes Beispiel"));
        SpanAligner span2 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4", stringAut2);

        AlignedTrees sat1 = new AlignedTrees(stringAut1, span1);
        AlignedTrees sat2 = new AlignedTrees(stringAut2, span2);

        stringList1.add(sat1);
        stringList2.add(sat2);

        Iterable<Pair<TreeAutomaton, HomomorphismManager>> solutions
                = corp.makeRuleTrees(stringList1, stringList2);
        
        Pair<TreeAutomaton,HomomorphismManager> pair = solutions.iterator().next();
        this.solution = pair.getLeft();
        this.homm = pair.getRight();
        
        Iterable<Rule> rules = this.solution.getAllRulesTopDown();
        for(Rule r : rules) {
            r.setWeight(0.9);
        }
        
        Iterator<Tree<Integer>> lang = this.solution.languageIteratorRaw();
        Tree<Integer> t1 = lang.next();
        Tree<Integer> t2 = lang.next();
        
        imp1 = this.solution.getRuleTree(t1);
        imp2 = this.solution.getRuleTree(t2);
        
        
        this.ita = new InterpretedTreeAutomaton(solution);
        List<Signature> list = new ArrayList<>();
        list.add(ita.getAutomaton().getSignature());
        this.ins = new IndependentTrees(0.5, list, "S");
        
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
        
        
        System.out.println(d1);
        System.out.println(d2);
        assertEquals(d1,d2,0.00001);
        assertEquals(d1,-69.31471805599452,0.00001);
        
        this.ins.add(this.imp1, ita, 200.0);
        
        double d3;
        double d4;
        d3 = this.ins.getLogWeight(imp1, ita);
        d4 = this.ins.getLogWeight(imp2, ita);
        
        
        assertEquals(d3,-3.300832476232765,0.000001);
        assertEquals(d4,-63.80406428593885,0.000001);
        
        assertTrue(d3 > d1);
        assertTrue(d4 > d1);
        
        this.ins.add(this.imp1, ita, -200.0);
        
        assertEquals(d1,this.ins.getLogWeight(imp1, ita),0.00001);
        assertEquals(this.ins.getLogWeight(imp2, ita),this.ins.getLogWeight(imp1, ita),0.00001);
    }
}
