package de.up.ling.irtg.automata.condensed

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.basic.Pair
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser
import de.up.ling.irtg.hom.Homomorphism
import de.up.ling.irtg.signature.IdentitySignatureMapper
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.*
import de.up.ling.irtg.util.*
import de.up.ling.irtg.util.CpuTimeStopwatch
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.Signature
import java.nio.charset.Charset
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.algebra.graph.*


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
        PMFactoryNonrestrictive.addToPatternMatchingAutomaton(pth("f(g(a,?1),?2)", s), "q", auto, s, true)
      
        assert auto.accepts(pt("f(g(a,b),a)"))
        assert auto.accepts(pt("f(g(a,f(b,a)),g(a,b))"))
        assert auto.accepts(pt("g(a,f(g(a,b),a))"))
        assert ! auto.accepts(pt("f(g(b,a),a)"))
        assert ! auto.accepts(pt("a"))        
    }
    
        

    
    @Test
    public void testInvhomCfg() {
        InterpretedTreeAutomaton cfg = pi(CFG)
        PatternMatchingInvhomAutomatonFactory f = new PMFactoryRestrictive(cfg.getInterpretation("i").getHomomorphism())
        f.computeMatcherFromHomomorphism()
        
        Algebra alg = cfg.getInterpretation("i").getAlgebra()
        List input = alg.parseString("john watches the woman with the telescope")
        TreeAutomaton decomp = alg.decompose(input)
        
//        CpuTimeStopwatch sw = new CpuTimeStopwatch()
//        sw.record(0)
        
        CondensedTreeAutomaton invhom = f.invhom(decomp)
        
//        sw.record(1)
//        sw.printMilliseconds("invhom")
        
        assert invhom.accepts(pt("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"));
        assert invhom.accepts(pt("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        assert ! invhom.accepts(pt("r1(r9,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        
//        System.err.println(invhom.toString())
    }
    
    @Test//should have a hand made explicit rhs automaton here? so that this does not depend on the graph side working.
    public void testPatternMatchingInvhomBottomUp() {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream(HRGCleanS.getBytes( Charset.defaultCharset() ) ))
        Homomorphism hom = irtg.getInterpretation("graph").getHomomorphism()
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra()
        
        PatternMatchingInvhomAutomatonFactory f = new PMFactoryRestrictive(hom)
        f.computeMatcherFromHomomorphism()
        
        String input = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 (g / girl)) :dummy g)"
        
        SGraph sgraph = alg.parseString(input)
        
        TreeAutomaton<BoundaryRepresentation> rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class)
        
        CondensedTreeAutomaton<BoundaryRepresentation> invhom = f.invhom(rhs)
        TreeAutomaton finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        
        
        assertEquals(pa(intersectionGold), finalIntAut.asConcreteTreeAutomatonWithStringStates());
    }
    
    @Test //should have a hand made explicit rhs automaton here? so that this does not depend on the graph side working.
    public void testPatternMatchingInvhomTopDown() {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream(HRGCleanS.getBytes( Charset.defaultCharset() ) ))
        Homomorphism hom = irtg.getInterpretation("graph").getHomomorphism()
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra()
        
        PatternMatchingInvhomAutomatonFactory f = new PMFactoryRestrictive(hom)
        f.computeMatcherFromHomomorphism()
        
        String input = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 (g / girl)) :dummy g)"
        
        SGraph sgraph = alg.parseString(input)
        
        TreeAutomaton<BoundaryRepresentation> rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonTopDown.class)
        
        CondensedTreeAutomaton<BoundaryRepresentation> invhom = f.invhom(rhs)
        TreeAutomaton finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        
        String res = finalIntAut.asConcreteTreeAutomatonWithStringStates().toString();
        assert res.equals(intersectionGoldTopDown) || res.equals(intersectionGoldTopDown2);//sometimes this seems to change
        
    }

    
    
    public static final String HRGCleanS = """\n\

interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

S! -> want2(NP, VP)
[string] *(?1, *(wants, *(to, ?2)))
[graph]  f_subj(f_vcomp(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2)))))

S -> want3(NP, NP, VP)
[string] *(?1, *(wants, *(?2, *(to, ?3))))
[graph] f_subj(f_obj(f_vcomp(merge(merge(merge('(u<root> / want-01  :ARG0 (v<subj>)  :ARG1 (w<vcomp>)  :dummy (x<obj>))', 
                          r_subj(?1)), 
                    r_obj(?2)), 
              r_vcomp(r_subj_obj(?3))))))

NP -> boy
[string] *(the, boy)
[graph]  '(x<root> / boy)'

NP -> girl
[string] *(the, girl)
[graph]  '(x<root> / girl)'

// every VP has a 'subj' source at which the subject is inserted
VP -> believe(S)
[string] *(believe, *(that, ?1))
[graph]  f_xcomp(merge('(u<root> / believe-01  :ARG0 (v<subj>)  :ARG1 (w<xcomp>))', r_xcomp(f_root(?1))))

S -> likes(NP,NP)
[string] *(?1, *(likes, ?2))
[graph]  f_subj(f_obj(merge(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_subj(?1)), r_obj(?2))))

VP -> go
[string] go
[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'




    """;
    
    public static final String intersectionGold = """'NP,[b<root> {b_b}]' -> boy [1.0]
'NP,[g<root> {g_g}]' -> girl [1.0]
'VP,[g<subj> {go_g}, go<root> {go_g, go_go}]' -> go [1.0]
'S,[w<root> {w_b, w_go, w_g, w_w}]'! -> want3('NP,[b<root> {b_b}]', 'NP,[g<root> {g_g}]', 'VP,[g<subj> {go_g}, go<root> {go_g, go_go}]') [1.0]\n\
"""
    public static final String intersectionGoldTopDown = """'NP,[g<root> | (g_g)]' -> girl [1.0]
'NP,[b<root> | (b_b)]' -> boy [1.0]
'VP,[go<root>, g<subj> | (go_g)+(go_go)]' -> go [1.0]
'S,[w<root> | (w_b)+(w_g,w_go)+(w_w)]'! -> want3('NP,[b<root> | (b_b)]', 'NP,[g<root> | (g_g)]', 'VP,[go<root>, g<subj> | (go_g)+(go_go)]') [1.0]\n\
"""
    
    public static final String intersectionGoldTopDown2 = """'NP,[g<root> | (g_g)]' -> girl [1.0]
'NP,[b<root> | (b_b)]' -> boy [1.0]
'VP,[go<root>, g<subj> | (go_go)+(go_g)]' -> go [1.0]
'S,[w<root> | (w_b)+(w_g,w_go)+(w_w)]'! -> want3('NP,[b<root> | (b_b)]', 'NP,[g<root> | (g_g)]', 'VP,[go<root>, g<subj> | (go_go)+(go_g)]') [1.0]
"""
    
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