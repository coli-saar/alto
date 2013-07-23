/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.hom


import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*
import de.saar.chorus.term.*
import de.up.ling.tree.*
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class HomomorphismTest {
    @Test
    public void testMap() {
        Homomorphism h = hom(["f":"g(?1,h(?1))", "a":"k(b)"], sig(["f":1, "a":0]));
        Tree t = pt("f(a)");
        Tree gold = pt("g(k(b),h(k(b)))");

        assertEquals(gold, h.applyLabeled(t));
    }

    @Test
    public void testMapBinary() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Tree t = pt("f(a,c)");
        Tree gold = pt("g(l(e),h(k(b)))");
        Tree rhs = h.applyLabeled(t);

        assertEquals(gold, rhs);
    }

    @Test
    public void testNonDestructive() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Tree t = pt("f(a,c)");
        Tree gold = pt("g(l(e),h(k(b)))");
        Tree rhs = h.applyLabeled(t); // this needs to be here

        assertEquals(pth("g(?2,h(?1))", h.getTargetSignature()), h.get(h.getSourceSignature().getIdForSymbol("f")));
    }
    
//    @Test
    public void testGensym() {
        Homomorphism h = hom(["a":"h+3(+1)", "f":"g(?1,?2,?3)", "b":"+2"], sig(["f":3, "a":0, "b":0]))
        Tree t = pt("f(a,a,b)");
        Tree gold = pt("g(h_2(_1), h_2(_1), _3)");
        Tree rhs = h.apply(t);
        
        assertEquals(gold, rhs)
    }
    

    @Test
    public void testIndexForVar() {
        assert HomomorphismSymbol.createVariable("?1").getValue() == 0;
        assert HomomorphismSymbol.createVariable("?A3").getValue() == 2;
        assert HomomorphismSymbol.createVariable("?HalloHallo100").getValue() == 99;
    }
    
    @Test
    public void testToString() {
        //Homomorphism h = hom(["f":"g(?1)"])
        assertEquals("g(?1)", Homomorphism.rhsAsString(pth("g(?1)")));
        assertEquals("*(?1,?2)", Homomorphism.rhsAsString(pth("*(?1,?2)")));
        assertEquals("'`'", Homomorphism.rhsAsString(Tree.create("`")));
    }

    
    /*
    public static Homomorphism hom(Map<String,String> mappings) {
        Homomorphism ret = new Homomorphism();

        mappings.each {
            ret.add(it.key, TermParser.parse(it.value).toTreeWithVariables());
        }

        return ret;
    }
    */
}

