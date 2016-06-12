/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.edit_distance;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.Status;
import static de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.Status.KEPT;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class EditDistanceTreeAutomatonTest {

    private final static String EXPECTED_1 = "'<0,1>'! -> __*__ [0.36787944117144233]\n"
            + "'<1,1>' -> __*__ [0.36787944117144233]\n"
            + "'<0,0>' -> __*__ [0.36787944117144233]\n"
            + "'<0,1>'! -> b [0.36787944117144233]\n"
            + "'<1,1>' -> b [0.36787944117144233]\n"
            + "'<0,0>' -> b [0.36787944117144233]\n"
            + "'<0,0>' -> a [0.36787944117144233]\n"
            + "'<0,1>'! -> a [1.0]\n"
            + "'<1,1>' -> a [0.36787944117144233]\n"
            + "'<0,0>' -> *('<0,0>', '<0,0>') [1.0]\n"
            + "'<0,1>'! -> *('<0,0>', '<0,0>') [0.36787944117144233]\n"
            + "'<0,1>'! -> *('<0,0>', '<0,1>') [1.0]\n"
            + "'<0,1>'! -> *('<0,0>', '<1,1>') [0.36787944117144233]\n"
            + "'<0,1>'! -> *('<0,1>', '<1,1>') [1.0]\n"
            + "'<0,1>'! -> *('<1,1>', '<1,1>') [0.36787944117144233]\n"
            + "'<1,1>' -> *('<1,1>', '<1,1>') [1.0]";

    /**
     *
     */
    private EditDistanceTreeAutomaton edt2;

    /**
     *
     */
    private EditDistanceTreeAutomaton edt1;

    /**
     *
     */
    private StringAlgebra sal;

    @Before
    public void setUp() {
        sal = new StringAlgebra();

        sal.getSignature().addSymbol("a", 0);
        sal.getSignature().addSymbol("b", 0);
        sal.getSignature().addSymbol("c", 2);

        edt1 = new EditDistanceTreeAutomaton(sal.getSignature(), sal.parseString("a"));

        // delete, insert, substitute
        edt2 = new EditDistanceTreeAutomaton(sal.getSignature(), sal.parseString("a a"), (int pos) -> -(pos + 1), (int pos) -> -((pos + 1) * (pos + 1)), (int pos) -> -(1.0 / (pos + 2)));
    }

    /**
     * Test of getRulesBottomUp method, of class EditDistanceTreeAutomaton.
     */
    @Test
    public void testGetRulesBottomUp() {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();

        TreeAutomaton ta = edt1.asConcreteTreeAutomatonWithStringStates();
        TreeAutomaton comp = taic.read(EXPECTED_1);

        assertEquals(ta, comp);

        ta = edt2.asConcreteTreeAutomatonWithStringStates();
        TreeAutomaton in = sal.decompose(sal.parseString("a"));

        TreeAutomaton q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), 0.36787944117144233, 0.0000001);

        in = sal.decompose(sal.parseString("a b"));

        q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), Math.exp(-(1.0 / 3.0)), 0.00000001);

        in = sal.decompose(sal.parseString("a b a"));

        q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), Math.exp(-1.5), 0.00000001);

        in = sal.decompose(sal.parseString("a a"));

        q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), Math.exp(0.0), 0.00000001);

        in = sal.decompose(sal.parseString("b"));

        q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), Math.exp(-(1 + (1.0 / 3.0))), 0.00000001);
    }

    /**
     * Test of computeStatus method, of class EditDistanceTreeAutomaton.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testComputeStatus() throws Exception {
        Tree<Rule> tr = edt2.maxRuleTree();

        Status[] keep = edt2.computeStatus(tr);
        assertEquals(keep[0], KEPT);
        assertEquals(keep[1], KEPT);

        Rule r = null;
        for (Rule rule : (Iterable<Rule>) edt2.getAllRulesTopDown()) {
            if (rule.getArity() == 2) {
                r = rule;
            }
        }

        tr = Tree.create(r);

        keep = edt2.computeStatus(tr);
        assertEquals(keep[0], Status.DELETED);
        assertEquals(keep[1], Status.DELETED);

        r = null;
        for (Rule rule : (Iterable<Rule>) edt2.getAllRulesTopDown()) {
            if (rule.getArity() == 0 && edt2.getSignature().resolveSymbolId(rule.getLabel()).equals("b")) {
                EditDistanceTreeAutomaton.EditDistanceState stat = edt2.getStateForId(rule.getParent());

                if (stat.getReadSpanStart() == 0 && stat.getReadSpanEnd() == 1) {
                    r = rule;
                }
            }
        }

        tr = Tree.create(r);

        keep = edt2.computeStatus(tr);
        assertEquals(keep[0], Status.SUBSTITUTED);
        assertEquals(keep[1], Status.DELETED);
    }
}
