package de.up.ling.irtg.automata.condensed

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.basic.Pair
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.*
import de.up.ling.irtg.util.*
import de.up.ling.irtg.util.CpuTimeStopwatch
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.Signature
import static de.up.ling.irtg.util.TestingTools.*;


class PatternMatchingInvhomAutomatonTest {
    @Test
    public void testMatchingAuto() {
        TreeAutomaton match = pa(MATCH1)
        assert match.accepts(pt("f(g(a,b),a)"))
        assert match.accepts(pt("f(g(a,f(b,a)),g(a,b))"))
        assert match.accepts(pt("g(a,f(g(a,b),a))"))
        assert ! match.accepts(pt("f(g(b,a),a)"))
        assert ! match.accepts(pt("a"))
    }

    @Test
    public void testComputeMatchingAuto() {
        Signature s = sig(["f":2, "g":2, "a":0, "b":0])
        ConcreteTreeAutomaton auto = new ConcreteTreeAutomaton()
        PatternMatchingInvhomAutomatonFactory.addToPatternMatchingAutomaton(pth("f(g(a,?1),?2)", s), "q", auto, s, true)
      
        assert auto.accepts(pt("f(g(a,b),a)"))
        assert auto.accepts(pt("f(g(a,f(b,a)),g(a,b))"))
        assert auto.accepts(pt("g(a,f(g(a,b),a))"))
        assert ! auto.accepts(pt("f(g(b,a),a)"))
        assert ! auto.accepts(pt("a"))        
    }
    
    @Test
    public void testInvhomCfg() {
        InterpretedTreeAutomaton cfg = pi(CFG)
        PatternMatchingInvhomAutomatonFactory f = new PatternMatchingInvhomAutomatonFactory(cfg.getInterpretation("i").getHomomorphism())
        f.computeMatcherFromHomomorphism()
        
        Algebra alg = cfg.getInterpretation("i").getAlgebra()
        List input = alg.parseString("john watches the woman with the telescope")
        TreeAutomaton decomp = alg.decompose(input)
        
        CpuTimeStopwatch sw = new CpuTimeStopwatch()
        sw.record(0)
        
        CondensedTreeAutomaton invhom = f.invhom(decomp)
        
        sw.record(1)
        sw.printMilliseconds("invhom")
        
        assert invhom.accepts(pt("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"));
        assert invhom.accepts(pt("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        assert ! invhom.accepts(pt("r1(r2,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        
//        System.err.println(invhom.toString())
    }
    
    
    private static final String CFG = '''\n\
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
''';

    private static final String MATCH1 = '''
q1! -> f(q0, q1)
q1  -> f(q1, q0)
q1  -> g(q0, q1)
q1  -> g(q1, q0)

q0  -> f(q0, q0)
q0  -> g(q0, q0)
q0  -> a
q0  -> b

q1! -> f(q0, t_1/)
q1  -> f(t_1/, q0)
q1  -> g(q0, t_1/)
q1  -> g(t_1/, q0)

t_1/ ! -> f(t_1/1, t_1/2)
t_1/1  -> g(t_1/11, t_1/12)
t_1/11 -> a

t_1/12 -> f(q0, q0)
t_1/12 -> g(q0, q0)
t_1/12 -> a
t_1/12 -> b

t_1/2 -> f(q0, q0)
t_1/2 -> g(q0, q0)
t_1/2 -> a
t_1/2 -> b

''';
}