/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
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
    }

    @Test
    public void testMakeFirstPruning() throws ParserException, Exception{
        List<AlignedTrees> list1  = CorpusCreator.makeInitialAlignedTrees(2, firstInputs, firstAlign, sal, this.cc.getFirtAL());
        List<AlignedTrees> pruned = CorpusCreator.makeFirstPruning(list1, cc.getFirstPruner(), cc.getFirstVI());
        
        assertTrue(pruned.get(0).getAlignments() instanceof SpecifiedAligner);
        assertEquals(pruned.size(),2);
        
        assertTrue(pruned.get(0).getTrees().accepts(pt("*(John,*(went,home))")));
        assertTrue(pruned.get(0).getTrees().accepts(pt("*(John,Xwent_home(*(went,home)))")));
        assertFalse(pruned.get(0).getTrees().accepts(pt("*(John,XJohn_home(*(went,home)))")));
        
        assertTrue(pruned.get(1).getTrees().accepts(pt("*(Frank,Xwent_home(*(went,home)))")));
        
        TreeAutomaton t = pruned.get(0).getTrees();
        int label = t.getSignature().getIdForSymbol("John");
        Rule r = (Rule) t.getRulesBottomUp(label, new int[0]).iterator().next();
        Object o = t.getStateForId(r.getParent());
        
        assertEquals(pruned.get(0).getAlignments().getAlignmentMarkers(o).size(),1);
        assertTrue(pruned.get(0).getAlignments().getAlignmentMarkers(o).contains(1));
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
     */
    @Test
    public void testGetAlgebra() {
        assertEquals(cc.getSecondAlgebra(),this.mta);
        assertEquals(cc.getFirstAlgebra(),this.sal);
    }
    
    @Test
    public void testFinalResult() throws ParserException, Exception {
        List<TreeAutomaton> result = this.cc.makeRuleTrees(firstInputs, secondInputs, firstAlign, secondAlign);
        
        assertEquals(result.size(),2);
        
        TreeAutomaton first = result.get(0);
        TreeAutomaton second = result.get(1);
        
        assertEquals(first.language().size(),second.language().size());
        
        for(Rule r : (Iterable<Rule>) first.getAllRulesTopDown()){
            if(this.cc.getHomomorphismManager().getSignature().resolveSymbolId(r.getLabel()).equals("Xwent_home(x1) / X(x1) | 1")
                    || this.cc.getHomomorphismManager().getSignature().resolveSymbolId(r.getLabel()).equals("*(x1, x2) / __LEFT__INTO__RIGHT__(x1, x2) | 2")){
                r.setWeight(2.0);
            }
            else{
                r.setWeight(0.2);
            }
        }
        
        Tree<String> ts = first.viterbi();
        
        Homomorphism hm1 = this.cc.getHomomorphismManager().getHomomorphism1();
        Homomorphism hm2 = this.cc.getHomomorphismManager().getHomomorphism2();
        
        assertEquals(hm1.apply(ts),pt("*(XJohn_John(John),Xwent_home(Xwent_home(*(Xwent_went(went),Xhome_home(home)))))"));
        Tree<String> q = shorten(hm2.apply(ts));
        System.out.println(q);
        assertEquals(this.mta.evaluate(q),pt("S(NP(John),VP(went,NP(home)))"));
        
        Tree<String> l = hm1.apply(ts);
        Tree<String> r = hm2.apply(ts);
        
        int firstVariables = 0;
        for(Tree<String> node : l.getAllNodes()){
            if(Variables.IS_VARIABLE.test(node.getLabel())){
                ++firstVariables;
            }
        }
        
        int secondVariables = 0;
        for(Tree<String> node : r.getAllNodes()){
            if(Variables.IS_VARIABLE.test(node.getLabel())){
                ++secondVariables;
            }
        }
        
        assertEquals(firstVariables,5);
        assertEquals(secondVariables,firstVariables);
    }
    
    /**
     * 
     * @param tr
     * @return 
     */
    private Tree<String> shorten(Tree<String> tr){
        if(Variables.IS_VARIABLE.test(tr.getLabel())){
            return shorten(tr.getChildren().get(0));
        }
        
        List<Tree<String>> children = new ArrayList<>();
        tr.getChildren().forEach((child) -> {
            children.add(shorten(child));
        });
        
        return Tree.create(tr.getLabel(), children);
    }
}