/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*

/**
 *
 * @author koller
 */
class TreeAutomatonParserTest {
    static TreeAutomaton automaton;
    
    @Test
    public void testParserNotNull() {
        TreeAutomaton automaton = parse("q1 -> a\n q2! -> f(q1,q1)");

        assert automaton != null;
    }

    @Test
    public void testParserNoNewlines() {
        TreeAutomaton automaton = parse("q1 -> a q2! -> f(q1,q1)");

        assert automaton != null;
    }
    
    @Test
    public void testParserWithComments() {
        TreeAutomaton automaton = parse("q1 -> a /* foo -> bar */ q2! -> f(q1,  /* lalala */ q1)");

        assert automaton != null;
    }

    
    @Test
    public void testParser1() {
        automaton = parse("q1 -> a \n q2 ! -> f(q1,q1)");        

        assertRulesBottomUp(automaton, "a", [], [r("q1", "a", [])]);
        assertRulesBottomUp(automaton, "f", ["q1", "q1"], [r("q2", "f", ["q1","q1"])]);
        assertEquals(new HashSet([automaton.getIdForState("q2")]), automaton.getFinalStates());
    }

    @Test
    public void testParser2() {
        automaton = parse("p1! -> f(p2,p3)\n p2 -> a\n p3 -> a");
        assertRulesBottomUp(automaton, "a", [], [r("p2", "a", []), r("p3", "a", [])]);
    }

    @Test
    public void testWeights() {
        automaton = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        assertRulesBottomUp(automaton, "a", [], [rw("q1", "a", [], 2)]);
        assertRulesBottomUp(automaton, "f", ["q1", "q2"], [rw("q", "f", ["q1", "q2"], 1.5)]);
    }
    
    @Test
    public void testQuotedName() {
        automaton = parse("Foo -> \'foo bar\'");
        assertRulesBottomUp(automaton, "foo bar", [], [r("Foo", "foo bar", [])]);        
    }
    
    @Test
    public void testQuotedName2() {
        automaton = parse("Foo -> \'\"\'");
        assertRulesBottomUp(automaton, "\"", [], [r("Foo", "\"", [])]);
    }
    
    @Test
    public void testDoubleQuotedName() {
        automaton = parse("Foo -> \"foo bar\"");
        assertRulesBottomUp(automaton, "foo bar", [], [r("Foo", "foo bar", [])]);
    }
    
    @Test
    public void testDoubleQuotedName2() {
        automaton = parse("Foo -> \"\'\"");
        assertRulesBottomUp(automaton, "\'", [], [r("Foo", "\'", [])]);
    }
    
    private void assertRulesBottomUp(TreeAutomaton automaton, String label, List childStates, List<Rule> rules) {
        assertEquals(new HashSet(rules), automaton.getRulesBottomUp(s(label), childStates.collect { automaton.getIdForState(it) }));
    }

    private static TreeAutomaton parse(String s) {
        return pa(s);
    }

    private static Rule r(parent, label, children) {
        return automaton.createRule(parent, label, children, 1);
    }

    private static Rule rw(parent, label, children, weight) {
        return automaton.createRule(parent, label, children, weight);
    }
    
    private static int s(String symbol) {
        return automaton.getSignature().getIdForSymbol(symbol);
    }
}

