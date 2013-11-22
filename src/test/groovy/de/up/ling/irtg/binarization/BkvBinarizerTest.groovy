/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.binarization


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;
import it.unimi.dsi.fastutil.ints.*;

/**
 *
 * @author koller
 */
class BkvBinarizerTest {
    @Test
    public void testVar() {
        TreeAutomaton auto = pa("q03! -> f(q01, q13)\n q03 -> f(q02, q23)\n q02 -> f(q01,q12)\n q13 -> f(q12, q23)\n q01 -> '?1'\n q12 -> '?2'\n q23 -> '?3'")
        Int2ObjectMap<IntSet> var = BkvBinarizer.computeVar(auto);
        
        assertEquals(is([0,1,2]), var.get(auto.getIdForState("q03")))
        assertEquals(is([0,1]), var.get(auto.getIdForState("q02")))
        assertEquals(is([1,2]), var.get(auto.getIdForState("q13")))
        assertEquals(is([0]), var.get(auto.getIdForState("q01")))
        assertEquals(is([1]), var.get(auto.getIdForState("q12")))
        assertEquals(is([2]), var.get(auto.getIdForState("q23")))
    }
    
    @Test
    public void testCollectForks() {
        Tree<String> vt = pt("f('0', g('1', '2'))");
        Set<String> forks = BkvBinarizer.collectForks(vt);
        
        assertEquals(new HashSet(["0", "1", "2", "1+2", "0+1_2"]), forks)
    }
    
    
    @Test
    public void testStep4() {
//        TreeAutomaton<String> binarizationsForVartree(TreeAutomaton<String> binarizations, Tree<String> commonVariableTree, Int2ObjectMap<IntSet> var) {
        TreeAutomaton binarizations = pa("q03! -> f(q01, q13)\n q03 -> f(q02, q23)\n q02 -> f(q01,q12)\n q13 -> f(q12, q23)\n q01 -> '?1'\n q12 -> '?2'\n q23 -> '?3'")
        Int2ObjectMap<IntSet> var = BkvBinarizer.computeVar(binarizations);
        Tree<String> vt = pt("f('0', g('1', '2'))");
        
        TreeAutomaton selected = BkvBinarizer.binarizationsForVartree(binarizations, vt, var);
        
        assertEquals(new HashSet([pt("f(?1, f(?2,?3))")]), selected.language())
    }
    
    @Test
    public void testStep4b() {
        TreeAutomaton binarizations = pa("""
q1_q -> '?1' [1.0]
q2_q -> '?2' [1.0]
q3_q -> '?3' [1.0]
q_1-3 -> *(q2_q, q3_q) [1.0]
q_0-3! -> *(q_0-2, q3_q) [1.0]
q_0-3! -> *(q1_q, q_1-3) [1.0]
q_0-2 -> *(q1_q, q2_q) [1.0]     """)
        
        Int2ObjectMap<IntSet> var = BkvBinarizer.computeVar(binarizations);
        Tree<String> vt = pt("'0_1_2'('0_1'('0','1'),'2')");
        
        TreeAutomaton selected = BkvBinarizer.binarizationsForVartree(binarizations, vt, var);
        
        assertEquals(new HashSet([pt("*(*(?1,?2), ?3)")]), selected.language())
    }
    
    @Test
    public void testStep2() {
        // static TreeAutomaton<IntSet> vartreesForAutomaton(TreeAutomaton<String> automaton, Int2ObjectMap<IntSet> vars) {
        TreeAutomaton binarizations = pa("q03! -> f(q01, q13)\n q03 -> f(q02, q23)\n q02 -> f(q01,q12)\n q13 -> f(q12, q23)\n q01 -> '?1'\n q12 -> '?2'\n q23 -> '?3'")
        Int2ObjectMap<IntSet> vars = BkvBinarizer.computeVar(binarizations);
        
        TreeAutomaton vartreeAuto = BkvBinarizer.vartreesForAutomaton(binarizations, vars);
        
        assertEquals(new HashSet([pt("'0_1_2'('0_1'('0','1'),'2')"), pt("'0_1_2'('0','1_2'('1','2'))")]), vartreeAuto.language())
    }

    @Test
    public void testStep2b() {
        // static TreeAutomaton<IntSet> vartreesForAutomaton(TreeAutomaton<String> automaton, Int2ObjectMap<IntSet> vars) {
        TreeAutomaton binarizations = pa("q03! -> f(q01, q13)\n q03 -> f(q23, q02)\n q02 -> f(q01,q12)\n q13 -> f(q12, q23)\n q01 -> '?1'\n q12 -> '?2'\n q23 -> a")
        Int2ObjectMap<IntSet> vars = BkvBinarizer.computeVar(binarizations);
        
        TreeAutomaton vartreeAuto = BkvBinarizer.vartreesForAutomaton(binarizations, vars);
        
        assertEquals(new HashSet([pt("'0_1'('0','1'))")]), vartreeAuto.language())
    }
    
    @Test
    public void testMakeHomomorphism() {
        testHom("*('?3',*(a,*('?1','?2')))", " _br1(_br0('0','1'),'2')", ["_br1": "*('?2','?1')", "_br0":"*(a,*('?1','?2'))"], false)
    }
    
