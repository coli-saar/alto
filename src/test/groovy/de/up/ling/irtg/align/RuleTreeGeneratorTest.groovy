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
        
        Set<String> s = new TreeSet<>(hom1.getSourceSignature().getSymbols());
        s.addAll(hom1.getSourceSignature().getSymbols());
        
        for(Rule r : ta.getRuleIterable()){
            String label = r.getLabel(ta);
            
            if(label.matches("X.+")){
               r.setWeight(1.0);
               continue;
            }
            
            r.setWeight(0.5);
        }
        
        SubtreePairer sp = new SubtreePairer(hom1,hom2);
        
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
}
