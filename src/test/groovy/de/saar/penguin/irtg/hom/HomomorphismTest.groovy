/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.hom


import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*
import de.saar.chorus.term.*
import de.saar.basic.tree.*

/**
 *
 * @author koller
 */
class HomomorphismTest {
    @Test
    public void testMap() {
        Homomorphism h = hom(["f":"g(?1,h(?1))", "a":"k(b)"]);
        Tree t = TermParser.parse("f(a)").toTree();
        Term gold = TermParser.parse("g(k(b),h(k(b)))");
        Tree rhs = h.apply(t);
        Term rhsTerm = rhs.toTerm();

        assertEquals(gold, rhsTerm);
    }

    @Test
    public void testMapBinary() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"]);
        Tree t = TermParser.parse("f(a,c)").toTree();
        Term gold = TermParser.parse("g(l(e),h(k(b)))");
        Tree rhs = h.apply(t);
        Term rhsTerm = rhs.toTerm();

        assertEquals(gold, rhsTerm);
    }

    @Test
    public void testNonDestructive() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"]);
        Tree t = TermParser.parse("f(a,c)").toTree();
        Term gold = TermParser.parse("g(l(e),h(k(b)))");
        Tree rhs = h.apply(t);
        Term rhsTerm = rhs.toTerm();

        assertEquals(TermParser.parse("g(?2,h(?1))").toTree(), h.get(new Constant("f")));
    }


    private static Homomorphism hom(Map<String,String> mappings) {
        Homomorphism ret = new Homomorphism();

        mappings.each {
            ret.add(new Constant(it.key), TermParser.parse(it.value).toTree());
        }

        return ret;
    }
}

