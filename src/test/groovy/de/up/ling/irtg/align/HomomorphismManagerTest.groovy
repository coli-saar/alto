/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.util.Pair;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class HomomorphismManagerTest {
    
    /**
     * 
     */
    private Signature sig1;
    
    /**
     * 
     */
    private Signature sig2;
    
    /**
     * 
     */
    private HomomorphismManager hm;
    
    /**
     * 
     */
    private IntSet todo1;
    
    /**
     * 
     */
    private IntSet todo2;
    
    /**
     * 
     */
    private IntSet todo3;
    
    /**
     * 
     */
    private IntSet todo4;
    
    /**
     * 
     */
    private IntAVLTreeSet todo5;
    
    /**
     * 
     */
    private IntAVLTreeSet todo6;
    
    
    @Before
    public void setUp() {
        sig1 = new Signature();
        sig2 = new Signature();
        
        hm = new HomomorphismManager(sig1, sig2);
        
        int n1 = sig1.addSymbol("a", 2);
        int n2 = sig1.addSymbol("b", 0);
        int n3 = sig1.addSymbol("c", 0);
        int n4 = sig1.addSymbol("d", 1);
        int n5 = sig1.addSymbol("XX_87", 1);
        int n6 = sig1.addSymbol("XX1", 1);
        
        todo1 = new IntAVLTreeSet();
        todo1.add(n1);
        todo1.add(n2);
        todo1.add(n3);
        todo1.add(n4);
        todo1.add(n5);
        todo1.add(n6);
        
        todo2 = new IntAVLTreeSet();
        todo2.add(sig2.addSymbol("e", 2));
        todo2.add(sig2.addSymbol("g", 0));
        todo2.add(sig2.addSymbol("h", 1));
        todo2.add(sig2.addSymbol("XX1", 1));
        
        todo3 = new IntAVLTreeSet(todo1);
        todo3.add(sig1.addSymbol("i", 3));
        todo3.add(sig1.addSymbol("ii", 2));
        
        todo4 = new IntAVLTreeSet(todo2);
        todo4.add(sig2.addSymbol("j", 3));
        todo4.add(sig2.addSymbol("jj", 2));
        
        todo5 = new IntAVLTreeSet();
        todo5.add(sig1.addSymbol("mc3", 1));
        todo5.add(sig1.addSymbol("vv", 0));
        
        todo6 = new IntAVLTreeSet();
        todo6.add(sig2.addSymbol("hk", 1));
        todo6.add(sig2.addSymbol("hk54", 0));
    }
    
    
    @Test
    public void testVariable(){
        assertEquals(HomomorphismManager.VARIABLE_PREFIX,"XX");
        assertTrue(HomomorphismManager.VARIABLE_PATTERN.test(HomomorphismManager.VARIABLE_PREFIX));
        assertTrue(HomomorphismManager.VARIABLE_PATTERN.test(HomomorphismManager.VARIABLE_PREFIX+"asfdjöhg"));
        assertFalse(HomomorphismManager.VARIABLE_PATTERN.test("k"));
        assertFalse(HomomorphismManager.VARIABLE_PATTERN.test("k"+HomomorphismManager.VARIABLE_PREFIX));
    }

    /**
     * Test of update method, of class HomomorphismManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testUpdate() throws Exception {
        hm.update(todo1, todo2);
        
        Homomorphism hom1 = hm.getHomomorphism1();
        Homomorphism hom2 = hm.getHomomorphism2();
        
        Signature source = hom1.getSourceSignature();
        assertEquals(hom1.getSourceSignature(),hom2.getSourceSignature());
        
        Set<Pair<String,Pair<Tree<String>,Tree<String>>>> expected = new HashSet<>();
        
        expected.add(new Pair("a(x1, x2) / x1 | 2",new Pair(pt("a('?1','?2')"),pt("'?1'"))));
        expected.add(new Pair("a(x1, x2) / x2 | 2",new Pair(pt("a('?1','?2')"),pt("'?2'"))));
        expected.add(new Pair("a(x1, x2) / x3 | 3",new Pair(pt("a('?1','?2')"),pt("'?3'"))));
        expected.add(new Pair("a(x1, x2) / e(x1, x2) | 2",new Pair(pt("a('?1','?2')"),pt("e('?1','?2')"))));
        expected.add(new Pair("a(x1, x2) / e(x2, x1) | 2",new Pair(pt("a('?1','?2')"),pt("e('?2','?1')"))));
        
        expected.add(new Pair("b() / x1 | 1",new Pair(pt("b"),pt("'?1'"))));
        expected.add(new Pair("b() / g() | 0",new Pair(pt("b"),pt("g"))));
        expected.add(new Pair("c() / x1 | 1",new Pair(pt("c"),pt("'?1'"))));
        expected.add(new Pair("d(x1) / x1 | 1",new Pair(pt("d('?1')"),pt("'?1'"))));
        expected.add(new Pair("d(x1) / x2 | 2",new Pair(pt("d('?1')"),pt("'?2'"))));
        expected.add(new Pair("XX1(x1) / XX1(x1) | 1",new Pair(pt("XX1('?1')"),pt("XX1('?1')"))));
        expected.add(new Pair("XX(x1) / XX(x1) | 1",new Pair(pt("XX('?1')"),pt("XX('?1')"))));
        
        expected.add(new Pair("x1 / e(x1, x2) | 2",new Pair(pt("'?1'"),pt("e('?1','?2')"))));
        expected.add(new Pair("x2 / e(x1, x2) | 2",new Pair(pt("'?2'"),pt("e('?1','?2')"))));
        expected.add(new Pair("x3 / e(x1, x2) | 3",new Pair(pt("'?3'"),pt("e('?1','?2')"))));
        
        expected.add(new Pair("x1 / g() | 1",new Pair(pt("'?1'"),pt("g"))));
        expected.add(new Pair("x1 / h(x1) | 1",new Pair(pt("'?1'"),pt("h('?1')"))));
        expected.add(new Pair("x2 / h(x1) | 2",new Pair(pt("'?2'"),pt("h('?1')"))));
        
        assertFalse(hom1.isNonDeleting());
        assertFalse(hom2.isNonDeleting());        
        
        for(Pair<String,Pair<Tree<String>,Tree<String>>> p : expected){
            assertEquals(hom1.get(p.getKey()),p.getValue().getKey());
            assertEquals(hom2.get(p.getKey()),p.getValue().getValue());
        }
        
        assertEquals(source.getMaxSymbolId(),source.getMaxSymbolId());
        
        hm.update(todo1, todo2);
        
        assertEquals(source.getMaxSymbolId(),source.getMaxSymbolId());
        
        hm.update(todo5, todo6);
        
        expected.add(new Pair("x1 / hk54() | 1",new Pair(pt("'?1'"),pt("hk54"))));
        expected.add(new Pair("x1 / hk(x1) | 1",new Pair(pt("'?1'"),pt("hk('?1')"))));
        expected.add(new Pair("x2 / hk(x1) | 2",new Pair(pt("'?2'"),pt("hk('?1')"))));
        expected.add(new Pair("vv() / x1 | 1",new Pair(pt("vv"),pt("'?1'"))));
        expected.add(new Pair("mc3(x1) / x1 | 1",new Pair(pt("mc3('?1')"),pt("'?1'"))));
        expected.add(new Pair("mc3(x1) / x2 | 2",new Pair(pt("mc3('?1')"),pt("'?2'"))));
        
        for(Pair<String,Pair<Tree<String>,Tree<String>>> p : expected){
            assertEquals(hom1.get(p.getKey()),p.getValue().getKey());
            assertEquals(hom2.get(p.getKey()),p.getValue().getValue());
        }
        
        assertEquals(hom1,hm.getHomomorphism1());
        assertEquals(hom2,hm.getHomomorphism2());
        
        assertEquals(source.getMaxSymbolId(),24);
        assertEquals(source.getMaxSymbolId(),expected.size());
        
        assertFalse(hm.getRestrictionManager().getVariableSequenceing().accepts(pt(TEST_ONE)));
        assertFalse(hm.getRestrictionManager().getTermination().accepts(pt(TEST_ONE)));
        
        assertTrue(hm.getRestrictionManager().getRestriction() == hm.getRestriction());
        
        assertTrue(hm.getRestriction().accepts(pt(TEST_TWO)));
        assertFalse(hm.getRestriction().accepts(pt(TEST_ONE)));
        
        Tree<String> test = pt(TEST_THREE);
        assertTrue(hm.getRestriction().accepts(test));
        assertEquals(hom1.apply(test),pt("c"));
        assertEquals(hom2.apply(test),pt("hk(hk54)")); 
        
        test = pt(TEST_FOUR);
        assertTrue(hm.getRestriction().accepts(test));
        assertEquals(hom1.apply(test),pt("XX(a(mc3(XX1(d(c))),d(mc3(vv))))"));
        assertEquals(hom2.apply(test),pt("e(g,h(XX(XX1(e(hk54,e(hk54,e(hk54,g)))))))"));
        
        test = pt(TEST_FIVE);
        assertTrue(hm.getRestriction().accepts(test));
        assertEquals(hom1.apply(test),pt("a(b,a(XX(XX(a(b,mc3(d(a(mc3(d(a(d(c),vv))),XX1(b))))))),d(b)))"));
        assertEquals(hom2.apply(test),pt("hk(h(e(e(h(e(hk54,hk(hk54))),e(hk54,h(h(e(e(e(g,h(hk(h(hk54)))),e(hk(e(h(h(g)),h(e(hk(g),hk(hk54))))),hk54)),h(g)))))),XX(h(hk(h(hk(hk(XX(XX1(g)))))))))))"));
        
        expected = new HashSet<>();
        expected.add(new Pair("b() / g() | 0",new Pair(pt("b"),pt("g"))));
        expected.add(new Pair("x1 / hk54() | 1",new Pair(pt("'?1'"),pt("hk54"))));
        expected.add(new Pair("x1 / hk(x1) | 1",new Pair(pt("'?1'"),pt("hk('?1')"))));
        expected.add(new Pair("x2 / hk(x1) | 2",new Pair(pt("'?2'"),pt("hk('?1')"))));
        expected.add(new Pair("vv() / x1 | 1",new Pair(pt("vv"),pt("'?1'"))));
        expected.add(new Pair("mc3(x1) / x1 | 1",new Pair(pt("mc3('?1')"),pt("'?1'"))));
        expected.add(new Pair("mc3(x1) / x2 | 2",new Pair(pt("mc3('?1')"),pt("'?2'"))));
        
        
        hom1 = hm.getHomomorphismRestriction1(todo5,todo6);
        hom2 = hm.getHomomorphismRestriction2(todo6,todo5);
        
        for(Pair<String,Pair<Tree<String>,Tree<String>>> p : expected){
            assertEquals(hom1.get(p.getKey()),p.getValue().getKey());
            assertEquals(hom2.get(p.getKey()),p.getValue().getValue());
        }
        
        IntSet all = new IntOpenHashSet();
        
        for(int i=1;i<=hom1.getMaxLabelSetID();++i){
            all.addAll(hom1.getLabelSetByLabelSetID(i));
        }
        
        assertEquals(all.size(),7);
    }
    
    /**
     * 
     */
    private final static String TEST_ONE = "'XX1(x1) / XX1(x1) | 1'('a(x1, x2) / x1 | 2'('x1 / hk54() | 1'('b() / g() | 0'),'x1 / g() | 1'('b() / g() | 0')))";
    
    /**
     * 
     */
    private final static String TEST_TWO = "'c() / x1 | 1'('x3 / e(x1, x2) | 3'('x1 / g() | 1'('b() / g() | 0'),'x3 / e(x1, x2) | 3'('x1 / g() | 1'('b() / g() | 0'),'x1 / g() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'))";
    
    /**
     * 
     */
    private final static String TEST_THREE = "'c() / x1 | 1'('x2 / hk(x1) | 2'('x1 / hk54() | 1'('b() / g() | 0'),'b() / g() | 0'))";
    
    /**
     * 
     */
    private final static String TEST_FOUR = "'x2 / e(x1, x2) | 2'('x1 / g() | 1'('b() / g() | 0'),'x1 / h(x1) | 1'('XX(x1) / XX(x1) | 1'('a(x1, x2) / x1 | 2'('mc3(x1) / x1 | 1'('XX1(x1) / XX1(x1) | 1'('d(x1) / x2 | 2'('c() / x1 | 1'('b() / g() | 0'),'x3 / e(x1, x2) | 3'('x1 / hk54() | 1'('b() / g() | 0'),'x3 / e(x1, x2) | 3'('x1 / hk54() | 1'('b() / g() | 0'),'x3 / e(x1, x2) | 3'('x1 / hk54() | 1'('b() / g() | 0'),'x1 / g() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0')))),'d(x1) / x2 | 2'('mc3(x1) / x2 | 2'('vv() / x1 | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0')))))";
    
    /**
     * 
     */
    private final static String TEST_FIVE = "'a(x1, x2) / x2 | 2'('b() / x1 | 1'('b() / g() | 0'),'a(x1, x2) / x1 | 2'('x1 / hk(x1) | 1'('x1 / h(x1) | 1'('x2 / e(x1, x2) | 2'('x3 / e(x1, x2) | 3'('x2 / h(x1) | 2'('x3 / e(x1, x2) | 3'('x1 / hk54() | 1'('b() / g() | 0'),'x2 / hk(x1) | 2'('x1 / hk54() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'x3 / e(x1, x2) | 3'('x1 / hk54() | 1'('b() / g() | 0'),'x2 / h(x1) | 2'('x2 / h(x1) | 2'('x3 / e(x1, x2) | 3'('x3 / e(x1, x2) | 3'('x3 / e(x1, x2) | 3'('x1 / g() | 1'('b() / g() | 0'),'x2 / h(x1) | 2'('x2 / hk(x1) | 2'('x2 / h(x1) | 2'('x1 / hk54() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'x3 / e(x1, x2) | 3'('x2 / hk(x1) | 2'('x3 / e(x1, x2) | 3'('x2 / h(x1) | 2'('x2 / h(x1) | 2'('x1 / g() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'x2 / h(x1) | 2'('x3 / e(x1, x2) | 3'('x2 / hk(x1) | 2'('x1 / g() | 1'('b() / g() | 0'),'b() / g() | 0'),'x2 / hk(x1) | 2'('x1 / hk54() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'x1 / hk54() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'x2 / h(x1) | 2'('x1 / g() | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'XX(x1) / XX(x1) | 1'('x1 / h(x1) | 1'('x1 / hk(x1) | 1'('x1 / h(x1) | 1'('x1 / hk(x1) | 1'('x1 / hk(x1) | 1'('XX(x1) / XX(x1) | 1'('a(x1, x2) / x2 | 2'('b() / x1 | 1'('b() / g() | 0'),'mc3(x1) / x1 | 1'('d(x1) / x1 | 1'('a(x1, x2) / x2 | 2'('mc3(x1) / x2 | 2'('d(x1) / x2 | 2'('a(x1, x2) / x3 | 3'('d(x1) / x2 | 2'('c() / x1 | 1'('b() / g() | 0'),'b() / g() | 0'),'vv() / x1 | 1'('b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'b() / g() | 0'),'XX1(x1) / XX1(x1) | 1'('b() / x1 | 1'('x1 / g() | 1'('b() / g() | 0'))))))))))))))))),'d(x1) / x2 | 2'('b() / x1 | 1'('b() / g() | 0'),'b() / g() | 0')))";
}