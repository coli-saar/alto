/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import static org.junit.Assert.*

/**
 *
 * @author koller
 */
class BottomUpAutomatonTest {
    @Test
    public void testIntersection() {
        BottomUpAutomaton auto1 = parse("a -> q1\n f(q1 q1) -> q2 !");
        BottomUpAutomaton auto2 = parse("f(p2 p3) -> p1!\n a -> p2\n a -> p3");
        BottomUpAutomaton intersect = auto1.intersect(auto2);

        assertEquals([p("q1","p2"), p("q1", "p3")], intersect.getParentStates("a", []));
        assertEquals([p("q1","p2"), p("q1", "p3")], intersect.getParentStates("a", []));

        assertEquals([p("q2", "p1")], intersect.getParentStates("f", [p("q1","p2"), p("q1","p3")]));

        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
    }

    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }

    private static BottomUpAutomaton parse(String s) {
        return BottomUpAutomatonParser.parse(new StringReader(s));
    }
}

