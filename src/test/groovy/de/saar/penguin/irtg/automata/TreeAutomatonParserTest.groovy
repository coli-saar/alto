/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*

/**
 *
 * @author koller
 */
class TreeAutomatonParserTest {
    @Test
    public void testParserNotNull() {
        TreeAutomaton automaton = parse("a -> q1\n f(q1,q1) -> q2!");

        assert automaton != null;
    }

    @Test
    public void testParserNoNewlines() {
        TreeAutomaton automaton = parse("a -> q1 f(q1,q1) -> q2!");

        assert automaton != null;
    }
    
    @Test
    public void testParserWithComments() {
        TreeAutomaton automaton = parse("a -> q1 /* foo -> bar */ f(q1,  /* lalala */ q1) -> q2!");

        assert automaton != null;
    }

    
    @Test
    public void testParser1() {
        TreeAutomaton automaton = parse("a -> q1\n f(q1,q1) -> q2 !");

        assertEquals(new HashSet([r("q1", "a", [])]), automaton.getRulesBottomUp("a", []));
        assertEquals(new HashSet([r("q2", "f", ["q1","q1"])]), automaton.getRulesBottomUp("f", ["q1", "q1"]));
        assertEquals(new HashSet(["q2"]), automaton.getFinalStates());
    }

    @Test
    public void testParser2() {
        TreeAutomaton automaton = parse("f(p2,p3) -> p1!\n a -> p2\n a -> p3");
        assertEquals(new HashSet([r("p2", "a", []), r("p3", "a", [])]), automaton.getRulesBottomUp("a", []));
    }

    @Test
    public void testWeights() {
        TreeAutomaton automaton = parse("a -> q1 [2]\n b -> q2 [1]\n f(q1,q1) -> q! [1]\n f(q1,q2) -> q! [1.5]");
        assertEquals(new HashSet([rw("q1", "a", [], 2)]), automaton.getRulesBottomUp("a", []));
        assertEquals(new HashSet([rw("q", "f", ["q1", "q2"], 1.5)]), automaton.getRulesBottomUp("f", ["q1", "q2"]));
    }

    private static TreeAutomaton parse(String s) {
        return TreeAutomatonParser.parse(new StringReader(s));
    }

    private static Rule r(parent, label, children) {
        return new Rule(parent, label, children);
    }

    private static Rule rw(parent, label, children, weight) {
        return new Rule(parent, label, children, weight);
    }
}

