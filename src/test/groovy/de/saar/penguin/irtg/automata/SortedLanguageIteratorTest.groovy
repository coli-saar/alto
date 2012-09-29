/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.saar.penguin.irtg.hom.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.saar.penguin.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
public class SortedLanguageIteratorTest {
    @Test
    public void testOneRuleNonRecursiveAutomaton() {
//        System.err.println("\n\n*** testOneRuleNonRecursiveAutomaton ***");
        TreeAutomaton a = pa("b -> qb [0.5]\n f(qb) -> qa! [0.7]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assert w.getTree().equals(pt("f(b)"))
        assert w.getWeight() == 0.35
        
        assert ! it.hasNext()
    }
    
    @Test
    public void testMultiRulesNonRecursiveAutomaton() {
//        System.err.println("\n\n*** testMultiRulesNonRecursiveAutomaton ***");
        
        TreeAutomaton a = pa("b -> qb [0.5]\n f(qb) -> qa! [0.7]\ng(qb) -> qa! [0.3]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assertEquals(pt("f(b)"), w.getTree())
        assert w.getWeight() == 0.35

        w = it.next();
        assertEquals(pt("g(b)"), w.getTree())
        assert w.getWeight() == 0.15
        
        assert ! it.hasNext()
    }
    
    @Test
    public void testRecursive() {
//        System.err.println("\n\n*** testRecursive ***");
        TreeAutomaton a = pa("b -> qb [0.5]\n f(qb) -> qb! [0.5]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assert w != null : "tree 1 null"
        assert w.getTree().equals(pt("b"))
        assert w.getWeight() == 0.5

        w = it.next();
        assert w != null : "tree 2 null"
        assert w.getTree().equals(pt("f(b)"))
        assert w.getWeight() == 0.25

        w = it.next();
        assert w != null : "tree 3 null"
        assert w.getTree().equals(pt("f(f(b))"))
        assert w.getWeight() == 0.125
        
        assert it.hasNext()
    }
    
    @Test
    public void testMultiFinalStates() {
        TreeAutomaton a = pa("b -> qb [0.5]\n f(qb) -> qb! [0.5]\n g(qb) -> qa! [0.4]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assert w != null : "tree 1 null"
        assert w.getTree().equals(pt("b"))
        assert w.getWeight() == 0.5

        w = it.next();
        assert w != null : "tree 2 null"
        assert w.getTree().equals(pt("f(b)"))
        assert w.getWeight() == 0.25
        
        w = it.next();
        assert w != null : "tree 3 null"
        assert w.getTree().equals(pt("g(b)"))
        assert w.getWeight() == 0.2

        w = it.next();
        assert w != null : "tree 4 null"
        assert w.getTree().equals(pt("f(f(b))"))
        assert w.getWeight() == 0.125
        
        w = it.next();
        assert w != null : "tree 5 null"
        assert w.getTree().equals(pt("g(f(b))"))
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
//        System.err.println("\n\n*** testLanguage ***");
        TreeAutomaton auto = pa("a -> q1 [2]\n b -> q2 [1]\n f(q1,q1) -> q! [1]\n f(q1,q2) -> q! [1.5]");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet([pt("f(a,a)"), pt("f(a,b)")]*.toString());
        assertEquals(gold, lang);
    }
    
    @Test
    public void testLanguage2() {
//        System.err.println("\n\n*** testLanguage2 ***");
        TreeAutomaton auto = pa("a -> q1\n b -> q1\n c -> q2\n d -> q2\n f(q1,q2) -> q!\n g(q1,q2) -> q!");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        assertEquals(gold, lang)
    }
    
    @Test
    public void testEmptyLanguageIterator() {
//        System.err.println("\n\n*** testEmptyLanguage ***");
        TreeAutomaton auto = pa("g(q1,q2) -> q!");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet();        
        assertEquals(gold, lang)
    }

}
