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
class BinarizingAlgebraTest {
   @Test
   public void testBinarizingTree() {
       BinarizingTreeAlgebra alg = new BinarizingTreeAlgebra();
       Tree t = pt("f(a,b,g(d,e,e))")
       
       assertEquals(t, alg.evaluate(pt("f(_@_(_@_(a,b),g(_@_(_@_(d,e),e))))")))

       TreeAutomaton decomp = alg.decompose(pt("f(a,b,g(d,e,e))"))
           
       assert decomp.accepts(pt("f(_@_(_@_(a,b),g(_@_(_@_(d,e),e))))"))
       
       Set lang = decomp.language()
       lang.each { it -> assertEquals(t, alg.evaluate(it)) }
   }
}

