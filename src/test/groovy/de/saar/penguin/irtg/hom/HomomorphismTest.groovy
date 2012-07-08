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
import de.up.ling.tree.*
import static de.saar.penguin.irtg.util.TestingTools.pt;

/**
 *
 * @author koller
 */
class HomomorphismTest {
    @Test
    public void testMap() {
        Homomorphism h = hom(["f":"g(?1,h(?1))", "a":"k(b)"]);
        Tree t = pt("f(a)");
        Tree gold = pt("g(k(b),h(k(b)))");

        assertEquals(gold, h.apply(t));
    }

    @Test
    public void testMapBinary() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"]);
        Tree t = pt("f(a,c)");
        Tree gold = pt("g(l(e),h(k(b)))");
        Tree rhs = h.apply(t);

        assertEquals(gold, rhs);
    }

    @Test
    public void testNonDestructive() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"]);
        Tree t = pt("f(a,c)");
        Tree gold = pt("g(l(e),h(k(b)))");
        Tree rhs = h.apply(t); // this needs to be here

        assertEquals(TermParser.parse("g(?2,h(?1))").toTreeWithVariables(), h.get("f"));
    }
    
    @Test
    public void testGensym() {
        Homomorphism h = hom(["a":"h+3(+1)", "f":"g(?1,?2,?3)", "b":"+2"])
        Tree t = pt("f(a,a,b)");
        Tree gold = pt("g(h_2(_1), h_2(_1), _3)");
        Tree rhs = h.apply(t);
        
        assertEquals(gold, rhs)
    }
    


    public static Homomorphism hom(Map<String,String> mappings) {
        Homomorphism ret = new Homomorphism();

        mappings.each {
            ret.add(it.key, TermParser.parse(it.value).toTreeWithVariables());
        }

        return ret;
    }
}

