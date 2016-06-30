/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.siblingfinder

import org.junit.Test
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.ConcreteTreeAutomaton
import de.up.ling.irtg.automata.ConcreteTreeAutomaton
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.tree.Tree
import static org.junit.Assert.*

/**
 *
 * @author groschwitz
 */
class SiblingFinderIntersectionTest {

    
    @Test
    public void TestStringInvhomIntersection() {
        String grammarstring = '''
            /* declarating the interpretations */
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra  /* another comment */

            /* automaton starts here */

S! -> r1(NP,VP)
  [i] *(?1,?2)



VP -> r2(VP,NP) [.6]
  [i] *(?1,*(on, ?2))

VP -> r2Wrong(VP,VP) [.8]
  [i] *(?1,*(on, ?2))

S -> r3(S,NP) [.4]
  [i] *(?1,*(on, ?2))


NP -> r4 
  [i] john


VP -> r5 
  [i] walks


NP -> r6
  [i] mars


        ''';
        
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromString(grammarstring);
        List<String> inputString = irtg.getInterpretations().get("i").getAlgebra().parseString("john walks on mars");
        //decomposition
        TreeAutomaton decompAuto = irtg.getInterpretations().get("i").getAlgebra().decompose(inputString);
        //invhom  
        SiblingFinderInvhom invhom = new SiblingFinderInvhom(decompAuto, irtg.getInterpretations().get("i").getHomomorphism());
        //intersection
        SiblingFinderIntersection intersect = new SiblingFinderIntersection((ConcreteTreeAutomaton)irtg.getAutomaton(), invhom);
        intersect.makeAllRulesExplicit();
        //viterbi
        Tree<String> res = intersect.seenRulesAsAutomaton().viterbi();
        assert res.toString().equals("r1(r4,r2(r5,r6))")
        
    }
}
