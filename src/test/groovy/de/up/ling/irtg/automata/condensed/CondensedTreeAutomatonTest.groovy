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
import de.up.ling.irtg.automata.TreeAutomatonParser
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.signature.Signature

/**
 *
 * @author gontrum/koller
 */
class CondensedTreeAutomatonTest {
    static CondensedTreeAutomaton automaton;
    
    @Test
    public void testToAutomaton() {
        System.out.println("\nCondensedTreeAutomaton:")
        CondensedTreeAutomaton automaton = parse("q! -> {f,g}(q2) q2! -> {a}");
        System.out.println("toString BottomUp:\n" + automaton.toStringBottomUp());
        System.out.println("toString TopDown:\n" + automaton.toString());
        System.out.println("toString Language:\n" + automaton.language());
        for (CondensedRule cr : automaton.getCondensedRuleSet()) {
            System.out.println(cr.toString(automaton));
        }
    }
    
    @Test
    public void testTAtoCTA() {
        System.out.println("\nCTA from TA:")
        CondensedTreeAutomaton automaton = parseTA("q! -> f(q2) q! -> g(q2) q2! -> a");
        System.out.println(automaton.toStringBottomUp());
        System.out.println(automaton.language());
        for (CondensedRule cr : automaton.getCondensedRuleSet()) {
            System.out.println(cr.toString(automaton));
        }
    }
    
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

    
    private CondensedTreeAutomaton parse(String s) {
        return CondensedTreeAutomatonParser.parse(new StringReader(s));
    }
    
    private CondensedTreeAutomaton parseTA(String s) {
        return new ConcreteCondensedTreeAutomaton(TreeAutomatonParser.parse(new StringReader(s)));
    }

}

