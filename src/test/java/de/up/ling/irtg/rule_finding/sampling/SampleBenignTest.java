/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import com.google.common.base.Function;
import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.Lexicalized;
import de.up.ling.irtg.rule_finding.pruning.intersection.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.sampling.SampleBenign.Configuration;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SampleBenignTest {

    /**
     *
     */
    private RuleCountBenign sampler;

    /**
     *
     */
    private InterpretedTreeAutomaton solution;

    /**
     *
     */
    private HomomorphismManager homomorphismHolder;

    @Before
    public void setUp() throws ParserException {
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

        Pair<TreeAutomaton, HomomorphismManager> example = solutions.iterator().next();
               
        this.solution = new InterpretedTreeAutomaton(example.getLeft());
        this.homomorphismHolder = example.getRight();
        
        this.sampler = new RuleCountBenign(0.01, 926986467599918617L, this.solution);
    }

    /**
     * Test of setAutomaton method, of class SampleBenign.
     */
    @Test
    public void testSetAutomaton() {
        Configuration config = new Configuration(new Model() {
            @Override
            public double getLogWeight(Tree<Rule> t, InterpretedTreeAutomaton ita) {
                return 0.0;
            }

            @Override
            public void add(Tree<Rule> t, InterpretedTreeAutomaton ita, double amount) {
            }
        });

        this.sampler.getSample(config);

        int start = this.solution.getAutomaton().getFinalStates().iterator().nextInt();

        assertTrue(this.sampler.getFinalStateCount(start) > 10.0);

        TreeAutomaton t = new ConcreteTreeAutomaton();
        this.sampler.setAutomaton(new InterpretedTreeAutomaton(t));

        assertEquals(this.sampler.getFinalStateCount(start), 0.0, 0.00001);

        this.sampler.setAutomaton(this.solution);
        this.sampler.getSample(config);

        assertTrue(this.sampler.getFinalStateCount(start) > 10.0);

        this.sampler.setResetEverySample(false);
        this.sampler.getSample(config);

        assertTrue(this.sampler.getFinalStateCount(start) > 25.0);

        assertEquals(this.sampler.getFinalStateCount(start)
                + 0.01, this.sampler.getSmoothedFinalStateCount(start), 0.000001);

        this.sampler.resetAdaption();
        
    }
    
    /**
     * Test of setResetEverySample method, of class SampleBenign.
     */
    @Test
    public void testSetResetEverySample() {
        assertTrue(this.sampler.isResetEverySample());
        this.sampler.setResetEverySample(false);
        assertFalse(this.sampler.isResetEverySample());
    }
    
    /**
     * 
     * @throws ParseException 
     */
    @Test
    public void testGetSample() throws ParseException {
        Tree<String> goal1 = TreeParser.parse("*(X(very),*(X(bad),X(example)))");
        Tree<Integer> iGoal1 = 
                this.homomorphismHolder.getHomomorphism1().getTargetSignature().addAllSymbols(goal1);
        
        Configuration config = new Configuration(new Model() {
            private final Function<Rule, Integer> funct = (Rule r) -> {
                return r.getLabel();
            };

            @Override
            public double getLogWeight(Tree<Rule> t, InterpretedTreeAutomaton ita) {
                Tree<Integer> tq = t.map(funct);
                tq = homomorphismHolder.getHomomorphism1().applyRaw(tq);
                double sum = 0.0;
                
                for(Tree<Integer> ti : tq.getAllNodes()) {
                    if(ti.equals(iGoal1)){
                        sum += 10.0;
                    }
                }
                
                return sum < 1.0 ? -5.0 : sum;
            }

            @Override
            public void add(Tree<Rule> t, InterpretedTreeAutomaton it, double amount) {}
        });
        
        config.setSampleSize((int i) -> 1000);
        config.setRounds(20);
        
        List<Tree<Rule>> sample = this.sampler.getSample(config);

        Homomorphism strHom1 = this.homomorphismHolder.getHomomorphism1();

        Function<Rule, Integer> funct = (Rule rul) -> {
            int num = rul.getLabel();
            return num;
        };
        Function<Integer, String> string1 = (Integer i) -> {
            return strHom1.getTargetSignature().resolveSymbolId(i);
        };

        double d1 = 0.0;
        for(Tree<Rule> t : sample) {
            Tree<Integer> iTree = t.map(funct);
            Tree<Integer> tTree1 = strHom1.applyRaw(iTree);
            Tree<String> qTree1 = strHom1.getTargetSignature().resolve(tTree1);
            
            for(Tree<String> sol : qTree1.getAllNodes()) {
                if(sol.equals(goal1)) {
                    d1 += 1;
                }
            }
        }
        
        assertTrue(d1 > 800.0);
    }
}
