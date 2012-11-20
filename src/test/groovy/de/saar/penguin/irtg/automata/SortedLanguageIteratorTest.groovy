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
        TreeAutomaton a = pa("qb -> b [0.5]\n qa! -> f(qb) [0.7]");
        Iterator<WeightedTree> it = a.sortedLanguageIterator();
        
        WeightedTree w = it.next();
        assert w.getTree().equals(pt("f(b)"))
        assert w.getWeight() == 0.35
        
        assert ! it.hasNext()
    }
    
    @Test
    public void testMultiRulesNonRecursiveAutomaton() {
//        System.err.println("\n\n*** testMultiRulesNonRecursiveAutomaton ***");
        
        TreeAutomaton a = pa("qb -> b [0.5]\n qa! -> f(qb) [0.7]\n qa! -> g(qb) [0.3]");
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
        TreeAutomaton a = pa("qb -> b [0.5]\n qb! -> f(qb) [0.5]");
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
        TreeAutomaton a = pa("qb -> b [0.5]\n qb! -> f(qb) [0.5]\n qa! -> g(qb) [0.4]");
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
        TreeAutomaton auto = pa("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2)  [1.5]");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet([pt("f(a,a)"), pt("f(a,b)")]*.toString());
        assertEquals(gold, lang);
    }
    
    // TODO:         
    // TreeAutomaton auto = pa("q1 -> a \n q1 -> b \n q2 -> c \n q2 -> d \n q! -> f(q1,q2) \n q1 -> g(q1,q2) ");
    // exceeds heap

    
    @Test
    public void testLanguage2() {
        TreeAutomaton auto = pa("q1 -> a \n q1 -> b \n q2 -> c \n q2 -> d \n q! -> f(q1,q2) \n q! -> g(q1,q2) ");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        assertEquals(gold, lang)
    }
    
    @Test
    public void testEmptyLanguageIterator() {
        TreeAutomaton auto = pa("q! -> g(q1,q2) ");
        Set lang = new HashSet(collectTrees(auto)*.toString());
        Set gold = new HashSet();        
        assertEquals(gold, lang)
    }

}
