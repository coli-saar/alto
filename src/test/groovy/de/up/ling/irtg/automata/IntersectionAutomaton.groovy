/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata

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

/**
 *
 * @author gontrum
 */
public class IntersectionAutomatonTest {

    
    @Test
    public void makeAllRulesExplicitTest() {
        String grammarstring = '''
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(A, B)
 [i] *(?1, ?2)

A -> r2 
 [i] a

B -> r3(A, D)
 [i] *(?1, ?2)

D -> r4
 [i] b

       ''';
        
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
        List words = irtg.parseString("i", "a a b");
        TreeAutomaton chart = irtg.parseInputObjects(["i": words]);
//        chart.makeAllRulesExplicit();
//        System.err.println(irtg);
    } 
}




