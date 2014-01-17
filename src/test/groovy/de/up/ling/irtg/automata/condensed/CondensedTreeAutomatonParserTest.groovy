/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser
/**
 *
 * @author gontrum/koller
 */
class CondensedTreeAutomatonParserTest {
//    static CondensedTreeAutomaton automaton;
    
    @Test
    public void testParserNotNull() {
        CondensedTreeAutomaton automaton = parse("q2! -> {f}(q1,q1)");
        assert automaton != null;
    }

    @Test
    public void testParserNoNewlines() {
        CondensedTreeAutomaton automaton = parse("q1 -> {a} q2! -> {f}(q1,q1)");

        assert automaton != null;
    }
    
    @Test
    public void testParserWithComments() {
        CondensedTreeAutomaton automaton = parse("q1 -> {a} /* foo -> {bar} */ q2! -> {f}(q1,  /* lalala */ q1)");

        assert automaton != null;
    }

    
//    @Test
//    public void testParser1() {
//        automaton = parse("q1 -> a \n q2 ! -> f(q1,q1)");        
//
//        assertRulesBottomUp(automaton, "a", [], [r("q1", "a", [])]);
//        assertRulesBottomUp(automaton, "f", ["q1", "q1"], [r("q2", "f", ["q1","q1"])]);
//        assertEquals(new HashSet([automaton.getIdForState("q2")]), automaton.getFinalStates());
//    }
//
//    @Test
//    public void testParser2() {
//        automaton = parse("p1! -> f(p2,p3)\n p2 -> a\n p3 -> a");
//        assertRulesBottomUp(automaton, "a", [], [r("p2", "a", []), r("p3", "a", [])]);
//    }
//
//    @Test
//    public void testWeights() {
//        automaton = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
//        assertRulesBottomUp(automaton, "a", [], [rw("q1", "a", [], 2)]);
//        assertRulesBottomUp(automaton, "f", ["q1", "q2"], [rw("q", "f", ["q1", "q2"], 1.5)]);
//    }
//    
//    @Test
//    public void testQuotedName() {
//        automaton = parse("Foo -> \'foo bar\'");
//        assertRulesBottomUp(automaton, "foo bar", [], [r("Foo", "foo bar", [])]);        
//    }
//    
//    @Test
//    public void testQuotedName2() {
//        automaton = parse("Foo -> \'\"\'");
//        assertRulesBottomUp(automaton, "\"", [], [r("Foo", "\"", [])]);
//    }
//    
//    @Test
//    public void testDoubleQuotedName() {
//        automaton = parse("Foo -> \"foo bar\"");
//        assertRulesBottomUp(automaton, "foo bar", [], [r("Foo", "foo bar", [])]);
//    }
//    
//    @Test
//    public void testDoubleQuotedName2() {
//        automaton = parse("Foo -> \"\'\"");
//        assertRulesBottomUp(automaton, "\'", [], [r("Foo", "\'", [])]);
//    }
//    
//    private void assertRulesBottomUp(TreeAutomaton automaton, String label, List childStates, List<Rule> rules) {
//        assertEquals(new HashSet(rules), automaton.getRulesBottomUp(s(label), childStates.collect { automaton.getIdForState(it) }));
//    }
//
    private CondensedTreeAutomaton parse(String s) {
        return CondensedTreeAutomatonParser.parse(new StringReader(s));
    }

}

