/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.edit_distance;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.EditDistanceState;
import de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.Status;
import static de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.Status.KEPT;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class EditDistanceTreeAutomatonTest {
    /**
     * 
     */
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
    public void setUp() throws ParseException {
        sal = new StringAlgebra();

        sal.getSignature().addSymbol("a", 0);
        sal.getSignature().addSymbol("b", 0);
        sal.getSignature().addSymbol("c", 2);
        
        Tree<String> one = sal.decompose(sal.parseString("a")).viterbi();

        edt1 = new EditDistanceTreeAutomaton(sal.getSignature(), one);

        // delete, insert, substitute
        Tree<String> two = TreeParser.parse("*(*(a,b),*(b,a))");
        edt2 = new EditDistanceTreeAutomaton(sal.getSignature(), two, (int pos) -> -(pos + 1), (int pos) -> -((pos + 1) * (pos + 1)), (int pos) -> -(1.0 / (pos + 2)));
    }

    /**
     * Test of getRulesBottomUp method, of class EditDistanceTreeAutomaton.
     */
    @Test
    public void testGetRulesBottomUp() throws ParseException {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();

        TreeAutomaton tb = edt1.asConcreteTreeAutomatonWithStringStates();
        TreeAutomaton comp = taic.read(EXPECTED_1);

        assertEquals(tb, comp);

        EditDistanceTreeAutomaton ta = edt2;
        TreeAutomaton in = sal.decompose(sal.parseString("a a b b"));

        TreeAutomaton<Pair<? extends Object, EditDistanceState>> q = in.intersect(ta);
        
        assertEquals(q.viterbiRaw().getWeight(), 0.5866462195100318, 0.0000001);

        in = sal.decompose(sal.parseString("a b b"));

        q = in.intersect(ta);
        
        assertEquals(q.viterbiRaw().getWeight(), Math.exp(-1)*Math.exp(-1.0/3.0)*Math.exp(-1.0/5.0), 0.00000001);

        in = sal.decompose(sal.parseString("a b a"));

        q = in.intersect(ta);

        assertEquals(q.viterbiRaw().getWeight(), Math.exp(-1)*Math.exp(-1.0/3.0), 0.00000001);
        
        
        in = sal.decompose(sal.parseString("a a b b"));
        for(Rule r : (Iterable<Rule>) in.getAllRulesTopDown()) {
            if(r.getChildren().length > 1) {
                StringAlgebra.Span span = (StringAlgebra.Span) in.getStateForId(r.getChildren()[0]);
                if(span.end-span.start != 1) {
                    r.setWeight(0.0);
                }
            }
        }
        
        
        q = in.intersect(ta);
                
        Tree<Rule> choice = q.maxRuleTree();
        
        IntersectionResolver ir = new IntersectionResolver(q);
        
        Tree<Pair<EditDistanceState,String>> inter = choice.map(ir);
        Set<Tree<String>> choices = ta.selectCoveringSetForFalse(inter);
        
        assertEquals(choices.size(),2);
        assertTrue(choices.contains(TreeParser.parse("*(a,*(b,b))")));
        assertTrue(choices.contains(TreeParser.parse("*(a,*(a,*(b,b)))")));
        
        in = sal.decompose(sal.parseString("a b b b"));
        for(Rule r : (Iterable<Rule>) in.getAllRulesTopDown()) {
            if(r.getChildren().length > 1) {
                StringAlgebra.Span span = (StringAlgebra.Span) in.getStateForId(r.getChildren()[0]);
                if(span.end-span.start != 1) {
                    r.setWeight(0.0);
                }
            }
        }
        q = in.intersect(ta);
        choice = q.maxRuleTree();
        
        ir = new IntersectionResolver(q);
        
        inter = choice.map(ir);
        ta.selectCoveringSetForFalse(inter);
        
        choices = ta.selectCoveringSetForFalse(inter);
        assertEquals(choices.size(),4);
        assertTrue(choices.contains(TreeParser.parse("b")));
        assertTrue(choices.contains(TreeParser.parse("*(b,*(b,b))")));
        assertTrue(choices.contains(TreeParser.parse("*(b,b)")));
        assertTrue(choices.contains(TreeParser.parse("*(a,*(b,*(b,b)))")));
        
        Status[] errs = ta.computeStatusGeneral(inter);
        choices = ta.selectCoveringTreeForCorrect(errs);
        
        assertEquals(choices.size(),3);
        assertTrue(choices.contains(TreeParser.parse("a")));
        assertTrue(choices.contains(TreeParser.parse("*(*(a,b),*(b,a))")));
        assertTrue(choices.contains(TreeParser.parse("*(b,a)")));
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
