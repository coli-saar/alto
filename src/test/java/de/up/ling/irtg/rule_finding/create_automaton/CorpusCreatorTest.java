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
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.pruning.intersection.tree.NoLeftIntoRight;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Iterator;
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
    private CorpusCreator<List<String>, Tree<String>> cc;

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
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton t) -> {
            return new RightBranchingNormalForm(t.getSignature(),t.getAllLabels());
        }))
                .setSecondPruner(new IntersectionPruner<>((TreeAutomaton t) -> {
            return new NoLeftIntoRight(t.getSignature(),t.getAllLabels());
        }))
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
    public void testMakeFirstPruning() throws ParserException, Exception {
        Iterable<AlignedTrees> list1 = CorpusCreator.makeInitialAlignedTrees(firstInputs, firstAlign, sal, this.cc.getFirtAlignmentFactory());
        Iterable<AlignedTrees> pruned = CorpusCreator.makeFirstPruning(list1, cc.getFirstPruner(), cc.getFirstVI());

        Iterator<AlignedTrees> it = pruned.iterator();
        AlignedTrees at1 = it.next();
        AlignedTrees at2 = it.next();
        
        
        assertTrue(at1.getAlignments() instanceof SpecifiedAligner);
        assertFalse(it.hasNext());

        assertTrue(at1.getTrees().accepts(pt("*(John,*(went,home))")));
        assertTrue(at1.getTrees().accepts(pt("*(John,Xwent_home(*(went,home)))")));
        assertFalse(at1.getTrees().accepts(pt("*(John,XJohn_home(*(went,home)))")));

        assertTrue(at2.getTrees().accepts(pt("*(Frank,Xwent_home(*(went,home)))")));

        TreeAutomaton t = at1.getTrees();
        int label = t.getSignature().getIdForSymbol("John");
        Rule r = (Rule) t.getRulesBottomUp(label, new int[0]).iterator().next();
        Object o = t.getStateForId(r.getParent());

        assertEquals(at1.getAlignments().getAlignmentMarkers(o).size(), 1);
        assertTrue(at1.getAlignments().getAlignmentMarkers(o).contains(1));
    }

    @Test
    public void testMakeInitialAlignedTrees() throws ParserException, Exception {
        Iterable<AlignedTrees> list1 = CorpusCreator.makeInitialAlignedTrees(firstInputs, firstAlign, sal, this.cc.getFirtAlignmentFactory());
        
        Iterator<AlignedTrees> it = list1.iterator();
        AlignedTrees at1 = it.next();
        AlignedTrees at2 = it.next();
        
        Set<Tree<String>> lang1 = at1.getTrees().language();
        Set<Tree<String>> lang2 = at2.getTrees().language();

        assertFalse(it.hasNext());
        
        assertEquals(lang1.size(), 2);
        assertEquals(lang2.size(), 2);
        assertTrue(lang1.contains(pt("*(John,*(went,home))")));
        assertTrue(lang1.contains(pt("*(*(John,went),home)")));
        assertTrue(lang2.contains(pt("*(*(Frank,went),home)")));
        assertTrue(lang2.contains(pt("*(Frank,*(went,home))")));

        assertEquals(at1.getAlignments().toString(), "SpanAligner{alignments={1-2=>{2}, 0-1=>{1}, 2-3=>{3}}}");
        assertEquals(at2.getAlignments().toString(), "SpanAligner{alignments={1-2=>{2}, 0-1=>{1}, 2-3=>{3}}}");

        Iterable<AlignedTrees> list2 = CorpusCreator.makeInitialAlignedTrees(secondInputs, secondAlign, mta, this.cc.getSecondAlignmentFactory());
        assertEquals(list2.iterator().next().getTrees().language().size(), 64);
        assertEquals(list2.iterator().next().getAlignments().toString(), "AddressAligner{map={0-0-0-1-1-0=>{3}, 0-0-0-1-0=>{2}, 0-0-0-0-0=>{1}}}");
    }

    /**
     * Test of getAlgebra2 method, of class CreateCorpus.
     */
    @Test
    public void testGetAlgebra() {
        assertEquals(cc.getSecondAlgebra(), this.mta);
        assertEquals(cc.getFirstAlgebra(), this.sal);
    }

    @Test
    public void testFinalResult() throws ParserException, Exception {
        Iterable<Pair<TreeAutomaton,HomomorphismManager>> result =
                this.cc.makeRuleTrees(firstInputs, secondInputs, firstAlign, secondAlign);

        Iterator<Pair<TreeAutomaton,HomomorphismManager>> it = result.iterator();
        Pair<TreeAutomaton,HomomorphismManager> p1 = it.next();
        Pair<TreeAutomaton,HomomorphismManager> p2 = it.next();
        
        assertFalse(it.hasNext());

        TreeAutomaton first = p1.getLeft();
        TreeAutomaton second = p2.getLeft();

        assertEquals(first.language().size(), second.language().size());

        for (Rule r : (Iterable<Rule>) first.getAllRulesTopDown()) {
            r.setWeight(0.2);
        }

        Tree<String> ts = first.viterbi();

        Homomorphism hm1 = p1.getRight().getHomomorphism1();
        Homomorphism hm2 = p1.getRight().getHomomorphism2();
        
        assertEquals(hm1.apply(ts), pt("*(XJohn_John(John),*(Xwent_went(went),Xhome_home(home)))"));
        Tree<String> q = shorten(hm2.apply(ts));
        assertEquals(this.mta.evaluate(q), pt("S(NP(John),VP(went,NP(home)))"));

        Tree<String> l = hm1.apply(ts);
        Tree<String> r = hm2.apply(ts);

        int firstVariables = 0;
        firstVariables = l.getAllNodes().stream().filter((node) -> (Variables.IS_VARIABLE.test(node.getLabel()))).map((_item) -> 1).reduce(firstVariables, Integer::sum);

        int secondVariables = 0;
        secondVariables = r.getAllNodes().stream().filter((node) -> (Variables.IS_VARIABLE.test(node.getLabel()))).map((_item) -> 1).reduce(secondVariables, Integer::sum);

        assertEquals(firstVariables, 3);
        assertEquals(secondVariables, firstVariables);
    }

    /**
     *
     * @param tr
     * @return
     */
    private Tree<String> shorten(Tree<String> tr) {
        if (Variables.IS_VARIABLE.test(tr.getLabel())) {
            return shorten(tr.getChildren().get(0));
        }

        List<Tree<String>> children = new ArrayList<>();
        tr.getChildren().forEach((child) -> {
            children.add(shorten(child));
        });

        return Tree.create(tr.getLabel(), children);
    }
}
