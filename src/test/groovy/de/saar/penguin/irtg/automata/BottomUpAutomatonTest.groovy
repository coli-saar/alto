/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.saar.basic.tree.*
import de.saar.penguin.irtg.hom.*
import static org.junit.Assert.*
import static de.saar.penguin.irtg.hom.HomomorphismTest.hom;


/**
 *
 * @author koller
 */
class BottomUpAutomatonTest {
    @Test
    public void testIntersection() {
        BottomUpAutomaton auto1 = parse("a -> q1\n f(q1,q1) -> q2 !");
        BottomUpAutomaton auto2 = parse("f(p2,p3) -> p1!\n a -> p2\n a -> p3");
        BottomUpAutomaton intersect = auto1.intersect(auto2);

        assertEquals(new HashSet([r(p("q1","p2"), "a", []), r(p("q1", "p3"), "a", [])]), 
            intersect.getRulesBottomUp("a", []));

        assertEquals(new HashSet([r(p("q2", "p1"), "f", [p("q1","p2"), p("q1","p3")])]), 
            intersect.getRulesBottomUp("f", [p("q1","p2"), p("q1","p3")]));

        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
    }

    @Test
    public void testRun() {
        BottomUpAutomaton auto2 = parse("f(p2,p3) -> p1!\n a -> p2\n a -> p3");

        Tree t = TermParser.parse("f(a,a)").toTree();
        assertEquals(new HashSet(["p1"]), auto2.run(t));

        Tree ta = TermParser.parse("a").toTree();
        assertEquals(new HashSet(["p2","p3"]), auto2.run(ta));
    }

    @Test
    public void testInvHom() {
        BottomUpAutomaton rhs = parse("c(q12,q23) -> q13\n c(q23,q34) -> q24\n c(q12,q24) -> q14!\n" +
                "c(q13,q34) -> q14\n a -> q12\n b -> q23\n d -> q34");
        Homomorphism h = hom(["r1":"c(?1,?2)", "r2":"c(?1,?2)", "r3":"a", "r4":"b", "r5":"d"]);

        BottomUpAutomaton gold = parse("r1(q12,q23) -> q13\n r1(q23,q34) -> q24\n r1(q12,q24) -> q14!\n" +
                "r1(q13,q34) -> q14\n r2(q12,q23) -> q13\n r2(q23,q34) -> q24\n r2(q12,q24) -> q14!\n" +
                "r2(q13,q34) -> q14\n r3 -> q12\n r4 -> q23\n r5 -> q34");

        BottomUpAutomaton pre = rhs.inverseHomomorphism(h);
        pre.makeAllRulesExplicit();
//        System.out.println(pre);

        assertEquals(gold, pre);
    }

    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }

    private static BottomUpAutomaton parse(String s) {
        return BottomUpAutomatonParser.parse(new StringReader(s));
    }

    private static Rule r(parent, label, children) {
        return new Rule(parent, label, children);
    }
}

