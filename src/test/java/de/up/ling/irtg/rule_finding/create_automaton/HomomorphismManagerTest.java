/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
    private HomomorphismManager hom;

    /**
     * 
     */
    private TreeAutomaton leftAut;

    /**
     * 
     */
    private TreeAutomaton rightAut;

    /**
     * 
     */
    private final static String LEFT_TEST_AUTOMATON
            = "q_0 -> b [1.0]\n"
            + "q_1 -> b [1.0]\n"
            + "q! -> a(q_0, q_1) [1.0]";

    /**
     * 
     */
    private final static String RIGHT_TEST_AUTOMATON
            = "q_0 -> u [1.0]\n"
            + "q! -> o(q_0, q_1) [1.0]\n"
            + "q_1 -> u [1.0]";

    /**
     * 
     */
    private static final String LEFT_ALIGNMENT =
            "q_1 ||| 1\n"
            + "q_0 ||| 2";
    
    /**
     * 
     */
    private static final String RIGHT_ALIGNMENT = 
              "q_0 ||| 1\n"
            + "q_1 ||| 2";
    
    /**
     * 
     */
    private SpecifiedAligner samL;
    
    /**
     * 
     */
    private SpecifiedAligner samR;
    
    /**
     * 
     */
    private Signature sharedSig;
    
    @Before
    public void setUp() throws ParserException, IOException {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        this.leftAut = taic.read(new ByteArrayInputStream(LEFT_TEST_AUTOMATON.getBytes()));

        this.rightAut = taic.read(RIGHT_TEST_AUTOMATON);
        
        assertFalse(leftAut.getSignature() == rightAut.getSignature());
        
        samL = new SpecifiedAligner(leftAut, new ByteArrayInputStream(LEFT_ALIGNMENT.getBytes()));
        samR = new SpecifiedAligner(rightAut, new ByteArrayInputStream(RIGHT_ALIGNMENT.getBytes()));
        
        Propagator prop = new Propagator();
        
        Int2ObjectMap<IntSortedSet> m1 = prop.propagate(leftAut, samL);
        Int2ObjectMap<IntSortedSet> m2 = prop.propagate(rightAut, samR);
        
        Set<String> counter1 = Propagator.turnToMarkers(m1.values());
        Set<String> counter2 = Propagator.turnToMarkers(m2.values());
        
        this.leftAut = prop.convert(leftAut, m1, counter2);
        this.rightAut = prop.convert(rightAut, m2, counter1);
        
        this.sharedSig = new Signature();
        this.hom = new HomomorphismManager(this.leftAut.getSignature(), this.rightAut.getSignature(), sharedSig);
    }

    /**
     * Test of update method, of class HomomorphismManager.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testUpdate() throws Exception {
        this.hom.update(this.leftAut.getAllLabels(), this.rightAut.getAllLabels());
        
        RuleFindingIntersectionAutomaton rfi = new RuleFindingIntersectionAutomaton(leftAut, rightAut, this.hom.getHomomorphism1(), this.hom.getHomomorphism2());
        TopDownIntersectionAutomaton tdi = new TopDownIntersectionAutomaton(rfi, hom.getRestriction());
        
        Set<Pair<String,String>> proposals = new HashSet<>();
        for(Tree<String> tree : tdi.languageIterable()) {
            proposals.add(new Pair<>(hom.getHomomorphism1().apply(tree).toString(),hom.getHomomorphism2().apply(tree).toString()));
        }
        
        assertEquals(proposals.size(),4);
        
        Pair<String,String> p =
                new Pair<>("a('__X__{2 _@_ q_0}'(b),'__X__{1 _@_ q_1}'(b))","o('__X__{1 _@_ q_0}'(u),'__X__{2 _@_ q_1}'(u))");
        assertTrue(proposals.contains(p));
        proposals.remove(p);
        
        p = new Pair<>("a(b,b)","o(u,u)");
        assertTrue(proposals.contains(p));
        proposals.remove(p);
        
        p = new Pair<>("a(b,'__X__{1 _@_ q_1}'(b))","o('__X__{1 _@_ q_0}'(u),u)");
        assertTrue(proposals.contains(p));
        proposals.remove(p);
        
        p = new Pair<>("a('__X__{2 _@_ q_0}'(b),b)","o(u,'__X__{2 _@_ q_1}'(u))");
        assertTrue(proposals.contains(p));
        proposals.remove(p);
        
        assertTrue(proposals.isEmpty());
        
        Set<Pair<String,Pair<String,String>>> mappings = new HashSet();
        for(int i=1;i<this.hom.getSignature().getMaxSymbolId();++i) {
            String label = this.hom.getSignature().resolveSymbolId(i);
            
            if(Variables.isVariable(label)) {
                assertTrue(this.hom.isVariable(i));
            } else {
                assertFalse(this.hom.isVariable(i));
            }
            
            Tree<String> ts1 = this.hom.getHomomorphism1().get(label);
            Tree<String> ts2 = this.hom.getHomomorphism2().get(label);
            
            Pair<String,String> right = new Pair<>(ts1.toString(),ts2.toString());
            Pair<String,Pair<String,String>> whole = new Pair<>(label,right);
            
            mappings.add(whole);
        }
        
        assertEquals(mappings.size(),13);
        
        Pair<String,String> pa = new Pair<>("b","'?1'");
        Pair<String,Pair<String,String>> q = new Pair<>("b() / x1 | 1",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("a('?1','?2')","'?2'");
        q = new Pair<>("a(x1, x2) / x2 | 2",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'?3'","o('?1','?2')");
        q = new Pair<>("x3 / o(x1, x2) | 3",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("a('?1','?2')","'?1'");
        q = new Pair<>("a(x1, x2) / x1 | 2",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'?1'","u");
        q = new Pair<>("x1 / u() | 1",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'__X__{1 _@_ q_1}'('?1')","'__X__{1 _@_ q_0}'('?1')");
        q = new Pair<>("__X__{q_1 ||| q_0}",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'?1'","o('?1','?2')");
        q = new Pair<>("x1 / o(x1, x2) | 2",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'__X__{2 _@_ q_0}'('?1')","'__X__{2 _@_ q_1}'('?1')");
        q = new Pair<>("__X__{q_0 ||| q_1}",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("___END___","___END___");
        q = new Pair<>("___END___() / ___END___() | 0",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("a('?1','?2')","o('?1','?2')");
        q = new Pair<>("a(x1, x2) / o(x1, x2) | 2",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'__X__{1,2 _@_ q}'('?1')","'__X__{1,2 _@_ q}'('?1')");
        q = new Pair<>("__X__{q ||| q}",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("'?2'","o('?1','?2')");
        q = new Pair<>("x2 / o(x1, x2) | 2",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        pa = new Pair<>("a('?1','?2')","'?3'");
        q = new Pair<>("a(x1, x2) / x3 | 3",pa);
        assertTrue(mappings.contains(q));
        mappings.remove(q);
        
        assertTrue(mappings.isEmpty());
        
        TreeAutomaton<String> reduced =
                this.hom.reduceToOriginalVariablePairs(tdi);
        proposals = new HashSet<>();
        for(Tree<String> tree : reduced.languageIterable()) {
            proposals.add(new Pair<>(hom.getHomomorphism1().apply(tree).toString(),hom.getHomomorphism2().apply(tree).toString()));
        }
        
        assertEquals(proposals.size(),4);
        
        Pair<String,String> target = new Pair<>("'__X__{__UAS__}'(a(b,b))","'__X__{__UAS__}'(o(u,u))");
        assertTrue(proposals.contains(target));
        proposals.remove(target);
        
        target = new Pair<>("'__X__{__UAS__}'(a(b,'__X__{1 _@_ q_1}'(b)))","'__X__{__UAS__}'(o('__X__{1 _@_ q_0}'(u),u))");
        assertTrue(proposals.contains(target));
        proposals.remove(target);
        
        target = new Pair<>("'__X__{__UAS__}'(a('__X__{2 _@_ q_0}'(b),b))","'__X__{__UAS__}'(o(u,'__X__{2 _@_ q_1}'(u)))");
        assertTrue(proposals.contains(target));
        proposals.remove(target);
        
        target = new Pair<>("'__X__{__UAS__}'(a('__X__{2 _@_ q_0}'(b),'__X__{1 _@_ q_1}'(b)))","'__X__{__UAS__}'(o('__X__{1 _@_ q_0}'(u),'__X__{2 _@_ q_1}'(u)))");
        assertTrue(proposals.contains(target));
        proposals.remove(target);
        
        assertTrue(proposals.isEmpty());
    }
}
