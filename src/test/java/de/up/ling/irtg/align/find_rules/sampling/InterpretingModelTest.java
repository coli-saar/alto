/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.rule_finding.create_automaton.Propagator;
import de.up.ling.irtg.rule_finding.alignments.AlignmentFactory;
import de.up.ling.irtg.rule_finding.alignments.Empty;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
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
    private InterpretingModel normalModel;

    /**
     * 
     */
    private InterpretingModel strictLexical;
    
    /**
     * 
     */
    private InterpretingModel strictNonEmpty;
    
    /**
     * 
     */
    private Tree<Rule> target;
    
    /**
     * 
     */
    private Tree<Rule> avoid;
    
    
    @Before
    public void setUp() throws ParserException, Exception {
        /**
        MinimalTreeAlgebra mtaL = new MinimalTreeAlgebra();
        MinimalTreeAlgebra mtaR = new MinimalTreeAlgebra();
        
        CorpusCreator<Tree<String>,Tree<String>> cc =
                                                new CorpusCreator<>(mtaL,mtaR);
        
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
        
        this.normalModel = new InterpretingModel(cc.getMainManager(), 0.3, Math.log(1E-2),Math.log(1E-2));
        this.strictLexical = new InterpretingModel(cc.getMainManager(), 0.2, Math.log(1E-2), Math.log(1E-4));
        this.strictNonEmpty = new InterpretingModel(cc.getMainManager(), 0.3, Math.log(1E-3), Math.log(1E-2));
        
        Tree<String> t = ta.viterbi();
        
        Tree<Integer> ti = ta.viterbiRaw().getTree();
        this.target = ta.getRuleTree(ti);
        
        Tree<String> ts = target.map((Rule r) -> ta.getSignature().resolveSymbolId(r.getLabel()));
        assertEquals(ts,t);
        
        it = ta.getAllRulesTopDown();
        for(Rule r : it){
            r.setWeight((100*r.getParent())+(r.getLabel()));
        }
        
        ti = ta.viterbiRaw().getTree();
        this.avoid = ta.getRuleTree(ti);
        */
    }

    /**
     * Test of getLogWeight method, of class InterpretingModel.
     */
    @Test
    public void testGetLogWeight() {
        double d = this.normalModel.getLogWeight(target);
        assertEquals(d,-29.617524661949112,0.0000001);
        
        double v = this.strictLexical.getLogWeight(target);
        
        double also = d-Math.log(1E-2)+Math.log(1E-4);
        assertEquals(v,also,0.000001);
        
        v = this.strictNonEmpty.getLogWeight(target);
        also = d-Math.log(1E-2)+Math.log(1E-3);
        assertEquals(v,also,0.000001);
        
        double other = this.normalModel.getLogWeight(avoid);
        assertTrue(d > other);
        
        
        this.normalModel.add(target, 5);
        
        double q = this.normalModel.getLogWeight(target);
        double otherV = this.normalModel.getLogWeight(avoid);
        assertTrue(other > otherV);
        
        assertEquals(q,-10.655747594369284,0.00000001);
        assertTrue(q > d);
        
        double p = this.normalModel.getLogWeight(target);
        assertEquals(p,q,0.0000000000000001);
        
        this.normalModel.add(target, -5);
        double h = this.normalModel.getLogWeight(target);
        assertEquals(h,d,0.00000001);
        assertEquals(other,this.normalModel.getLogWeight(avoid),0.00000001);
        
        this.normalModel.add(target, 12);
        double k = this.normalModel.getLogWeight(target);
        assertTrue(k > q);

        this.normalModel.add(avoid, 3000);
        assertTrue(this.normalModel.getLogWeight(avoid) > other);
    }
}