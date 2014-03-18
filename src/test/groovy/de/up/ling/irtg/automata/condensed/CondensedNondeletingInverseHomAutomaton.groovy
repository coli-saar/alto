/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.automata.TreeAutomaton


/**
 *
 * @author gontrum
 */
class CondensedNondeletingInverseHomAutomatonTest {
    
    @Test
    public void testInvhomNonConcrete() {
        InterpretedTreeAutomaton irtg = pi(AB_IRTG);
        Homomorphism h = irtg.getInterpretation("i").getHomomorphism();
        Algebra a = irtg.getInterpretation("i").getAlgebra();
        
        
        TreeAutomaton decomp = a.decompose(a.parseString("a a a"));
        
//        System.err.println("=====================");
//        System.err.println(decomp);
        
        assert h.isNonDeleting();

        CondensedTreeAutomaton inv = decomp.inverseHomomorphism(h);
        
//        System.err.println("++++++++++++++++");
//        System.err.println(inv.toString());
//        System.err.println("----------------");

//        System.err.println(inv.getSignature())
        
//        System.err.println("inv language => " + inv.language())
        
        assertEquals(new HashSet([pt("r1(r1(r2,r2), r2)"), pt("r1(r2, r1(r2,r2))")]), inv.language())
    }
    
    private static final String AB_IRTG = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(S,S)
   [i] *(?1,?2)

S -> r2
   [i] a

S -> r3
   [i] b
""";
}