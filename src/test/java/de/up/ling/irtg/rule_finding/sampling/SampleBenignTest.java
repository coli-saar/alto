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
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.Lexicalized;
import de.up.ling.irtg.rule_finding.pruning.intersection.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.sampling.SampleBenign.Configuration;
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

    @Before
    public void setUp() throws ParserException {
        /*        = string1.decompose(string1.parseString("a very bad example"));
        SpanAligner span1 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4", stringAut1);

        TreeAutomaton stringAut2
                = string2.decompose(string2.parseString("ein sehr schlechtes Beispiel"));
        SpanAligner span2 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4", stringAut2);*/

        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton();
        cta.addFinalState(cta.addState("START"));
        cta.addRule(cta.createRule("START", "a", new String[] {}));
        
        this.solution = new InterpretedTreeAutomaton(cta);
        this.sampler = new RuleCountBenign(0.01, 8883843987987L, solution);
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

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
}
