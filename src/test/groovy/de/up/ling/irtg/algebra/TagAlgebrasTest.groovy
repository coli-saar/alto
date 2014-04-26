/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra

import org.junit.*
import static org.junit.Assert.*
import de.up.ling.irtg.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.hom.Homomorphism
import static de.up.ling.irtg.util.TestingTools.*
import de.up.ling.tree.Tree
import de.saar.basic.Pair

class TagAlgebrasTest {
    
    //@Test
    public void testTinyTagAdjective() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));
        
        TreeAutomaton chart = irtg.parse(["string": "the small businessman sleep"]);
        
        assert chart.accepts(pt("inx0V-sleep(iNXN-businessman(aAn-small(*NOP*,*NOP*),aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)"))
        
        assertEquals(new HashSet([pt("inx0V-sleep(iNXN-businessman(aAn-small(*NOP*,*NOP*),aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)")]), chart.language());
    }
    
    //@Test
    public void testTinyTagPre() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));
        Interpretation i = irtg.getInterpretations().get("string")
        Algebra a = i.getAlgebra()
        
        Tree dt = pt("inx0V-sleep(iNXN-businessman(*NOP*,aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)")
        TreeAutomaton decomp = a.decompose(a.parseString("the businessman sleep"))
        
        assert decomp.accepts(i.getHomomorphism().apply(dt))
    }
    
    //@Test
    public void testTinyTag() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));        
        TreeAutomaton chart = irtg.parse(["string": "the businessman sleep"]);
        assertEquals(new HashSet([pt("inx0V-sleep(iNXN-businessman(*NOP*,aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)")]), chart.language());
    }
    
    //@Test
    public void testTinyTagTree() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));        
        TreeAutomaton chart = irtg.parse(["tree": "S2(NP2(D0(the),NP1(N0(businessman))),VP1(V0(sleep)))"]);        
        assertEquals(new HashSet([pt("inx0V-sleep(iNXN-businessman(*NOP*,aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)")]), chart.language());
    }
    
    //@Test
    public void testTinyTagAdjectivePre() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));
        Interpretation si = irtg.getInterpretations().get("string");
        Tree dt = pt("inx0V-sleep(iNXN-businessman(aAn-small(*NOP*,*NOP*),aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)")        
        Tree ht = si.getHomomorphism().apply(dt);
        assertEquals(si.getAlgebra().parseString("the small businessman sleep"), si.getAlgebra().evaluate(ht));
    }

    //@Test
    public void testTinyTagAdjectiveDecomp() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));
        Interpretation si = irtg.getInterpretations().get("string");
        Tree dt = pt("inx0V-sleep(iNXN-businessman(aAn-small(*NOP*,*NOP*),aDnx-the(*NOP*,*NOP*)),*NOP*,*NOP*,*NOP*)")        
        Tree ht = si.getHomomorphism().apply(dt);
        assert si.getAlgebra().decompose(si.getAlgebra().parseString("the small businessman sleep")).accepts(ht);
    }
    
    
    //@Test
    public void testNessonShieberPrePos() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(nessonShieber));
        
        Interpretation i = irtg.getInterpretations().get("syntax")
        Algebra a = i.getAlgebra()
        
        Tree dt = pt("a1(a2,a3,b4)")        
        assertEquals(pt("s(np(john), vp(adv(apparently), vp(v(likes), np(mary))))"), a.evaluate(i.getHomomorphism().apply(dt)))
        
        TreeAutomaton decomp = a.decompose(a.parseString("s(np(john), vp(adv(apparently), vp(v(likes), np(mary))))"))        
        assert decomp.accepts(i.getHomomorphism().apply(dt))
    }

    //@Test
    public void testNessonShieberPreNeg() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(nessonShieber));
        
        Interpretation i = irtg.getInterpretations().get("syntax")
        Algebra a = i.getAlgebra()
        
        Tree dt = pt("a1(a2,a2,b4)")
        assertEquals(pt("s(np(john), vp(adv(apparently), vp(v(likes), np(john))))"), a.evaluate(i.getHomomorphism().apply(dt)))
        
        TreeAutomaton decomp = a.decompose(a.parseString("s(np(john), vp(adv(apparently), vp(v(likes), np(mary))))"))        
        assert ! decomp.accepts(i.getHomomorphism().apply(dt))
    }
    
    //@Test
    public void testNessonShieber() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(nessonShieber));
        TreeAutomaton chart = irtg.parse(["syntax": "s(np(john), vp(adv(apparently), vp(v(likes), np(mary))))"]);
        assertEquals(new HashSet([pt("a1(a2,a3,b4)")]), chart.language());
    }


    //@Test
    public void testNessonShieberGen() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(nessonShieber));
        TreeAutomaton chart = irtg.parse(["semantics": "t(t_t(apparently),t(e_t(likes,e(mary)),e(john)))"]);        
        assertEquals(new HashSet([pt("a1(a2,a3,b4)")]), chart.language());
    }
    
    
    
    //@Test
    public void testTinyTagBottomUp() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(tinyTag));        
        Algebra alg = irtg.getInterpretations().get("tree").getAlgebra();
        
        Tree tree = alg.parseString("S2(NP2(D0(the),NP1(N0(businessman))),VP1(V0(sleep)))");
        TreeAutomaton decomp = alg.decompose(tree);
        
        assert decomp.accepts(tree);        
    }
    
    //@Test
    public void testTinyTagTopDown() {
        InterpretedTreeAutomaton g2 = pi(new StringReader(tinyTag));
        Algebra alg = g2.getInterpretations().get("tree").getAlgebra();
        TreeAutomaton univ = new UniversalAutomaton(alg.getSignature());
        
        Tree tree = alg.parseString("S2(NP2(D0(the),NP1(N0(businessman))),VP1(V0(sleep)))");
        TreeAutomaton decomp = alg.decompose(tree);
        

        Iterator it = decomp.languageIterator();        
        assert it.hasNext();
        
        for( int i = 0; i < 100 && it.hasNext(); i++ ) {
            Tree term = it.next();
            Tree result = alg.evaluate(term);
            assert tree.equals(result) : result;
        }
    }
        
    
    
    
    
    public static final String tinyTag = """
interpretation tree: de.up.ling.irtg.algebra.TagTreeAlgebra
interpretation string: de.up.ling.irtg.algebra.TagStringAlgebra

S_S! -> inx0V-sleep(NP_S, V_A, VP_A, S_A) [1.0]
  [tree] @(?4, S2(?1, @(?3, VP1(@(?2, V0(sleep))))))
  [string] *WRAP21*(?4, *CONC11*(?1, *WRAP21*(?3, *WRAP21*(?2, sleep))))

NP_S -> iNXN-businessman(N_A, NP_A)  [1.0]
  [tree] @(?2, NP1(@(?1, N0(businessman))))
  [string] *WRAP21*(?2, *WRAP21*(?1, businessman))

NP_A -> aDnx-the(D_A, NP_A)  [1.0]
  [tree] @(?2, NP2(@(?1, D0(the)), *))
  [string] *WRAP22*(?2, *CONC12*(*WRAP21*(?1, the), *EE*))

N_A -> aAn-small(A_A, N_A)  [1.0]
  [tree] @(?2, N2(@(?1, A0(small)), *))
  [string] *WRAP22*(?2, *CONC12*(*WRAP21*(?1, small), *EE*))

NP_A -> *NOP*  [1.0]
  [tree] *
  [string] *EE*

N_A -> *NOP* [1.0]
  [tree] *
  [string] *EE*

VP_A -> *NOP* [1.0]
  [tree] *
  [string] *EE*

V_A -> *NOP* [1.0]
  [tree] *
  [string] *EE*

S_A -> *NOP* [1.0]
  [tree] *
  [string] *EE*

D_A -> *NOP* [1.0]
  [tree] *
  [string] *EE*

A_A -> *NOP* [1.0]
  [tree] *
  [string] *EE*

    """;

    private static final String nessonShieber = """
    interpretation syntax: de.up.ling.irtg.algebra.TagTreeAlgebra
interpretation semantics: de.up.ling.irtg.algebra.TagTreeAlgebra

S! -> a1(NP, NP, VPa)
[syntax]    s(?1, @(?3, vp(v(likes), ?2)))
[semantics] @(?3, t(e_t(likes, ?2), ?1))

NP -> a2
[syntax]    np(john)
[semantics] e(john)

NP -> a3
[syntax]    np(mary)
[semantics] e(mary)

VPa -> b4
[syntax]    vp(adv(apparently), *)
[semantics] t(t_t(apparently), *)
""";
    
}


