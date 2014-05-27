/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed

import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.saar.basic.Pair
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.signature.IdentitySignatureMapper
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton
import de.up.ling.irtg.automata.condensed.CondensedIntersectionAutomatonTest

/**
 *
 * @author gontrum
 */
public class CondensedIntersectionAutomatonTest {

    
    
   // @Test
    public void makeAllRulesExplicitTest() {
        String grammarstring = '''
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(A, B)
 [i] *(?1, ?2)

A -> r2 
 [i] a

B -> r3(A, D)
 [i] *(?1, ?2)

D -> r5(A, D) [0.5]
 [i] *(?1, ?2)

D -> r4 [0.5]
 [i] b

C -> r6(X,Y)
 [i] *(?1, ?2)

X -> r7
  [i] x

Y -> r8(X,X)
[i] *(?1, ?2)


       ''';
        // Create an IRTG
        InterpretedTreeAutomaton irtg = pi(new StringReader(grammarstring));
        String toParse = "a a a a b ";
        
        System.out.println("Parsing the String '" + toParse + "' with the IRTG: \n" + irtg.toString());
        
        // Get Homomorphism and Algebra
        Homomorphism h = irtg.getInterpretation("i").getHomomorphism();
        Algebra a = irtg.getInterpretation("i").getAlgebra();
        
        // Create a decomposition of an input String
        TreeAutomaton decomp = a.decompose(a.parseString(toParse));
//        System.out.println("Decomp Automaton for the input string:\n" + decomp.toString());
        
        // Create an (Nondeleting) InverseHomomorphism
        CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(h);
//        System.out.println("Invhom Automaton (condensed):\n" + inv.toStringCondensed());
//        System.out.println("Invhom Automaton :\n" + inv.toString());

//        System.err.println("invhom(decomp(" + input  + "):\n" + inv.toStringBottomUp());
                
        TreeAutomaton<String> result = irtg.getAutomaton().intersectCondensed(inv, h.getSignatureMapper());
    } 
    
    @Test
    public void testIntersection() {
        TreeAutomaton auto1 = pa("q1 -> a\n q2 ! -> f(q1,q1) ");       
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p3) \n p2 -> {a}  \n p3 -> {a}");
        
        TreeAutomaton intersect = auto1.intersectCondensed(auto2);
        intersect.getStateInterner().setTrustingMode(false); // important!

        assert intersect.getSignature() == auto1.getSignature();
        
        assertEquals(intersect.getSignature().getSymbolsWithArities(), ["f":2, "a":0]);
     
        assertEquals(new HashSet([rs(p("q1","p2"), "a", [], intersect), rs(p("q1", "p3"), "a", [], intersect)]), 
            rbu("a", [], intersect));

        assertEquals(new HashSet([rs(p("q2", "p1"), "f", [p("q1","p2"), p("q1","p3")], intersect)]), 
            rbu("f", [p("q1","p2"), p("q1","p3")], intersect));
        
        assertEquals(new HashSet([p("q2","p1")]), new HashSet(intersect.getFinalStates().collect { intersect.getStateForId(it)}));
    }
    
    @Test
    public void testIntersectionLanguage() {
        
        TreeAutomaton auto1 = pa("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b");
        TreeAutomaton auto2 = pac("p1! -> {f}(p2,p2)\n p2 -> {a}");
        
        TreeAutomaton intersect = auto1.intersectCondensed(auto2);
        
        assertEquals(new HashSet([pt("f(a,a)")]), intersect.language());
    }
    
    @Test
    public void testIntersectionFinalStates() {
        TreeAutomaton auto1 = pa("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b\n qqqq! -> xxxx");
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p2)\n p2 -> {a}");
        TreeAutomaton intersect = auto1.intersectCondensed(auto2);
        
        assertEquals(new HashSet(["q1,p1"]), new HashSet(intersect.getFinalStates().collect { intersect.getStateForId(it).toString() }));
    }
    
    // not implemented yet
//    @Test
//    public void testInvHomNonlinearByIntersection() {
//        TreeAutomaton rhs = pa("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
//        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]), rhs.getSignature());
//        
//        CondensedTreeAutomaton pre = rhs.inverseCondensedHomomorphism(h);
//        
//        TreeAutomaton left = parse("p2! -> G(p1,p1) \n p1 -> A"); // accepts { G(A,A) }
//        TreeAutomaton result = left.intersectCondensed(pre)
//        
//        assert result.accepts(pt("G(A,A)"));
//    }
    
    @Test
    public void testIntersectWeights() {
        TreeAutomaton auto1 = pa("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p3) \n p2 -> {a} [0.4]\n p3 -> {a} [0.6]");
        TreeAutomaton intersect = auto1.intersectCondensed(auto2);
        
        Set rulesForA = rbu("a", [], intersect);
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w-0.3) < 0.001) || (Math.abs(w-0.2) < 0.001)) : rulesForA;
        }
        
//        assertEquals( new HashSet([0.4*0.5, 0.6*0.5]), new HashSet(rulesForA.collect { it.getWeight() }) )
    }
    
    @Test
    public void testIntersectWeightsViaExplicit() {
        TreeAutomaton auto1 = pa("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p3) \n p2 -> {a} [0.4]\n p3 -> {a} [0.6]");
        TreeAutomaton intersect = auto1.intersectCondensed(auto2);
        
        intersect.makeAllRulesExplicit();        
        Set rulesForA = rbu("a", [], intersect);
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w)-0.3 < 0.001) || (Math.abs(w)-0.2 < 0.001)) : rulesForA;
        }
    }
    
    //    @Test
    public void testBottomUpOrder() {
        CondensedTreeAutomaton a = pac("s03! -> {f}(s01,s13)\n s03 -> {f}(s02,s23)\n s13 -> {f}(s12, s23)\n s02 -> {f}(s01, s12)\n s01 -> {a}\n s12 -> {a}\n s23 -> {a}");
        List states = a.getStatesInBottomUpOrder().collect { a.getStateForId(it)};
        
        assertEquals(["s01", "s12", "s23", "s02", "s13", "s03"], states)
    }
    
    
    // Helping functions
    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }
    
    private static Rule rs(parent, String label, children, TreeAutomaton automaton) {
        return automaton.createRule(parent, label, children, 1);
    }
    

}




