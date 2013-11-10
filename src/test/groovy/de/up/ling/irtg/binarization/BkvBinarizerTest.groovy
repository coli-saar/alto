/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.binarization


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
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
class BkvBinarizerTest {
    @Test
    public void testVar() {
        TreeAutomaton auto = pa("q03! -> f(q01, q13)\n q03 -> f(q02, q23)\n q02 -> f(q01,q12)\n q13 -> f(q12, q23)\n q01 -> '?1'\n q12 -> '?2'\n q23 -> '?3'")
        Int2ObjectMap<IntSet> var = BkvBinarizer.computeVar(auto);
        
        assertEquals(is([0,1,2]), var.get(auto.getIdForState("q03")))
        assertEquals(is([0,1]), var.get(auto.getIdForState("q02")))
        assertEquals(is([1,2]), var.get(auto.getIdForState("q13")))
        assertEquals(is([0]), var.get(auto.getIdForState("q01")))
        assertEquals(is([1]), var.get(auto.getIdForState("q12")))
        assertEquals(is([2]), var.get(auto.getIdForState("q23")))
    }
    
    private IntSet is(List ints) {
        IntSet ret = new IntOpenHashSet()
        
        for( int i : ints ) {
            ret.add(i);
        }
        
        return ret;
    }
}

