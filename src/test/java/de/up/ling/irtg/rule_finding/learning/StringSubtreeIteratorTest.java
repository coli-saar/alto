/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.rule_finding.Variables;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class StringSubtreeIteratorTest {
    
    /**
     * 
     */
    private Tree<String> basis;
    
    @Before
    public void setUp() throws Exception {
        Tree<String> a = pt("a");
        Tree<String> b = pt("b");
        
        Tree<String> f = Tree.create("f", a,a,a);
        f = Tree.create("k", f, Tree.create(Variables.createVariable("abcd"), f));
        
        Tree<String> g = Tree.create("o", b,b);
        g = Tree.create(Variables.createVariable("hhhh"), g);
        f = Tree.create("i", f,g);
        f = Tree.create(Variables.createVariable("12222"), f);
        f = Tree.create(Variables.createVariable("9898"), f);
        f = Tree.create("z", f);
        this.basis = Tree.create(Variables.createVariable("iiiii"), f);
    }

    /**
     * Test of getSubtrees method, of class StringSubtreeIterator.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetSubtrees() throws Exception {
        Iterator<Tree<String>> it = StringSubtreeIterator.getSubtrees(basis);
        Tree[] arr = new Tree[] {
            pt("'__X__{iiiii}'(z('__X__{9898}'))"),
            pt("'__X__{9898}'('__X__{12222}')"),
            pt("'__X__{12222}'(i(k(f(a,a,a),'__X__{abcd}'),'__X__{hhhh}'))"),
            pt("'__X__{abcd}'(f(a,a,a))"),
            pt("'__X__{hhhh}'(o(b,b))")
        };
        
        int pos = 0;
        while(it.hasNext()){            
            Tree<String> t = it.next();
            
            assertEquals(t,arr[pos++]);
        }
    }
}
