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

        assertEquals(gold, h.apply(t).toTerm());
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

        assertEquals(TermParser.parse("g(?2,h(?1))").toTreeWithVariables(), h.get("f"));
    }
    
    @Test
    public void testGensym() {
        Homomorphism h = hom(["a":"h+3(+1)", "f":"g(?1,?2,?3)", "b":"+2"])
        Tree t = TermParser.parse("f(a,a,b)").toTree();
        Term gold = TermParser.parse("g(h_1(_2), h_1(_2), _3)");
        Tree rhs = h.apply(t);
        Term rhsTerm = rhs.toTerm();
        
        assertEquals(gold, rhsTerm)
    }
    


    public static Homomorphism hom(Map<String,String> mappings) {
        Homomorphism ret = new Homomorphism();

        mappings.each {
            ret.add(it.key, TermParser.parse(it.value).toTreeWithVariables());
        }

        return ret;
    }
}

