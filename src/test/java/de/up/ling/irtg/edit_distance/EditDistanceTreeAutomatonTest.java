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

    private final static String EXPECTED_1 = "'<0,1>'! -> __*__ [1.0]\n"
            + "'<1,1>' -> __*__ [1.0]\n"
            + "'<0,0>' -> __*__ [1.0]\n"
            + "'<0,1>'! -> b [1.0]\n"
            + "'<1,1>' -> b [1.0]\n"
            + "'<0,0>' -> b [1.0]\n"
            + "'<0,0>' -> a [1.0]\n"
            + "'<0,1>'! -> a [0.0]\n"
            + "'<1,1>' -> a [1.0]\n"
            + "'<0,0>' -> *('<0,0>', '<0,0>') [0.0]\n"
            + "'<0,1>'! -> *('<0,0>', '<0,0>') [1.0]\n"
            + "'<0,1>'! -> *('<0,0>', '<0,1>') [0.0]\n"
            + "'<0,1>'! -> *('<0,0>', '<1,1>') [1.0]\n"
            + "'<0,1>'! -> *('<0,1>', '<1,1>') [0.0]\n"
            + "'<0,1>'! -> *('<1,1>', '<1,1>') [1.0]\n"
            + "'<1,1>' -> *('<1,1>', '<1,1>') [0.0]";

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

        for (Rule r : (Iterable<Rule>) comp.getAllRulesTopDown()) {
            r.setWeight(-r.getWeight());
        }

        assertEquals(ta, comp);

        ta = edt2.asConcreteTreeAutomatonWithStringStates();
        TreeAutomaton in = sal.decompose(sal.parseString("a"));

        TreeAutomaton q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), -1.0, 0.000001);

        Semiring<Double> tropical = new Semiring<Double>() {
            @Override
            public Double add(Double x, Double y) {
                return Math.max(x, y);
            }

            @Override
            public Double multiply(Double x, Double y) {
                return x + y;
            }

            @Override
            public Double zero() {
                return Double.NEGATIVE_INFINITY;
            }
        };

        in = sal.decompose(sal.parseString("a b"));

        q = in.intersect(ta);

        Int2ObjectMap<Double> map = q.evaluateInSemiring(tropical, (Rule r) -> r.getWeight());
        assertEquals(map.get(q.getFinalStates().iterator().nextInt()), -(1.0 / 3.0), 0.00000001);

        in = sal.decompose(sal.parseString("a b a"));

        q = in.intersect(ta);

        map = q.evaluateInSemiring(tropical, (Rule r) -> r.getWeight());
        assertEquals(map.get(q.getFinalStates().iterator().nextInt()), -1.5, 0.00000001);

        in = sal.decompose(sal.parseString("a a"));

        q = in.intersect(ta);

        map = q.evaluateInSemiring(tropical, (Rule r) -> r.getWeight());
        assertEquals(map.get(q.getFinalStates().iterator().nextInt()), 0.0, 0.00000001);

        in = sal.decompose(sal.parseString("b"));

        q = in.intersect(ta);

        map = q.evaluateInSemiring(tropical, (Rule r) -> r.getWeight());
        assertEquals(map.get(q.getFinalStates().iterator().nextInt()), -(1 + (1.0 / 3.0)), 0.00000001);
    }

    /**
     * Test of supportsBottomUpQueries method, of class
     * EditDistanceTreeAutomaton.
     */
    @Test
    public void testSupportsBottomUpQueries() {
        assertTrue(edt1.supportsBottomUpQueries());
        assertTrue(edt2.supportsBottomUpQueries());
    }

    /**
     * Test of supportsTopDownQueries method, of class
     * EditDistanceTreeAutomaton.
     */
    @Test
    public void testSupportsTopDownQueries() {
        assertFalse(edt1.supportsTopDownQueries());
        assertFalse(edt2.supportsTopDownQueries());
    }

    /**
     * Test of isBottomUpDeterministic method, of class
     * EditDistanceTreeAutomaton.
     */
    @Test
    public void testIsBottomUpDeterministic() {
        assertFalse(edt1.isBottomUpDeterministic());
        assertFalse(edt2.isBottomUpDeterministic());
    }

    /**
     * Test of computeStatus method, of class EditDistanceTreeAutomaton.
     * @throws java.lang.Exception
     */
    @Test
    public void testComputeStatus() throws Exception {
        Tree<Rule> tr = edt2.maxTropicalRules();
        
        Status[] keep = edt2.computeStatus(tr);
        assertEquals(keep[0],KEPT);
        assertEquals(keep[1],KEPT);
        
        Rule r = null;
        for(Rule rule : (Iterable<Rule>) edt2.getAllRulesTopDown()) {
            if(rule.getArity() == 2) {
                r = rule;
            }
        }
        
        tr = Tree.create(r);
        
        keep = edt2.computeStatus(tr);
        assertEquals(keep[0],Status.DELETED);
        assertEquals(keep[1],Status.DELETED);
        
        r = null;
        for(Rule rule : (Iterable<Rule>) edt2.getAllRulesTopDown()) {
            if(rule.getArity() == 0 && edt2.getSignature().resolveSymbolId(rule.getLabel()).equals("b")) {
                EditDistanceTreeAutomaton.EditDistanceState stat = edt2.getStateForId(rule.getParent());
                
                if(stat.getReadSpanStart() == 0 && stat.getReadSpanEnd() == 1) {
                    r = rule;
                }
            }
        }
        
        tr = Tree.create(r);
        
        keep = edt2.computeStatus(tr);
        assertEquals(keep[0],Status.SUBSTITUTED);
        assertEquals(keep[1],Status.DELETED);
    }
}
