/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class TreeForcedBinaryWithAritiesAlgebraTest {
    /**
     * 
     */
    private TreeForcedBinaryWithAritiesAlgebra tfb;
    
    @Before
    public void setUp() {
        tfb = new TreeForcedBinaryWithAritiesAlgebra();
    }

    /**
     * Test of parseString method, of class TreeForcedBinaryWithAritiesAlgebra.
     */
    @Test
    public void testParseString() throws Exception {
        Tree<String> t;
        assertEquals(t = tfb.parseString("a(a(a(a),a),a,a(a(a,a,a)))"),TreeParser.parse("a(a(a(a),a),a,a(a(a,a,a)))"));
        
        Signature sig = tfb.getSignature();
        
        assertTrue(sig.contains("a_0"));
        assertTrue(sig.contains("a_1"));
        assertTrue(sig.contains("a_2"));
        assertTrue(sig.contains("a_@_2"));
        
        assertEquals(sig.getArity(sig.getIdForSymbol("a_0")),0);
        assertEquals(sig.getArity(sig.getIdForSymbol("a_1")),1);
        assertEquals(sig.getArity(sig.getIdForSymbol("a_2")),2);
        assertEquals(sig.getArity(sig.getIdForSymbol("a_@_2")),2);
        
        assertEquals(tfb.decompose(t).language().size(),1);
        assertEquals(tfb.decompose(t).languageIterator().next().toString(),"a_2(a_2(a_1(a_0),a_0),'a_@_2'(a_0,a_1(a_2(a_0,'a_@_2'(a_0,a_0)))))");
        
        assertEquals(tfb.evaluate((Tree<String>) tfb.decompose(t).languageIterator().next()),TreeParser.parse("a(a(a(a),a),a,a(a(a,a,a)))"));
    }
}
