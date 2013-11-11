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
        Tree binarizationTerm = pt("*('?3',*(a,*('?1','?2')))")
        Tree xi = pt(" _br1(_br0('0','1'),'2')")
        Homomorphism hom = new Homomorphism(new Signature(), new Signature());
        
        hom.getSourceSignature().addSymbol("_br0", 2);
        hom.getSourceSignature().addSymbol("_br1", 2);
        
        BkvBinarizer.addEntriesToHomomorphism(hom, xi, binarizationTerm)
        
        assertEquals(pt("*('?2','?1')"), hom.get("_br1"))
        assertEquals(pt("*(a,*('?1','?2'))"), hom.get("_br0"))
    }
    
    private Signature sig(InterpretedTreeAutomaton irtg, String interp) {
        return irtg.getInterpretation(interp).getAlgebra().getSignature();
    }
    
    @Test
    public void testBinarize() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(BIN_IRTG));
        irtg.getInterpretation("left").getAlgebra().getSignature().addSymbol("conc2", 2);
        irtg.getInterpretation("right").getAlgebra().getSignature().addSymbol("conc2", 2);
        
        Map seeds = ["left": new StringAlgebraSeed(sig(irtg, "left"), "conc2"), "right": new StringAlgebraSeed(sig(irtg, "right"), "conc2")];
        BkvBinarizer bin = new BkvBinarizer(seeds)
        
        InterpretedTreeAutomaton binarized = bin.binarize(irtg);
        
        
        System.err.println(binarized)     
        
        
        TreeAutomaton chart = binarized.parse(["left": "b c d"])
        
        System.err.println("chart: " + chart )
        
        assert ! chart.language().isEmpty()
        
//        chart.language().each { 
//            Map vals = irtg.interpret(it)
//            assertEquals(["b","c","d"], vals.get("left"))
//            assertEquals(["d", "a", "b", "c"], vals.get("right"))
//        }
//        
           
    }
    
    // test grammar from ACL-13 paper
   private static final String BIN_IRTG = """
interpretation left: de.up.ling.irtg.algebra.WideStringAlgebra
interpretation right: de.up.ling.irtg.algebra.WideStringAlgebra

A! -> r1(B,C,D)
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

    
    private IntSet is(List ints) {
        IntSet ret = new IntOpenHashSet()
        
        for( int i : ints ) {
            ret.add(i);
        }
        
        return ret;
    }
}

