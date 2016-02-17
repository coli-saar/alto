/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.Lexicalized;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.rule_finding.sampling.models.IndependentTrees;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SampleOnlineEMTest {

    /**
     *
     */
    private Model mod;

    /**
     *
     */
    private List<InterpretedTreeAutomaton> options;

    /**
     *
     */
    private SampleOnlineEM soe;

    /**
     *
     */
    private List<Homomorphism> left;

    @Before
    public void setUp() {
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
        fact.setSecondVariableSource(new LeftRightXFromFinite());

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

        stringAut1
                = string1.decompose(string1.parseString("another very bad example"));
        span1 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4", stringAut1);

        stringAut2
                = string2.decompose(string2.parseString("zusätliches sehr schlechtes Beispiel"));
        span2 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4", stringAut2);

        sat1 = new AlignedTrees(stringAut1, span1);
        sat2 = new AlignedTrees(stringAut2, span2);

        stringList1.add(sat1);
        stringList2.add(sat2);

        Iterable<Pair<TreeAutomaton, HomomorphismManager>> solutions
                = corp.makeRuleTrees(stringList1, stringList2);

        FunctionIterable<Signature, Pair<TreeAutomaton, HomomorphismManager>> fi
                = new FunctionIterable<>(solutions, (Pair<TreeAutomaton, HomomorphismManager> p) -> {
                    return p.getLeft().getSignature();
                });
        mod = new IndependentTrees(1, fi, "S");

        options = new ArrayList<>();
        left = new ArrayList<>();

        for (Pair<TreeAutomaton, HomomorphismManager> p : solutions) {
            options.add(new InterpretedTreeAutomaton(p.getLeft()));

            left.add(p.getRight().getHomomorphism1());
        }

        this.soe = new SampleOnlineEM();
        soe.setLearnSampleSize(200);
        soe.setTrainIterations(20);
    }

    /**
     * Test of getChoices method, of class SampleOnlineEM.
     */
    @Test
    public void testGetChoices() {
        Iterable<Iterable<Tree<String>>> it = soe.getChoices(options, mod, 9782598725987L);
        List<Tree<String>> results = new ArrayList<>();
        
        it.forEach((Iterable<Tree<String>> inner) -> inner.forEach(results::add));
        
        assertEquals(results.size(),400);
    }

}
