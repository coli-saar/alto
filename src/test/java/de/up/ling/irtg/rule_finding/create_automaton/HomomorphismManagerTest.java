/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.pruning.PruneOneSideTerminating;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.RemoveDead;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.irtg.signature.Signature;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class HomomorphismManagerTest {
    
    /**
     * 
     */
    private Signature sig1;
    
    /**
     * 
     */
    private Signature sig2;
    
    /**
     * 
     */
    private Signature shared;
    
    /**
     * 
     */
    private HomomorphismManager hm;
    
    /**
     * 
     */
    private List<AlignedTrees> pruned1;
    
    /**
     * 
     */
    private List<AlignedTrees> pruned2;
    
    @Before
    public void setUp() throws ParserException {
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new PruneOneSideTerminating()).setSecondPruner(Pruner.DEFAULT_PRUNER)
                .setFirstVariableSource(new LeftRightXFromFinite())
                .setSecondVariableSource(new JustXEveryWhere());
        
        StringAlgebra sal = new StringAlgebra();
        StringAlgebra mta = new StringAlgebra();
        
        CorpusCreator cc = fact.getInstance(sal, mta, new SpanAligner.Factory(), new SpanAligner.Factory());
        
        ArrayList<String> firstInputs = new ArrayList<>();
        firstInputs.add("John went home");
        firstInputs.add("Frank went home");
        
        ArrayList<String> firstAlign = new ArrayList<>();
        firstAlign.add("0:1:1 1:2:2 2:3:3");
        firstAlign.add("0:1:4 1:2:5 2:3:6");
        
        ArrayList<String> secondInputs = new ArrayList<>();
        secondInputs.add("John ging heim");
        secondInputs.add("Frank ging heim");
        
        ArrayList<String> secondAlign = new ArrayList<>();
        secondAlign.add("0:1:1 1:2:2 2:3:5");
        secondAlign.add("0:1:4 1:2:5 2:3:6 2:3:7");
        
        List<AlignedTrees> list1  = CorpusCreator.makeInitialAlignedTrees(2, firstInputs, firstAlign, sal, cc.getFirtAL());
        pruned1 = CorpusCreator.makeFirstPruning(list1, cc.getFirstPruner(), cc.getFirstVI());
        
        list1 = CorpusCreator.makeInitialAlignedTrees(2, secondInputs, secondAlign, mta, cc.getSecondAL());
        pruned2 = CorpusCreator.makeFirstPruning(list1, cc.getSecondPruner(), cc.getSecondVI());

        this.sig1 = sal.getSignature();
        this.sig2 = mta.getSignature();
        this.shared = new Signature();
        
        this.hm = new HomomorphismManager(this.sig1, this.sig2, this.shared);
    }

    /**
     * Test of update method, of class HomomorphismManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testUpdate() throws Exception {
        Propagator pg = new Propagator();
        TreeAutomaton ta1= pg.convert(this.pruned1.get(0)).getTrees();
        TreeAutomaton ta2 = pg.convert(this.pruned2.get(0)).getTrees();
        
        hm.update(ta1.getAllLabels(), ta2.getAllLabels());
        
        ta1= pg.convert(this.pruned1.get(0)).getTrees();
        ta2 = pg.convert(this.pruned2.get(0)).getTrees();
        hm.update(ta1.getAllLabels(), ta2.getAllLabels());

        Homomorphism hm1 = hm.getHomomorphismRestriction1(ta1.getAllLabels(), ta2.getAllLabels());
        Homomorphism hm2 = hm.getHomomorphismRestriction2(ta2.getAllLabels(), ta1.getAllLabels());
        
        RuleFindingIntersectionAutomaton rfi = new RuleFindingIntersectionAutomaton(ta1, ta2, hm1, hm2);
        TreeAutomaton done = new TopDownIntersectionAutomaton(rfi, hm.getRestriction());
            
        done = RemoveDead.reduce(done);
        Set<Tree<String>> oldLang = done.language();
        
        done = hm.reduceToOriginalVariablePairs(done);
        hm1 = hm.getHomomorphism1();
        hm2 = hm.getHomomorphism2();
        
        Set<Pair<Tree<String>,Tree<String>>> pairs = new HashSet<>();
        
        for(Tree<String> t : (Iterable<Tree<String>>) done.language()){
            Tree<String> t1 = hm1.apply(t);
            Tree<String> t2 = hm2.apply(t);
            
            Pair<Tree<String>,Tree<String>> p = new Pair<>(t1,t2);
            assertFalse(pairs.contains(p));
            pairs.add(p);
            
            for(Tree<String> node : t.getAllNodes()){
                String label = node.getLabel();
                String left = hm1.get(label).getLabel();
                String right = hm2.get(label).getLabel();
                
                if(Variables.IS_VARIABLE.test(left)){
                    assertTrue(Variables.IS_VARIABLE.test(left));
                    assertTrue(hm.isVariable(hm.getSignature().getIdForSymbol(label)));
                    assertEquals(right,"X");
                }else{
                    assertFalse(Variables.IS_VARIABLE.test(right));
                    assertFalse(hm.isVariable(hm.getSignature().getIdForSymbol(label)));
                }   
            }
        }
        
        assertTrue(done.accepts(pt("'*(x1, x2) / x1 | 2'('x1 / *(x1, x2) | 2'('XJohn_went(x1) / X(x1) | 1'('*(x1, x2) / *(x1, x2) | 2'('XJohn_John(x1) / X(x1) | 1'('John() / x1 | 1'('x1 / John() | 1'('John() / John() | 0'))),'Xwent_went(x1) / X(x1) | 1'('went() / x1 | 1'('x1 / ging() | 1'('John() / John() | 0'))))),'x1 / heim() | 1'('John() / John() | 0')),'home() / x1 | 1'('John() / John() | 0'))")));
        
        assertEquals(oldLang.size(),done.language().size());
        assertEquals(done.language().size(),20);
    }
}