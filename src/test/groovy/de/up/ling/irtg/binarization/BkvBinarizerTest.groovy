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
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.codec.BolinasHrgInputCodec
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import de.up.ling.irtg.util.*
import java.util.logging.*
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
    
    private void assertBinaryGrammar(InterpretedTreeAutomaton irtg) {
        irtg.getAutomaton().getRuleSet().each {
            assert it.getArity() <= 2 : ("non-binary rule " + it.toString(irtg.getAutomaton()))
        }
    }
    
    // NB This test is a bit nondeterministic. Depending on how the hashing
    // happens, different binarization terms can be selected for "right".
    // Some of these are harder to synchronize with the homomorphism than others.
    // Thus in case of bugs in the addRuleToHomomorphism, the test case might
    // sometimes pass, sometimes fail. :)
    @Test
    public void testBinarize() {
        InterpretedTreeAutomaton irtg = pi(BIN_IRTG);
        
        Algebra leftAlgebra = irtg.getInterpretation("left").getAlgebra()
        Algebra rightAlgebra = irtg.getInterpretation("right").getAlgebra()
        
        Algebra leftOutAlgebra = new StringAlgebra()
        Algebra rightOutAlgebra = new StringAlgebra()
        
        Map newAlgebras = ["left": leftOutAlgebra, "right": rightOutAlgebra]
        Map seeds = ["left": new StringAlgebraSeed(leftAlgebra, leftOutAlgebra), "right": new StringAlgebraSeed(rightAlgebra,rightOutAlgebra)];
        
        BkvBinarizer bin = new BkvBinarizer(seeds)
        
        InterpretedTreeAutomaton binarized = bin.binarize(irtg, newAlgebras);
        assertBinaryGrammar(binarized)
        
        List bcd =  ["b", "c", "d"] //p(binarized, "left", "b c d")
        List dabc = ["d", "a", "b", "c"]  //p(binarized, "right", "d a b c")
        
        // for weights, see comment in testBinarizeTree
        assertDecoding(binarized, ["left": bcd, "right": dabc], "left", 1.0)
        assertDecoding(binarized, ["left": bcd, "right": dabc], "right", 1.0)
    }
    
    private Object p(InterpretedTreeAutomaton irtg, String interpretation, String value) {
        return irtg.getInterpretation(interpretation).getAlgebra().parseString(value);
    }
    
    private void assertDecoding(InterpretedTreeAutomaton irtg, Map values, String inputInterp, double weight) {
        Map inp = new HashMap()
        inp.put(inputInterp, values.get(inputInterp))
        
        TreeAutomaton chart = irtg.parseInputObjects(inp)
        
//        Logging.get().fine("Chart:\n" + chart)
//        Logging.get().fine("Language:\n" + chart.language())
        
        
        assert ! chart.language().isEmpty()
        
        assertAlmostEquals(weight, chart.getWeight(chart.viterbi()))
        
        chart.language().each { 
            Map vals = irtg.interpret(it)
            assertEquals(values, vals)
        }

    }
    
    @Test
    public void testBinarizeConstant() {
        InterpretedTreeAutomaton irtg = pi(BIN_CONSTANT);
        
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
        InterpretedTreeAutomaton irtg = pi(de.up.ling.irtg.algebra.TagAlgebrasTest.tinyTag)
        
        Algebra ta = irtg.getInterpretation("tree").getAlgebra()
        Algebra sa = irtg.getInterpretation("string").getAlgebra()
        
        RegularSeed tis = IdentitySeed.fromInterp(irtg, "tree")
        RegularSeed sis = IdentitySeed.fromInterp(irtg, "string")
        BkvBinarizer bin = new BkvBinarizer(["tree": tis, "string":sis])
        
        InterpretedTreeAutomaton b = bin.binarize(irtg, ["string": sa, "tree":ta])
    }
    
    @Test
    public void testBinarizationWithUnaryRules()
    {
        String hrg = '''T -> (.want :arg0 (x. :E$) :arg1 (. :T$ x.));
T -> (. :believe' :arg0 (. :girl') :arg1 (. :T$ .*)); 
T -> (.want :arg1 .*);
E -> (. :boy');
T -> (.want :arg0 (x. :E$) :arg1 (. :X$ x.));
X -> (. :believe' :arg0 (x. :girl') :arg1 (. :Y$ x. .*)); 
Y -> (.want :arg0 .*2 :arg1 .*1);
        ''';
        BolinasHrgInputCodec ic = new BolinasHrgInputCodec();
        InterpretedTreeAutomaton irta = ic.read(hrg);
        
        int p = 0;
        Iterator<Rule> it = irta.getAutomaton().getRuleSet().iterator();
        while(it.hasNext())
        {
            it.next();
            ++p;
        }
        assertEquals(p,7);
        
        Algebra al = irta.getInterpretation("Graph").getAlgebra();
        assertFalse(al == null);
        
        RegularSeed rs = IdentitySeed.fromInterp(irta, "Graph");
        assertFalse(rs == null);
        
        BkvBinarizer bin = new BkvBinarizer(["Graph": rs]);
        assertFalse(bin == null);
        
        InterpretedTreeAutomaton result = bin.binarize(irta, ["Graph": al])
        
        p = 0;
        it = result.getAutomaton().getRuleSet().iterator();
        while(it.hasNext())
        {
            it.next();
            ++p;
        }
        assertEquals(p,7);
    }
    
    private InterpretedTreeAutomaton binarizeStringTree(InterpretedTreeAutomaton irtg, String stringIntp, String treeIntp) {
        Algebra leftAlgebra = irtg.getInterpretation(stringIntp).getAlgebra()
        Algebra rightAlgebra = irtg.getInterpretation(treeIntp).getAlgebra()
        
        Algebra leftOutAlgebra = new StringAlgebra()
        Algebra rightOutAlgebra = new BinarizingTreeAlgebra()
        
        Map newAlgebras = [(stringIntp): leftOutAlgebra, (treeIntp): rightOutAlgebra]
        Map seeds = [(stringIntp): new StringAlgebraSeed(leftAlgebra, leftOutAlgebra), (treeIntp): new BinarizingAlgebraSeed(rightAlgebra,rightOutAlgebra)];
        
        BkvBinarizer bin = new BkvBinarizer(seeds)

        InterpretedTreeAutomaton binarized = bin.binarize(irtg, newAlgebras);
        assertBinaryGrammar(binarized)
        
        return binarized
    }
    
    @Test
    public void testBinarizeTree() {
        InterpretedTreeAutomaton irtg = pi(SYNC_IRTG);
        InterpretedTreeAutomaton binarized = binarizeStringTree(irtg, "left", "right")

        List leftObj = ["a", "b", "c"] 
        Object rightObj = pt("f(d, a, g(c), b)")
        
        // this used to be assert weight = 2, but as of Aug 15, 2016,
        // we normalize weights after binarization; so weights will be 1
        assertDecoding(binarized, ["left": leftObj, "right": rightObj], "left", 1.0)        
        assertDecoding(binarized, ["left": leftObj, "right": rightObj], "right", 1.0)
    }
    
    @Test
    public void testWeirdBug() {
        InterpretedTreeAutomaton irtg = pi("""\n\
interpretation string: de.up.ling.irtg.algebra.WideStringAlgebra
interpretation tree: de.up.ling.irtg.algebra.TreeWithAritiesAlgebra\n\

'.'! -> r3944 [0.009389671361502348]
  [string] conc1('?')
  [tree] '._1'('?')""");
        InterpretedTreeAutomaton binarized = binarizeStringTree(irtg, "string", "tree")
        
        Tree st = binarized.getInterpretation("string").getHomomorphism().get("r3944_br0")
        assertEquals(Tree.create("?"), st)
        
        Tree tt = binarized.getInterpretation("tree").getHomomorphism().get("r3944_br0")
        assertEquals(pt("'._1'('?')"), tt)
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
    
    private static final String SYNC_IRTG = """
interpretation left: de.up.ling.irtg.algebra.WideStringAlgebra
interpretation right: de.up.ling.irtg.algebra.TreeAlgebra

S! -> r1(A,B,C) [2]
[left] conc3(?1, ?2, ?3)
[right] f(d, ?1, g(?3), ?2)

A -> r2
[left] a
[right] a

B -> r3
[left] b
[right] b

C -> r4
[left] c
[right] c
    """;
    
    private IntSet is(List ints) {
        IntSet ret = new IntOpenHashSet()
        
        for( int i : ints ) {
            ret.add(i);
        }
        
        return ret;
    }
}

