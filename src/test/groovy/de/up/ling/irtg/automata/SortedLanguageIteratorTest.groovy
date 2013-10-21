/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
public class SortedLanguageIteratorTest {
    private _assertTreeEquals(WeightedTree result, String gold, TreeAutomaton a) {
        assert result.getTree().equals(pti(gold, a.getSignature()));
    }
    
    @Test
    public void testSortedLanguageIteratorGontrumBug() {
        TreeAutomaton a = pa("S! -> r1(A, B) [1.0]\n A -> r2 [1.0]\n A -> r3(A) [0.0]\n B -> r4(B,B) [0.7]\n B -> r5 [0.3]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        _assertTreeEquals(w, "r1(r2,r5)", a)
        
        w = it.next();
        _assertTreeEquals(w, "r1(r2,r4(r5,r5))", a)
    }
    
    @Test
    public void testStrangeMerging() {
        TreeAutomaton a = pa("S! -> f(A,B) [0.9]\n  S! -> g(B) [0.1]\n A -> h(A,B) [0]\n A -> a [0.7]\n  A -> b [0.3]\n    B -> c [1]")
        
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        _assertTreeEquals(w, "f(a,c)", a);
        assert w.getWeight() == 0.63
        
        w = it.next();
        _assertTreeEquals(w, "f(b,c)", a);
        assert w.getWeight() == 0.27
        
        w = it.next();
        _assertTreeEquals(w, "g(c)", a);
        assert w.getWeight() == 0.1
    }
    
    @Test
    public void testOneRuleNonRecursiveAutomaton() {
//        System.err.println("\n\n*** testOneRuleNonRecursiveAutomaton ***");
        TreeAutomaton a = pa("qb -> b [0.5]\n qa! -> f(qb) [0.7]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        _assertTreeEquals(w, "f(b)", a);
        assert w.getWeight() == 0.35
        
        assert ! it.hasNext()
    }
    
    @Test
    public void testMultiRulesNonRecursiveAutomaton() {
//        System.err.println("\n\n*** testMultiRulesNonRecursiveAutomaton ***");
        
        TreeAutomaton a = pa("qb -> b [0.5]\n qa! -> f(qb) [0.7]\n qa! -> g(qb) [0.3]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        _assertTreeEquals(w, "f(b)", a);
        assert w.getWeight() == 0.35

        w = it.next();
        _assertTreeEquals(w, "g(b)", a);
        assert w.getWeight() == 0.15
        
        assert ! it.hasNext()
    }
    
    @Test
    public void testRecursive() {
//        System.err.println("\n\n*** testRecursive ***");
        TreeAutomaton a = pa("qb -> b [0.5]\n qb! -> f(qb) [0.5]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assert w != null : "tree 1 null"
        _assertTreeEquals(w, "b", a);
        assert w.getWeight() == 0.5

        w = it.next();
        assert w != null : "tree 2 null"
        _assertTreeEquals(w, "f(b)", a);
        assert w.getWeight() == 0.25

        w = it.next();
        assert w != null : "tree 3 null"
        _assertTreeEquals(w, "f(f(b))", a);
        assert w.getWeight() == 0.125
        
        assert it.hasNext()
    }
    
    @Test
    public void testMultiFinalStates() {
        TreeAutomaton a = pa("qb -> b [0.5]\n qb! -> f(qb) [0.5]\n qa! -> g(qb) [0.4]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assert w != null : "tree 1 null"
        _assertTreeEquals(w, "b", a);
        assert w.getWeight() == 0.5

        w = it.next();
        assert w != null : "tree 2 null"
        _assertTreeEquals(w, "f(b)", a);
        assert w.getWeight() == 0.25
        
        w = it.next();
        assert w != null : "tree 3 null"
        _assertTreeEquals(w, "g(b)", a);
        assert w.getWeight() == 0.2

        w = it.next();
        assert w != null : "tree 4 null"
        _assertTreeEquals(w, "f(f(b))", a);
        assert w.getWeight() == 0.125
        
        w = it.next();
        assert w != null : "tree 5 null"
        _assertTreeEquals(w, "g(f(b))", a);
        assert w.getWeight() == 0.1
        
        assert it.hasNext()
    }
    
    private List<Tree<String>> collectSortedIterator(Iterator<WeightedTree> it) {
        List<Tree<String>> ret = new ArrayList<Tree<String>>();
        
        while( it.hasNext() ) {
            WeightedTree wt = it.next();
            ret.add(wt.getTree())
        }
        
        return ret;
    }
    
    private List<Tree<String>> collectTrees(TreeAutomaton auto) {
        return collectSortedIterator(auto.sortedLanguageIterator());
    }
    
    @Test
    public void testLanguage() {
        TreeAutomaton auto = pa("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2)  [1.5]");
        Signature sig = auto.getSignature();
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet([pti("f(a,a)", sig), pti("f(a,b)", sig)]*.toString());
        assertEquals(gold, lang);
    }
    
    // TODO:         
    // TreeAutomaton auto = pa("q1 -> a \n q1 -> b \n q2 -> c \n q2 -> d \n q! -> f(q1,q2) \n q1 -> g(q1,q2) ");
    // exceeds heap

    
    @Test
    public void testLanguage2() {
        TreeAutomaton auto = pa("q1 -> a \n q1 -> b \n q2 -> c \n q2 -> d \n q! -> f(q1,q2) \n q! -> g(q1,q2) ");
        Signature sig = auto.getSignature();
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pti(it,sig).toString()});
        assertEquals(gold, lang)
    }
    
    @Test
    public void testEmptyLanguageIterator() {
        TreeAutomaton auto = pa("q! -> g(q1,q2) ");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet();        
        assertEquals(gold, lang)
    }
    
    @Test
    public void testPtbDecoded() {
        TreeAutomaton auto = pa(PTB_DECODED);
        Set lang = new HashSet(collectTrees(auto)*.toString());
        assert lang.size() == 1;
//        System.err.println(lang);
    }

    
    private static final String PTB_DECODED = """
MD1_5-6 -> MD1(qh7) [1.0]
NNP1_0-1 -> NNP1(qh4) [1.0]
NNP1_1-2 -> NNP1(qh20) [1.0]
NNP1_7-8 -> NNP1(qh16) [1.0]
NNP1_8-9 -> NNP1(qh6) [1.0]
NP-SBJ1_4-5 -> NP-SBJ1(PRP1_4-5) [1.0]
NP-SBJ2_0-2 -> NP-SBJ2(NNP1_0-1, NNP1_1-2) [1.0]
NP1_3-4 -> NP1(PRP1_3-4) [1.0]
NP2_7-9 -> NP2(NNP1_7-8, NNP1_8-9) [1.0]
PRP1_3-4 -> PRP1(qh12) [1.0]
PRP1_4-5 -> PRP1(qh21) [1.0]
S2_4-9 -> S2(NP-SBJ1_4-5, VP2_5-9) [1.0]
S3_0-10! -> S3(NP-SBJ2_0-2, VP3_2-9, SEP-PER1_9-10) [1.0]
SBAR1_4-9 -> SBAR1(S2_4-9) [1.0]
SEP-PER1_9-10 -> SEP-PER1(qh18) [1.0]
VB1_6-7 -> VB1(qh13) [1.0]
VBD1_2-3 -> VBD1(qh8) [1.0]
VP2_5-9 -> VP2(MD1_5-6, VP2_6-9) [1.0]
VP2_6-9 -> VP2(VB1_6-7, NP2_7-9) [1.0]
VP3_2-9 -> VP3(VBD1_2-3, NP1_3-4, SBAR1_4-9) [1.0]
qh12 -> himself [1.0]
qh13 -> forget [1.0]
qh16 -> Ann [1.0]
qh18 -> PERIOD [1.0]
qh20 -> Morgan [1.0]
qh21 -> he [1.0]
qh4 -> Dan [1.0]
qh6 -> Turner [1.0]
qh7 -> would [1.0]
qh8 -> told [1.0]
    """;
}
