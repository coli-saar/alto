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
import de.up.ling.irtg.automata.TreeAutomaton
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

        assertEquals(gold, h.apply(t));
    }

    @Test
    public void testMapBinary() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Tree t = pt("f(a,c)");
        Tree gold = pt("g(l(e),h(k(b)))");
        Tree rhs = h.apply(t);

        assertEquals(gold, rhs);
    }

    @Test
    public void testNonDestructive() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Tree t = pt("f(a,c)");
        Tree gold = pt("g(l(e),h(k(b)))");
        Tree rhs = h.apply(t); // this needs to be here

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
    public void testEquals() {
        Homomorphism h1 = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Homomorphism h2 = hom(["a":"k(b)", "f":"g(?2,h(?1))", "c":"l(e)"], sig(["a":0, "c":0, "f":2]));
        
        assert h1.equals(h2);
    }
    
    @Test
    public void testNotEquals1() {
        Homomorphism h1 = hom(["f":"x(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Homomorphism h2 = hom(["a":"k(b)", "f":"g(?2,h(?1))", "c":"l(e)"], sig(["a":0, "c":0, "f":2]));
        
        assert ! h1.equals(h2);
    }
    
    @Test
    public void testNotEquals2() {
        Homomorphism h1 = hom(["g":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["g":2, "a":0, "c":0]));
        Homomorphism h2 = hom(["a":"k(b)", "f":"g(?2,h(?1))", "c":"l(e)"], sig(["a":0, "c":0, "f":2]));
        
        assert ! h1.equals(h2);
    }
    
    @Test
    public void testNotEquals3() {
        Homomorphism h1 = hom(["f":"g(?1,h(?2))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        Homomorphism h2 = hom(["a":"k(b)", "f":"g(?2,h(?1))", "c":"l(e)"], sig(["a":0, "c":0, "f":2]));
        
        assert ! h1.equals(h2);
    }
    
    @Test
    public void testToString() {
        Homomorphism h = hom(["f":"g(?1)"], sig(["f":1]))
        
        assertEquals("g(?1)", h.rhsAsString(pth("g(?1)", h.getTargetSignature())));
        assertEquals("*(?1,?2)", h.rhsAsString(pth("*(?1,?2)", h.getTargetSignature())));
        assertEquals("'`'", h.rhsAsString(pth("\"`\"", h.getTargetSignature())));
    }
    
    @Test
    public void testPatternMatcher() {
        Homomorphism h = hom(["f":"g(?2,h(?1))", "a":"k(b)", "c":"l(e)"], sig(["f":2, "a":0, "c":0]));
        TreeAutomaton auto = h.patternMatcher();
        
        assert auto.accepts(pt("g(k(b), h(l(e)))"))
        assert auto.accepts(pt("g(l(e), h(k(b)))"))
        assert auto.accepts(pt("g(k(b), h(g(l(e), h(k(b)))))"))
        assert ! auto.accepts(pt("g(b, h(l(e)))"))
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

