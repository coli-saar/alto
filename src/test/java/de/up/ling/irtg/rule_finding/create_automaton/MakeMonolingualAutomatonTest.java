/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class MakeMonolingualAutomatonTest {
    /**
     * 
     */
    private MakeMonolingualAutomaton mma;
    
    /**
     * 
     */
    private TreeAutomaton<StringAlgebra.Span> aut;
    
    /**
     * 
     */
    private Function<Object,String> mapping;
    
    @Before
    public void setUp() {
        mma = new MakeMonolingualAutomaton();
        
       
        StringAlgebra sal = new StringAlgebra();
        
        List<String> words = sal.parseString("a b c");
        
        aut = sal.decompose(words);
        
        mapping = (Object s) -> {
            StringAlgebra.Span span = (StringAlgebra.Span) s;
            int l  = span.start;
            int r = span.end-1;
            
            return words.get(l)+" "+words.get(r);
        };
    }

    /**
     * Test of introduce method, of class MakeMonolingualAutomaton.
     * @throws de.up.ling.tree.ParseException
     */
    @Test
    public void testIntroduce() throws ParseException {
        TreeAutomaton t = mma.introduce(aut, mapping, "ROOT");
        
        Set<Tree<String>> lang = new HashSet<>(t.language());
        
        Tree<String> q = TreeParser.parse("'__X__{a c}'(*(a,*(b,c)))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(*('__X__{a a}'(a),b),c))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(a,*('__X__{b b}'(b),c)))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(a,*(b,'__X__{c c}'(c))))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(a,'__X__{b c}'(*(b,c))))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*('__X__{a a}'(a),'__X__{b c}'(*(b,c))))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*('__X__{a a}'(a),*('__X__{b b}'(b),c)))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*('__X__{a a}'(a),*(b,'__X__{c c}'(c))))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(a,*('__X__{b b}'(b),'__X__{c c}'(c))))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(*('__X__{a a}'(a),'__X__{b b}'(b)),'__X__{c c}'(c)))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*('__X__{a a}'(a),*('__X__{b b}'(b),'__X__{c c}'(c))))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        q = TreeParser.parse("'__X__{a c}'(*(*(a,b),c))");
        assertTrue(lang.contains(q));
        lang.remove(q);
        
        assertEquals(lang.size(),20);
    }
    
}
