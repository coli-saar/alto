/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra


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
class TreeAlgebraTest {
	@Test
        public void testWithArities() {
            TreeWithAritiesAlgebra a = new TreeWithAritiesAlgebra();
            Tree t = a.parseString("f(a,g(b))")
            Tree tw = pt("f_2(a_0, g_1(b_0))")
            
            TreeAutomaton auto = a.decompose(t)
            assertEquals(new HashSet([tw]), auto.language())
            
            assertEquals(t, a.evaluate(tw))            
        }
        
        @Test
        public void testWithArities2() {
            TreeWithAritiesAlgebra a = new TreeWithAritiesAlgebra();
            Tree t = a.parseString("a(a,a(b))")
            Tree tw = pt("a_2(a_0, a_1(b_0))")
            
            TreeAutomaton auto = a.decompose(t)
            assertEquals(new HashSet([tw]), auto.language())
            
            assertEquals(t, a.evaluate(tw))            
        }
        
    @Test
    public void testAddArities() {
        Tree t = pt("f('?1', g('?2'), b)");
        Tree tt = TreeWithAritiesAlgebra.addArities(t);
        assertEquals(pt("f_3('?1', g_1('?2'), b_0)"), tt)
    }
}

