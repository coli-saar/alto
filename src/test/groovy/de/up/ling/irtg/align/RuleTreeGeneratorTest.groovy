/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.align.RuleMarker;
import de.up.ling.irtg.align.RuleTreeGenerator;
import de.up.ling.irtg.align.alignment_algebras.StringAlignmentAlgebra
import de.up.ling.irtg.align.rule_markers.SimpleRuleMarker
import de.up.ling.irtg.automata.ConcreteTreeAutomaton
import de.up.ling.irtg.automata.InverseHomAutomaton
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree
import java.util.HashSet
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class RuleTreeGeneratorTest {
    
    /**
     * 
     */
    private RuleTreeGenerator rgen;
    
    
    @Before
    public void setUp() {
        rgen = new RuleTreeGenerator();
    }

    /**
     * Test of makeInverseIntersection method, of class RuleTreeGenerator.
     */
    @Test
    public void testMakeInverseIntersection() {
        MarkingPropagator mp = new MarkingPropagator();
        
        StringAlignmentAlgebra saa = new StringAlignmentAlgebra();
        
        String one = "a:1:4 b";
        String two = "c d:1 e:4";
        
        Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> parts = saa.decomposePair(one, two);
        
        TreeAutomaton ta1 = mp.introduce(parts.getRight().getLeft(),parts.getLeft(),0);
        TreeAutomaton ta2 = mp.introduce(parts.getRight().getRight(),parts.getLeft(),1);
        
        Pair<TreeAutomaton,Pair<Homomorphism,Homomorphism>> result = rgen.makeInverseIntersection(ta1,ta2,parts.getLeft());
        Homomorphism hom1 = result.getRight().getLeft();
        Homomorphism hom2 = result.getRight().getRight();
           
        TreeAutomaton ta = result.getLeft();
        ta.determinize();
        
        for(Rule r : ta.getRuleIterable()){
            String label = r.getLabel(ta);
            
            if(label.matches("X.+")){
               r.setWeight(1.0);
               continue;
            }
            
            r.setWeight(0.5);
        }
        
        Set<Pair<String,String>> seen = new HashSet<>();
        
        Iterator<Tree<String>> it = ta.languageIterator();
        for(int i=0;i<150;++i){
            Tree<String> t = it.next();
            
            seen.add(new Pair<>(hom1.apply(t).toString(),hom2.apply(t).toString()));
        }
        
        assertTrue(seen.contains(new Pair<>("*('X_{0, 1}'(a),'X_{}'(b))","*('X_{}'(c),'X_{0, 1}'(*(d,e)))")));
        assertFalse(seen.contains(new Pair<>("'X_{0, 1}'(*('X_{0, 1}'(a),'X_{}'(b)))","'X_{0, 1}'(*('X_{}'(c),'X_{0, 1}'(*(d,e))))")));
        assertTrue(seen.contains(new Pair<>("*('X_{0, 1}'(a),b)","*(c,'X_{0, 1}'(*(d,e)))")));
        assertTrue(seen.contains(new Pair<>("*(a,b)","*(c,*(d,e))")));
        assertTrue(seen.contains(new Pair<>("*(a,b)","*(*(c,d),e)")));
        assertFalse(seen.contains(new Pair<>("*(a,'X_{0, 1}'(b))","*(*(c,d),'X_{0, 1}'(e))")));
    }
    
    @Test
    public void testCorners() {
        SimpleRuleMarker srm = new SimpleRuleMarker("K");
        
        ConcreteTreeAutomaton<String> cta1 = new ConcreteTreeAutomaton<String>();
        ConcreteTreeAutomaton<String> cta2 = new ConcreteTreeAutomaton<String>();
        
        
        cta1.addFinalState(cta1.addState("root"));
        cta1.addRule(cta1.createRule("root","A",["a","b"]));
        
        Rule r1 = cta1.createRule("a","a",[]);
        cta1.addRule(r1);
        
        Rule r2 = cta1.createRule("b","b",[]);
        cta1.addRule(r2);
        
        cta2.addFinalState(cta2.addState("root"));
        
        cta2.addRule(cta2.createRule("root","b",["root"]));
        
        cta2.addRule(cta2.createRule("root","X",["a","b"]));
        
        Rule r = cta2.createRule("a","V",["x","x"]);
        cta2.addRule(r);
        srm.addPair(r1,r)
        
        r = cta2.createRule("b","G",["x","x","x"]);
        srm.addPair(r2,r);
        cta2.addRule(r);
        
        cta2.addRule(cta2.createRule("x","M",[]));
        
        MarkingPropagator mp = new MarkingPropagator();
        
        TreeAutomaton ta1 = mp.introduce(cta1,srm,0);
        TreeAutomaton ta2 = mp.introduce(cta2,srm,1);
        
        Pair<TreeAutomaton,Pair<Homomorphism,Homomorphism>> result = rgen.makeInverseIntersection(ta1,ta2,srm);
        
        
        TreeAutomaton ta = result.getLeft();
        Homomorphism hm1 = result.getRight().getLeft();
        Homomorphism hm2 = result.getRight().getRight();
        
        for(Rule h : ta.getRuleIterable()){
            String label = h.getLabel(ta);
            
            if(label.matches("X.+")){
               h.setWeight(3.0);
               continue;
            }
            
            h.setWeight(0.5);
        }
        
        Iterator<Tree<String>> it = ta.languageIterator();
        
        Set<Pair<String,String>> seen = new HashSet<>();
        for(int i=0;i<100;++i){
            Tree<String> ts = it.next();
            
            seen.add(new Pair<>(hm1.apply(ts).toString(),hm2.apply(ts).toString()));
        }
        
        assertTrue(seen.contains(new Pair<>("'K_{0, 1}'(A('K_{0}'(a),'K_{1}'(b)))","b('K_{0, 1}'(X('K_{0}'(V(M,M)),'K_{1}'(G(M,M,M)))))")));
    }
}