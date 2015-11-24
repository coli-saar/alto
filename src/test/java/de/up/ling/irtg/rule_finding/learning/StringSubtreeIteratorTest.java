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
    
    /**
     * 
     */
    private StringSubtreeIterator.VariableMapping map;
    
    @Before
    public void setUp() throws Exception {
        Tree<String> a = pt("a");
        Tree<String> b = pt("b");
        
        Tree<String> f = Tree.create("f", a,a,a);
        f = Tree.create("k", f, Tree.create(Variables.makeVariable("abcd"), f));
        
        Tree<String> g = Tree.create("o", b,b);
        g = Tree.create(Variables.makeVariable("hhhh"), g);
        f = Tree.create("i", f,g);
        f = Tree.create(Variables.makeVariable("12222"), f);
        f = Tree.create(Variables.makeVariable("9898"), f);
        f = Tree.create("z", f);
        this.basis = Tree.create(Variables.makeVariable("iiiii"), f);
        
        map = new StringSubtreeIterator.VariableMapping() {

            @Override
            public String getRoot(Tree<String> whole) {
                return Variables.makeVariable("START");
            }

            @Override
            public String get(Tree<String> child, Tree<String> whole) {
                return child.getLabel().substring(0, 3);
            }
        };
    }

    /**
     * Test of getSubtrees method, of class StringSubtreeIterator.
     */
    @Test
    public void testGetSubtrees() throws Exception {
        Iterator<Tree<String>> it = StringSubtreeIterator.getSubtrees(basis, map);
        Tree[] arr = new Tree[] {
            pt("XSTART(z(X98))"),
            pt("X98(X12)"),
            pt("X12(i(k(f(a,a,a),Xab),Xhh))"),
            pt("Xab(f(a,a,a))"),
            pt("Xhh(o(b,b))")
        };
        
        int pos = 0;
        while(it.hasNext()){
            Tree<String> t = it.next();
            assertEquals(t,arr[pos++]);
        }
    }
}
