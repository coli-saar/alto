/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.basic.Pair
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.signature.Signature
import de.up.ling.irtg.util.Logging
import static de.up.ling.irtg.util.TestingTools.*

/**
 *
 * @author gontrum/koller
 */
class CondensedTreeAutomatonTest {
    static CondensedTreeAutomaton automaton;
    
    @Test
    public void testToAutomaton() {
//        System.out.println("\nCondensedTreeAutomaton:")
        CondensedTreeAutomaton automaton = parse("q! -> {f,g}(q2) q2! -> {a}");
//        System.out.println("toString BottomUp:\n" + automaton.toStringBottomUp());
//        System.out.println("toString TopDown:\n" + automaton.toString());
//        System.out.println("toString Language:\n" + automaton.language());
        for (CondensedRule cr : automaton.getCondensedRuleSet()) {
//            System.out.println(cr.toString(automaton));
        }
    }
    
    @Test
    public void testTAtoCTA() {
//        System.out.println("\nCTA from TA:")
        TreeAutomaton automaton = pa("q! -> f(q2) q! -> g(q2) q2! -> a");
        CondensedTreeAutomaton condensed = ConcreteCondensedTreeAutomaton.fromTreeAutomaton(automaton)
        assertEquals(automaton, condensed)
//        System.out.println("toString BottomUp:\n" + automaton.toStringBottomUp());
//        System.out.println("toString TopDown:\n" + automaton.toString());
//        System.out.println("toString Language:\n" + automaton.language());
//        for (CondensedRule cr : automaton.getCondensedRuleSet()) {
//            System.out.println(cr.toString(automaton));
//        }
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
        return ConcreteCondensedTreeAutomaton.fromTreeAutomaton(pa(s));
    }
    
    static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }
    
    static Rule rs(parent, String[] label, children, CondensedTreeAutomaton automaton) {
        return automaton.createRule(parent, label, children, 1);
    }
    
    static String[] sarr(String s) {
        String[] ret = new String[1];
        ret[0] = s;
        return ret;
    }
    
    static Set<Rule> rbu(String label, List children, CondensedTreeAutomaton auto) {
        return auto.getRulesBottomUp(auto.getSignature().getIdForSymbol(label), children.collect { auto.getIdForState(it)});
    }

}

