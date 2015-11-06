/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.AlignmentFactory;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.pruning.PruneOneSideTerminating;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class CorpusCreatorTest {
    
    /**
     * 
     */
    private CorpusCreator<List<String>,Tree<String>> cc;
    
    /**
     * 
     */
    private StringAlgebra sal;
    
    /**
     * 
     */
    private MinimalTreeAlgebra mta;
    
    /**
     * 
     */
    private ArrayList<String> firstInputs;
    
    /**
     * 
     */
    private ArrayList<String> secondInputs;
    
    /**
     * 
     */
    private ArrayList<String> firstAlign;
    
    /**
     * 
     */
    private ArrayList<String> secondAlign;
    
    @Before
    public void setUp() {
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new PruneOneSideTerminating()).setSecondPruner(Pruner.DEFAULT_PRUNER)
                .setFirstVariableSource(new LeftRightXFromFinite())
                .setSecondVariableSource(new JustXEveryWhere());
        
        sal = new StringAlgebra();
        mta = new MinimalTreeAlgebra();
        
        cc = fact.getInstance(sal, mta, new SpanAligner.Factory(), new AddressAligner.Factory());
        
        this.firstInputs = new ArrayList<>();
        firstInputs.add("John went home");
        firstInputs.add("Frank went home");
        
        this.firstAlign = new ArrayList<>();
        firstAlign.add("0:1:1 1:2:2 2:3:3");
        firstAlign.add("0:1:1 1:2:2 2:3:3");
        
        this.secondInputs = new ArrayList<>();
        this.secondInputs.add("S(NP(John),VP(went,NP(home)))");
        this.secondInputs.add("S(NP(Frank),VP(went,NP(home)))");
        
        this.secondAlign = new ArrayList<>();
        this.secondAlign.add("0-0-0-0-0:1 0-0-0-1-0:2 0-0-0-1-1-0:3");
        this.secondAlign.add("0-0-0-0-0:1 0-0-0-1-0:2 0-0-0-1-1-0:3");
        //TODO
    }

    @Test
    public void testMakeFirstPruning() throws ParserException{
        List<AlignedTrees> list1  = CorpusCreator.makeInitialAlignedTrees(2, firstInputs, firstAlign, sal, this.cc.getFirtAL());
        List<AlignedTrees> pruned = CorpusCreator.makeFirstPruning(list1, cc.getFirstPruner(), cc.getFirstVI());
        
        System.out.println(pruned.get(0).getTrees());
        //TODO
    }
    
    @Test
    public void testMakeInitialAlignedTrees() throws ParserException, Exception{
        List<AlignedTrees> list1 = CorpusCreator.makeInitialAlignedTrees(2, firstInputs, firstAlign, sal, this.cc.getFirtAL());
        assertEquals(list1.size(),2);
        
        Set<Tree<String>> lang1 = list1.get(0).getTrees().language();
        Set<Tree<String>> lang2 = list1.get(1).getTrees().language();
        
        assertEquals(lang1.size(),2);
        assertEquals(lang2.size(),2);
        assertTrue(lang1.contains(pt("*(John,*(went,home))")));
        assertTrue(lang1.contains(pt("*(*(John,went),home)")));
        assertTrue(lang2.contains(pt("*(*(Frank,went),home)")));
        assertTrue(lang2.contains(pt("*(Frank,*(went,home))")));
        
        assertEquals(list1.get(0).getAlignments().toString(),"SpanAligner{alignments={1-2=>{2}, 0-1=>{1}, 2-3=>{3}}}");
        assertEquals(list1.get(1).getAlignments().toString(),"SpanAligner{alignments={1-2=>{2}, 0-1=>{1}, 2-3=>{3}}}");
        
        List<AlignedTrees> list2 = CorpusCreator.makeInitialAlignedTrees(1, secondInputs, secondAlign, mta, this.cc.getSecondAL());
        assertEquals(list2.size(),1);
        assertEquals(list2.get(0).getTrees().language().size(),64);
        assertEquals(list2.get(0).getAlignments().toString(),"AddressAligner{map={0-0-0-1-1-0=>{3}, 0-0-0-1-0=>{2}, 0-0-0-0-0=>{1}}}");
    }
    
    /**
     * Test of getAlgebra2 method, of class CreateCorpus.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testGetAlgebra() throws ParserException {
        assertEquals(cc.getSecondAlgebra(),this.mta);
        assertEquals(cc.getFirstAlgebra(),this.sal);
    }
}