/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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

/**
 *
 * @author koller
 */
class BestFirstIntersectionTest {
    @Test
    public void testParseIrtg() {
        InterpretedTreeAutomaton irtg = pi(IRTG)
        StringAlgebra alg = (StringAlgebra) irtg.getInterpretation("i").getAlgebra()
        Homomorphism hom = irtg.getInterpretation("i").getHomomorphism()
        
        List<String> words = alg.parseString("john watches the woman with the telescope")
        TreeAutomaton decomp = alg.decompose(words).inverseHomomorphism(hom)
        
        EdgeEvaluator eval = new EdgeEvaluator() {
            public double evaluate(int outputState, IntersectionAutomaton auto) {
                return 0;
            }
        }
        
        TreeAutomaton inter = new BestFirstIntersectionAutomaton(irtg.getAutomaton(), decomp, eval)
        inter.makeAllRulesExplicit()
        
        System.err.println(inter)
        
        assert inter.countTrees() == 1
    }
    
    
    private static final String IRTG = """
    interpretation i: de.up.ling.irtg.algebra.StringAlgebra




S! -> r1(NP,VP) 
  [i] *(?1,?2)


VP -> r4(V,NP) 
  [i] *(?1,?2)


VP -> r5(VP,PP)
  [i] *(?1,?2)


PP -> r6(P,NP)
  [i] *(?1,?2)


NP -> r7
  [i] john


NP -> r2(Det,N)
  [i] *(?1,?2)


V -> r8
  [i] watches


Det -> r9
  [i] the


N -> r10
  [i] woman


N -> r11
  [i] telescope

N -> r3(N,PP)
  [i] *(?1,?2)

P -> r12
  [i] with



""";
}

