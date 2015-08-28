/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.automata.FromRuleTreesAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.Logging;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.apache.commons.math3.util.Pair;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFinderTest {
    
    /**
     * 
     */
    private StringAlgebra alg1;
    
    /**
     * 
     */
    private StringAlgebra alg2;
    
    /**
     * 
     */
    private RuleFinder rf;
    
    /**
     * 
     */
    private HomomorphismManager homa;
    
    @Before
    public void setUp() {
        alg1 = new StringAlgebra();
        alg2 = new StringAlgebra();
        
        rf = new RuleFinder();
        homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
    }

    /**
     * Test of getRules method, of class RuleFinder.
     */
    @Test
    public void testGetRules() throws Exception {
        TreeAutomaton t1 = alg1.decompose(alg1.parseString("This is"));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString("is this"));
        
        assertTrue(t1.getSignature() == alg1.getSignature());
        assertTrue(t2.getSignature() == alg2.getSignature());
        
        SpanAligner spa1 = new SpanAligner("0:1:1 1:2:2", t1);
        SpanAligner spa2 = new SpanAligner("0:1:2 1:2:1", t2);
        
        Propagator prop = new Propagator();
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        assertTrue(t1.getSignature() == alg1.getSignature());
        assertTrue(t2.getSignature() == alg2.getSignature());
        
        TreeAutomaton t = rf.getRules(t1, t2, homa);
        
        assertTrue(homa.getHomomorphism1().isHeight1());
        assertTrue(homa.getHomomorphism2().isHeight1());
        
        Iterator<Tree<String>> lang = t.languageIterator();
        ObjectSet<Pair<Tree<String>,Tree<String>>> seen = new ObjectOpenHashSet<>();
        
        while(lang.hasNext()){
            Tree<String> q = lang.next();
            seen.add(new Pair(homa.getHomomorphism1().apply(q),homa.getHomomorphism2().apply(q)));
        }
        
        ObjectSet<Pair<Tree<String>,Tree<String>>> wanted = new ObjectOpenHashSet<>();
        wanted.add(new Pair(pt("*(This,is)"),pt("*(is,this)")));
        wanted.add(new Pair(pt("*('XX_{1}'(This),is)"),pt("*(is,'XX_{1}'(this))")));
        wanted.add(new Pair(pt("*('XX_{1}'(This),'XX_{2}'(is))"),pt("*('XX_{2}'(is),'XX_{1}'(this))")));
        wanted.add(new Pair(pt("*(This,'XX_{2}'(is))"),pt("*('XX_{2}'(is),this)")));
        
        for(Pair<Tree<String>,Tree<String>> p : wanted){
            assertTrue(seen.contains(p));
            seen.remove(p);
        }
        
        assertTrue(seen.isEmpty());
        /*
        t1 = alg1.decompose(alg1.parseString("This is a somewhat longer example of a translation that can be used ."));
        t2 = alg2.decompose(alg2.parseString("Dies ist ein Beispiel für eine längere Übersetzung ."));
        
        spa1 = new SpanAligner("0:1:1 1:2:2 5:6:3 4:5:4 8:9:5 13:14:6 2:3:7 7:8:8", t1);
        spa2 = new SpanAligner("0:1:1 1:2:2 3:4:3 6:7:4 7:8:5 8:9:6 2:3:7 5:6:8", t2);
        
        FromRuleTreesAutomaton sample = new FromRuleTreesAutomaton(t1);
        int samps = 3;
        
        for(int i=0;i<samps;++i){
            Tree<Rule> samp = t1.getRandomRuleTreeFromInside();
            sample.addRules(samp);
        }
        
        t1 = sample;
        
        sample = new FromRuleTreesAutomaton(t2);
        for(int i=0;i<samps;++i){
            Tree<Rule> samp = t2.getRandomRuleTreeFromInside();
            sample.addRules(samp);
        }
        t2 = sample;
        
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        assertTrue(t1.getSignature() == alg1.getSignature());
        assertTrue(t2.getSignature() == alg2.getSignature());
        t = rf.getRules(t1, t2, homa);
        
        for(Object o : t.getAllRulesTopDown()){
            Rule r = (Rule) o;
            
            int label = r.getLabel();
            String s = this.homa.getHomomorphism1().get(t.getSignature().resolveSymbolId(label)).getLabel();
            
            if(HomomorphismManager.VARIABLE_PATTERN.test(s)){
                r.setWeight(2.0);
            }else{
                r.setWeight(0.5);
            }
        }
        
        for(int i=0;i<1;++i){
            Tree<String> tr = t.viterbi();
            
            System.out.println("+++++++");
            System.out.println(homa.getHomomorphism1().apply(tr));
            System.out.println(homa.getHomomorphism2().apply(tr));
            System.out.println("+++++++");
        }*/
        
        InterpretedTreeAutomaton ita = rf.getInterpretation(t, homa, alg1, alg2);
        
        System.out.println(ita);
        //TODO
    }

    /**
     * Test of getAutomatonForObservations method, of class RuleFinder.
     */
    @Test
    public void testGetAutomatonForObservations() {
        //TODO
    }

    /**
     * Test of generalize method, of class RuleFinder.
     */
    @Test
    public void testGeneralize() {
        //TODO
    }

    /**
     * Test of generalizeBulk method, of class RuleFinder.
     */
    @Test
    public void testGeneralizeBulk() {
        //TODO
    }

    /**
     * Test of normalize method, of class RuleFinder.
     */
    @Test
    public void testNormalize() {
        //TODO
    }

    /**
     * Test of normalizeBulk method, of class RuleFinder.
     */
    @Test
    public void testNormalizeBulk() {
        //TODO
    }

    /**
     * Test of getInterpretation method, of class RuleFinder.
     */
    @Test
    public void testGetInterpretation_4args() {
        //TODO
    }

    /**
     * Test of getInterpretation method, of class RuleFinder.
     */
    @Test
    public void testGetInterpretation_5args() {
        //TODO
    }
}