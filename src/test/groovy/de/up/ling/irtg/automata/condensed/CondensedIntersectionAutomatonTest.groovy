/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed

import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton

/**
 *
 * @author gontrum
 */
public class CondensedIntersectionAutomatonTest {

    private static final String grammarstring = '''
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
    
    @Test
    public void makeAllRulesExplicitTest() {
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

        
//        
//        List words = irtg.parseString("i", "a a a a b");
//
//        TreeAutomaton chart = irtg.parseInputObjects(["i": words]);
        
        
        
//        words = irtg.parseString("i", "a a a a b");
//        chart = irtg.parseInputObjects(["i": words]);
//        
//        
//        
//        words = irtg.parseString("i", "a a a a b");
//        chart = irtg.parseInputObjects(["i": words]);
//        chart.makeAllRulesExplicit();
//        System.err.println(irtg);
    } 
}




