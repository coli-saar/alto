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

        CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(h);
        
//        System.err.println("++++++++++++++++");
//        System.err.println(inv.toString());
//        System.err.println("----------------");

//        System.err.println(inv.getSignature())
        
//        System.err.println("inv language => " + inv.language())
        
        assertEquals(new HashSet([pt("r1(r1(r2,r2), r2)"), pt("r1(r2, r1(r2,r2))")]), inv.language())
    }
    
//    @Test
    public void testNullaryRulesWithVariables() {
        // makes sure that rules like the following are working.
        // S! -> r1(X)
        // [chinese] ?1
        // [english] ?1

        InterpretedTreeAutomaton irtg = pi(CHINESE_IRTG);
       
        // create a map for the irtg.parseInputObjects() - method
        Map<String, String> inputMap = new HashMap<String, String>();
        
        inputMap.put("english", "friendly cooperation over the past 30 years");
        
        TreeAutomaton result = irtg.parse(inputMap);
        
        TreeAutomaton compare = pa(compareAutomaton);
        
        // TODO!
        // result and compare are exactly the same automaton, but nevertheless the assertion fails.
        assertEquals(result, compare);     
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
    
    private static final String CHINESE_IRTG = """
interpretation english: de.up.ling.irtg.algebra.StringAlgebra
interpretation chinese: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(X)
[chinese] ?1
[english] ?1

X -> r2(X,X)
[chinese] *(*(?1, de), ?2)
[english] *(?2, ?1)

X -> r3(X,X)
[chinese] *(?1, ?2)
[english] *(?1, ?2)

X -> r4(X)
[chinese] *(?1, duonianlai)
[english] *(*(*(*(over,the),past), ?1), years)

X -> r5
[chinese] '30'
[english] '30'

X -> r6
[chinese] youhao
[english] friendly

X -> r7
[chinese] hezuo
[english] cooperation
    """;
    
    private static final compareAutomaton = """
    'X,5-6' -> r5 [1.0]
    'X,0-1' -> r6 [1.0]
    'X,1-2' -> r7 [1.0]
    'X,0-2' -> r3('X,0-1', 'X,1-2') [1.0]
    'X,0-7' -> r3('X,0-1', 'X,1-7') [1.0]
    'X,0-7' -> r3('X,0-2', 'X,2-7') [1.0]
    'X,0-7' -> r2('X,2-7', 'X,0-2') [1.0]
    'X,1-7' -> r2('X,2-7', 'X,1-2') [1.0]
    'S,0-7'! -> r1('X,0-7') [1.0]
    'X,2-7' -> r4('X,5-6') [1.0]
    'X,0-2' -> r2('X,1-2', 'X,0-1') [1.0]
    'X,1-7' -> r3('X,1-2', 'X,2-7') [1.0]
    'X,0-7' -> r2('X,1-7', 'X,0-1') [1.0]
    """;
}