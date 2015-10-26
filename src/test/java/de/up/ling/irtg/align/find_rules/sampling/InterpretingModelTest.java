/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.AlignmentFactory;
import de.up.ling.irtg.align.alignment_marking.Empty;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.creation.CreateCorpus.InputPackage;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class InterpretingModelTest {
    
    /**
     * 
     */
    private InterpretingModel im;
    
    /**
     * 
     */
    private Tree<Rule> target;
    
    
    @Before
    public void setUp() throws ParserException, Exception {
        MinimalTreeAlgebra mtaL = new MinimalTreeAlgebra();
        MinimalTreeAlgebra mtaR = new MinimalTreeAlgebra();
        
        CreateCorpus<Tree<String>,Tree<String>> cc =
                                                new CreateCorpus<>(mtaL,mtaR);
        
        
        final Propagator prop = new Propagator();
        
        Function<Tree<String>,Propagator> f = (Tree<String> in) -> {return prop;};
        AlignmentFactory af = (String s, TreeAutomaton ta) -> {
            return new Empty(ta);
        };
        
        
        InputPackage ip1 = cc.makePackage("a(b,c)", "", f, af);
        InputPackage ip2 = cc.makePackage("a(b,b)", "", f, af);
        
        TreeAutomaton ta = cc.makeEntry(ip1, ip2);
        Iterable<Rule> it = ta.getAllRulesTopDown();
        for(Rule r : it){
            r.setWeight(1/(100*r.getParent())+1/(r.getLabel()));
        }
        
        this.im = new InterpretingModel(cc.getMainManager(), 0.3, Math.log(1E-2),Math.log(1E-2));
        
        Tree<String> t = ta.viterbi();
        System.out.println(t);
        System.out.println(cc.getMainManager().getHomomorphism1().apply(t));
        System.out.println(cc.getMainManager().getHomomorphism2().apply(t));
        
        Tree<Integer> ti = ta.viterbiRaw().getTree();
        target = ta.getRuleTree(ti);
        
        Tree<String> ts = target.map((Rule r) -> ta.getSignature().resolveSymbolId(r.getLabel()));
        assertEquals(ts,t);
        //TODO
    }

    /**
     * Test of getLogWeight method, of class InterpretingModel.
     */
    @Test
    public void testGetLogWeight() {
        double d = this.im.getLogWeight(target);
        assertEquals(d,-29.617524661949112,0.0000001);
        
        this.im.add(target, 5);
        
        double q = this.im.getLogWeight(target);
        
        assertEquals(q,-10.655747594369284,0.00000001);
        assertTrue(q > d);
        
        double p = this.im.getLogWeight(target);
        assertEquals(p,q,0.0000000000000001);
        
        this.im.add(target, -5);
        double h = this.im.getLogWeight(target);
        assertEquals(h,d,0.00000001);
        
        this.im.add(target, 12);
        double k = this.im.getLogWeight(target);
        assertTrue(k > q);
        
        System.out.println(d);
        System.out.println(q);

        //TODO
    }   
}