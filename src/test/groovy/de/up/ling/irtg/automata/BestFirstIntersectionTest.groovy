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
    public void testParseIrtgOneReading() {
        InterpretedTreeAutomaton irtg = pi(IRTG)
        StringAlgebra alg = (StringAlgebra) irtg.getInterpretation("i").getAlgebra()
        Homomorphism hom = irtg.getInterpretation("i").getHomomorphism()
        
        List<String> words = alg.parseString("john watches the woman with the telescope")
        TreeAutomaton<StringAlgebra.Span> decomp = alg.decompose(words).inverseHomomorphism(hom)
        
        EdgeEvaluator eval = new EdgeEvaluator() {
            public double evaluate(int outputState, IntersectionAutomaton auto) {
                int rightState = auto.getRightState(outputState)
                StringAlgebra.Span span = decomp.getStateForId(rightState)
                
                // prioritize the VP from 1-4, i.e. the VP-PP reading
                if( span.start == 1 && span.end == 4 )  {
                    return 100;
                } else {
                    return 0;
                }
            }
        }
        
        TreeAutomaton inter = new BestFirstIntersectionAutomaton(irtg.getAutomaton(), decomp, eval)
        inter.makeAllRulesExplicit()
        
//        System.err.println(inter)
        
        assert inter.countTrees() == 1
        assertEquals(pt("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"), inter.viterbi())
    }
    
    @Test
    public void testParseIrtgOtherReading() {
        InterpretedTreeAutomaton irtg = pi(IRTG)
        StringAlgebra alg = (StringAlgebra) irtg.getInterpretation("i").getAlgebra()
        Homomorphism hom = irtg.getInterpretation("i").getHomomorphism()
        
        List<String> words = alg.parseString("john watches the woman with the telescope")
        TreeAutomaton<StringAlgebra.Span> decomp = alg.decompose(words).inverseHomomorphism(hom)
        
        EdgeEvaluator eval = new EdgeEvaluator() {
            
            public double evaluate(int outputState, IntersectionAutomaton auto) {
                int rightState = auto.getRightState(outputState)
                StringAlgebra.Span span = decomp.getStateForId(rightState)
                
                // prioritize the NP from 2-7, i.e. the N-PP reading
                if( span.start == 2 && span.end == 7 )  {
                    return 100;
                // deprioritize both the VP from 1-4 and the PP from 4-7, otherwise the
                // PP,4-7 can be popped off the agenda and used to combine with the VP,1-4.
                } else if( (span.start == 1 && span.end == 4) || (span.start == 4 && span.end == 7)) {
                    return -100;
                } else {
                    return 0;
                }
            }
        }
        
        TreeAutomaton inter = new BestFirstIntersectionAutomaton(irtg.getAutomaton(), decomp, eval)
        inter.makeAllRulesExplicit()
        
//        System.err.println(inter)
        
        assert inter.countTrees() == 1
        assertEquals(pt("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"), inter.viterbi())
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

