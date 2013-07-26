/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata

import static de.up.ling.irtg.util.TestingTools.*;
import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.up.ling.tree.Tree


/**
 *
 * @author koller
 */
class UniversalAutomatonTest {
    @Test
    public void testAcceptance() {
        UniversalAutomaton auto = new UniversalAutomaton(sig(["f":1, "g":2, "a":0]));
        
        assert auto.accepts(pt("a"))
        assert auto.accepts(pt("f(a)"))
        assert auto.accepts(pt("g(a, f(a))"))
    }
    
    @Test
    public void testRuleTopDown() {
        UniversalAutomaton auto = new UniversalAutomaton(sig(["f":1, "a":0]));
        Iterator<Tree> it = auto.languageIterator();
        
        assertEquals(it.next(), pt("a"));
        assertEquals(it.next(), pt("f(a)"));
        assertEquals(it.next(), pt("f(f(a))"));
    }
	
}