    @Test
    public void testMakeHomTag() {
        testHom("'@'('?4',S2('?1','@'('?3',VP1('@'('?2',V0(sleep))))))", "inx0V-sleep_br2(inx0V-sleep_br1('0',inx0V-sleep_br0('1','2')),'3')",
            ["inx0V-sleep_br0": "@('?2', VP1(@('?1',V0(sleep))))",
             "inx0V-sleep_br1": "S2('?1','?2')",
             "inx0V-sleep_br2": "@('?2','?1')"],
        false)
    }
    
    
    @Test
    public void testMakeHom2() {
        testHom("conc2(conc2('?3',a),conc2('?1','?2'))", " _br1(_br0('0','1'),'2')", ["_br0":"conc2('?1','?2')", "_br1":"conc2(conc2('?2',a),'?1')"], false)
    }
    
    
    private void testHom(String binarizationTerm, String xi, Map gold, boolean debug) {
        BkvBinarizer b = new BkvBinarizer(null)
        b.setDebug(debug)
        
        Homomorphism hom = new Homomorphism(new Signature(), new Signature());        
        hom.getSourceSignature().addAllSymbols(pt(xi));
        
        b.addEntriesToHomomorphism(hom, pt(xi), pt(binarizationTerm))
        
        gold.each { k,v -> assertEquals(pt(v), hom.get(k))  }
    }
    
    private Signature sig(InterpretedTreeAutomaton irtg, String interp) {
        return irtg.getInterpretation(interp).getAlgebra().getSignature();
    }
    
    // NB This test is a bit nondeterministic. Depending on how the hashing
    // happens, different binarization terms can be selected for "right".
    // Some of these are harder to synchronize with the homomorphism than others.
    // Thus in case of bugs in the addRuleToHomomorphism, the test case might
    // sometimes pass, sometimes fail. :)
    @Test
    public void testBinarize() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(BIN_IRTG));
        
        Algebra leftAlgebra = irtg.getInterpretation("left").getAlgebra()
        Algebra rightAlgebra = irtg.getInterpretation("right").getAlgebra()
        
        Algebra leftOutAlgebra = new StringAlgebra()
        Algebra rightOutAlgebra = new StringAlgebra()
        
        Map newAlgebras = ["left": leftOutAlgebra, "right": rightOutAlgebra]
        Map seeds = ["left": new StringAlgebraSeed(leftAlgebra, leftOutAlgebra), "right": new StringAlgebraSeed(rightAlgebra,rightOutAlgebra)];
        
        BkvBinarizer bin = new BkvBinarizer(seeds)
        
        InterpretedTreeAutomaton binarized = bin.binarize(irtg, newAlgebras);
        
        
        // check decoding from left to right
        TreeAutomaton chart = binarized.parse(["left": "b c d"])
        
        assert ! chart.language().isEmpty()
        
        chart.language().each { 
            Map vals = binarized.interpret(it)
            assertEquals(["b","c","d"], vals.get("left"))
            assertEquals(["d", "a", "b", "c"], vals.get("right"))
        }
        
        // check decoding from right to left
        chart = binarized.parse(["right": "d a b c"])
        
        assert ! chart.language().isEmpty()
        
        assertAlmostEquals(2.0, chart.getWeight(chart.viterbi()))
        
        chart.language().each { 
            Map vals = binarized.interpret(it)
            assertEquals(["b","c","d"], vals.get("left"))
            assertEquals(["d", "a", "b", "c"], vals.get("right"))
        }  
    }
    
    @Test
    public void testBinarizeConstant() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(BIN_CONSTANT));
        
        Algebra leftAlgebra = irtg.getInterpretation("left").getAlgebra()
        
        Algebra leftOutAlgebra = new StringAlgebra()
        
        Map newAlgebras = ["left": leftOutAlgebra]
        Map seeds = ["left": new StringAlgebraSeed(leftAlgebra, leftOutAlgebra)];
        
        BkvBinarizer bin = new BkvBinarizer(seeds)
        
        InterpretedTreeAutomaton binarized = bin.binarize(irtg, newAlgebras);
        
        assertEquals(new HashSet([pt("r1_br0")]), binarized.getAutomaton().language())
    }
    
    @Test
    public void testBinarizeTag() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(de.up.ling.irtg.algebra.TagAlgebrasTest.tinyTag))
        
        Algebra ta = irtg.getInterpretation("tree").getAlgebra()
        Algebra sa = irtg.getInterpretation("string").getAlgebra()
        
        RegularSeed tis = IdentitySeed.fromInterp(irtg, "tree")
        RegularSeed sis = IdentitySeed.fromInterp(irtg, "string")
        BkvBinarizer bin = new BkvBinarizer(["tree": tis, "string":sis])
        
        InterpretedTreeAutomaton b = bin.binarize(irtg, ["string": sa, "tree":ta])
    }
    
    // test grammar from ACL-13 paper
   private static final String BIN_IRTG = """
interpretation left: de.up.ling.irtg.algebra.WideStringAlgebra
interpretation right: de.up.ling.irtg.algebra.WideStringAlgebra

A! -> r1(B,C,D) [2]
[left]  conc3(?1, ?2, ?3)
[right] conc4(?3, a, ?1, ?2)

B -> r2
[left] b
[right] b

C -> r3
[left] c
[right] c

D -> r4
[left] d
[right] d
   """;

    
   private static final String BIN_CONSTANT = """
interpretation left: de.up.ling.irtg.algebra.WideStringAlgebra

A! -> r1 [2]
[left]  'a'
""";
    
    private IntSet is(List ints) {
        IntSet ret = new IntOpenHashSet()
        
        for( int i : ints ) {
            ret.add(i);
        }
        
        return ret;
    }
}

