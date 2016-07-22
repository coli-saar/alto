/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.rule_finding.Variables;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class PropagatorTest {

    /**
     *
     */
    private static final String TEST_AUTOMATON
            = "q_1_0 -> d [1.0]\n"
            + "q_0 -> b [0.5]\n"
            + "q! -> a(q_0, q_1) [0.5]\n"
            + "q_1 -> c(q_1_0) [0.5]";

    /**
     *
     */
    private static final String ALIGNMENTS
            = "q_1 ||| 0 1\n"
            + "q_0 ||| 4 3";

    /**
     *
     */
    private TreeAutomaton<String> ta;

    /**
     * 
     */
    private static final String GOAL_AUTOMATON
            = "q_0 -> b [0.5]\n"
            + "q_1_0 -> d [1.0]\n"
            + "q_1_0 -> '__X__{ _@_ q_1_0}'(q_1_0) [1.0]\n"
            + "q_1 -> c(q_1_0) [0.5]\n"
            + "q! -> '__X__{0,1,3,4 _@_ q}'(q) [1.0]\n"
            + "q_0 -> '__X__{3,4 _@_ q_0}'(q_0) [1.0]\n"
            + "q! -> a(q_0, q_1) [0.5]\n"
            + "q_1 -> '__X__{0,1 _@_ q_1}'(q_1) [1.0]";

    /**
     *
     */
    private SpecifiedAligner spac;

    /**
     *
     */
    private Propagator prop;

    @Before
    public void setUp() throws IOException {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        ta = taic.read(new ByteArrayInputStream(TEST_AUTOMATON.getBytes()));
        spac = new SpecifiedAligner(ta, new ByteArrayInputStream(ALIGNMENTS.getBytes()));

        prop = new Propagator();
    }

    /**
     * Test of propagate method, of class Propagator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testConvert() throws Exception {
        Int2ObjectMap<IntSortedSet> groups = this.prop.propagate(ta, spac);
        Set<String> markings = Propagator.turnToMarkers(groups.values());
        
        assertEquals(markings.size(),4);
        assertTrue(markings.contains(""));
        assertTrue(markings.contains("0,1,3,4"));
        assertTrue(markings.contains("0,1"));
        assertTrue(markings.contains("3,4"));
        
        TreeAutomaton<String> solution = this.prop.convert(this.ta, groups, markings);

        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        TreeAutomaton<String> goal = taic.read(GOAL_AUTOMATON);

        assertEquals(goal,solution);
        
        Iterator<Rule> r = goal.getRulesTopDown(goal.getFinalStates().iterator().nextInt()).iterator();
        while(r.hasNext()) {
            Rule rule = r.next();
            
            if(rule.getArity() == 1) {
                assertEquals(rule.getWeight(),1.0,0.0000000001);
            } else {
                assertEquals(rule.getWeight(), 0.5, 0.000000001);
            }
        }
    }

    @Test
    public void getOriginalInformation() {
        String q  = Propagator.createVariableWithContent("2,4,19", "a65");
        assertEquals(q,"__X__{2,4,19 _@_ a65}");
        assertEquals(Propagator.getAlignments(q),"2,4,19");
        assertEquals(Propagator.getAlignments("__X__{ _@_ a65}"),"");
        
        assertEquals(Propagator.getStateDescription(q),"a65");
        
        String s = Variables.createVariable("uuu");
        assertEquals(Propagator.getStateDescription(s),null);
    }
}
