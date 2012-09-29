/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata

import static de.saar.penguin.irtg.util.TestingTools.*;
import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*


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
	
}

