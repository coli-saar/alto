/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra

import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class SetAlgebraTest {
    
    @Test
    public void testGenerateRE() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader('''
interpretation i: de.up.ling.irtg.algebra.SetAlgebra

N! -> a_rabbit(Adj_N)
  [i] intersect_1(rabbit, ?1)

Adj_N -> b_white
  [i] white

Adj_N -> b_nop
  [i] T
        '''));
        
        SetAlgebra alg = (SetAlgebra) irtg.getInterpretation("i").getAlgebra();
        alg.setAtomicInterpretations(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])]);
        TreeAutomaton chart = irtg.parse(["i":"{r1}"]);
        
        Set result = chart.language();
        Set gold = new HashSet([pt("a_rabbit(b_white)")])
        
        assertEquals(gold, result)
    }
    
    @Test
    public void testGenerateSent() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader('''
interpretation i: de.up.ling.irtg.algebra.SetAlgebra

    N -> a_rabbit(Adj_N)
    [i] intersect_1(rabbit, ?1)

    Adj_N -> b_white
    [i] white

    Adj_N -> b_nop
    [i] T

    S! -> a_sleeps_r1(N)
    [i] project_1(intersect_2(sleep, uniq_r1(?1)))
        '''));
        
        SetAlgebra alg = (SetAlgebra) irtg.getInterpretation("i").getAlgebra();
        alg.setAtomicInterpretations(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "sleep":sl([["e", "r1"], ["f", "h"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])]);
        TreeAutomaton chart = irtg.parse(["i":"{e}"]);
        
        Set result = chart.language();
        Set gold = new HashSet([pt("a_sleeps_r1(a_rabbit(b_white))")])
        
        assertEquals(gold, result)
    }
    
    // uniqueness requirement in semantics of "sleeps" is deliberately missing => two possible REs
    @Test
    public void testGenerateSent2() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader('''
interpretation i: de.up.ling.irtg.algebra.SetAlgebra

    N -> a_rabbit(Adj_N)
    [i] intersect_1(rabbit, ?1)

    Adj_N -> b_white
    [i] white

    Adj_N -> b_nop
    [i] T

    S! -> a_sleeps_r1(N)
    [i] project_1(intersect_2(sleep, ?1))
        '''));
        
        SetAlgebra alg = (SetAlgebra) irtg.getInterpretation("i").getAlgebra();
        alg.setAtomicInterpretations(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "sleep":sl([["e", "r1"], ["f", "h"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])]);
        TreeAutomaton chart = irtg.parse(["i":"{e}"]);
        
        Set result = chart.language();
        Set gold = new HashSet([pt("a_sleeps_r1(a_rabbit(b_white))"), pt("a_sleeps_r1(a_rabbit(b_nop))")])
        
        assertEquals(gold, result)
    }
    
    @Test
    public void testDecompose() {        
        SetAlgebra alg = new SetAlgebra();
        alg.setAtomicInterpretations(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])]);
        
        TreeAutomaton auto = alg.decompose(alg.parseString("{r1}"));
        
        assert auto.accepts(pt("intersect_1(rabbit, white)"));
        assert auto.accepts(pt("project_1(intersect_1(in,rabbit))"));
        assert auto.accepts(pt("project_1(intersect_1(intersect_1(in,project_1(in)),rabbit))"));
        assert auto.accepts(pt("project_1(intersect_1(in,project_1(rabbit)))"));
        assert auto.accepts(pt("project_1(intersect_1(intersect_1(in,project_1(in)),project_1(rabbit)))"));
        assert auto.accepts(pt("project_1(intersect_1(in,project_1(project_1(rabbit))))"));
    }
    
    @Test
    public void testParse() {
        SetAlgebra a = new SetAlgebra([:]);
        String s = "{a,b,c}"
        Set<List<String>> result = a.parseString(s);
        Set<List<String>> gold = new HashSet([["a"], ["b"], ["c"]]);
        assertEquals(gold, result);
    }

    @Test
    public void testParse2() {
        SetAlgebra a = new SetAlgebra([:]);
        String s = "{(a,b),(c,d)}"
        Set<List<String>> result = a.parseString(s);
        Set<List<String>> gold = sl([["a", "b"], ["c", "d"]]);
        assertEquals(gold, result);
    }

    @Test
    public void testEvaluate() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("rabbit"));
        Set<List<String>> gold = sl([["r1"], ["r2"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate2() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("intersect_1(rabbit, white)"));
        Set<List<String>> gold = sl([["r1"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate3() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("project_1(intersect_1(in, rabbit))"));
        Set<List<String>> gold = sl([["r1"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate4() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("uniq_r1(project_1(intersect_1(in, rabbit)))"));
        Set<List<String>> gold = sl([["r1"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate5() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("uniq_r1(rabbit)"));
        Set<List<String>> gold = sl([])
        assertEquals(gold, result)
    }
    
    
    
    // commented out until we can deal with getAllStates in the SetAlgebra
    /*
    @Test
    public void testGenerateRE() {
    SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
    Set referent = a.parseString("{r1}");
    TreeAutomaton decomp = a.decompose(referent);
        
    String grammarstring = '''
    interpretation i: de.up.ling.irtg.algebra.SetAlgebra

    a_rabbit(Adj_N) -> N!
    [i] intersect_1(rabbit, ?1)

    b_white -> Adj_N
    [i] white

    b_nop -> Adj_N
    [i] T
    ''';
    InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    Homomorphism hom = irtg.getInterpretations().get("i").getHomomorphism();
         
    TreeAutomaton chart = irtg.getAutomaton().intersect(decomp.inverseHomomorphism(hom));
    chart.makeAllRulesExplicit();
        
    Set result = chart.language();
    Set gold = new HashSet([pt("a_rabbit(b_white)")])
        
    assertEquals(gold, result)
    }
    
    @Test
    public void testGenerateSent() {
    SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "sleep":sl([["e", "r1"], ["f", "h"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
    Set referent = a.parseString("{e}");
    TreeAutomaton decomp = a.decompose(referent);
        
    String grammarstring = '''
    interpretation i: de.up.ling.irtg.algebra.SetAlgebra

    a_rabbit(Adj_N) -> N
    [i] intersect_1(rabbit, ?1)

    b_white -> Adj_N
    [i] white

    b_nop -> Adj_N
    [i] T

    a_sleeps_r1(N) -> S!
    [i] project_1(intersect_2(sleep, uniq_r1(?1)))
    ''';
        
    InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    Homomorphism hom = irtg.getInterpretations().get("i").getHomomorphism();
         
    TreeAutomaton chart = irtg.getAutomaton().intersect(decomp.inverseHomomorphism(hom));
    chart.makeAllRulesExplicit();
        
    Set result = chart.language();
    Set gold = new HashSet([pt("a_sleeps_r1(a_rabbit(b_white))")])
        
    assertEquals(gold, result)
    }
    
    // uniqueness requirement in semantics of "sleeps" is deliberately missing
    @Test
    public void testGenerateSent2() {
    SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "sleep":sl([["e", "r1"], ["f", "h"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
    Set referent = a.parseString("{e}");
    TreeAutomaton decomp = a.decompose(referent);
        
    String grammarstring = '''
    interpretation i: de.up.ling.irtg.algebra.SetAlgebra

    a_rabbit(Adj_N) -> N
    [i] intersect_1(rabbit, ?1)

    b_white -> Adj_N
    [i] white

    b_nop -> Adj_N
    [i] T

    a_sleeps_r1(N) -> S!
    [i] project_1(intersect_2(sleep, ?1))
    ''';
        
    InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    Homomorphism hom = irtg.getInterpretations().get("i").getHomomorphism();
         
    TreeAutomaton chart = irtg.getAutomaton().intersect(decomp.inverseHomomorphism(hom));
    chart.makeAllRulesExplicit();
        
    Set result = new HashSet(chart.language())
    Set gold = new HashSet([pt("a_sleeps_r1(a_rabbit(b_white))"), pt("a_sleeps_r1(a_rabbit(b_nop))")])
        
    assertEquals(gold, result)
    }
     */

    private Set<List<String>> sl(List<List<String>> ll) {
        return new HashSet<List<String>>(ll);
    }
}

