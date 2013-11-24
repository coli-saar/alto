/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;
import it.unimi.dsi.fastutil.ints.*;


/**
 *
 * @author koller
 */
class SingletonAutomatonTest {
    @Test
    public void testTreeTopDown() {
        Tree t = pt("f(a,g(c))");
        SingletonAutomaton sa = new SingletonAutomaton(t)        
        assertEquals(new HashSet([t]), sa.language())
    }
    
    @Test
    public void testTreeBottomUp() {
        Tree t = pt("f(a,g(c))");
        SingletonAutomaton sa = new SingletonAutomaton(t)        
        assert sa.accepts(t);
    }

}

