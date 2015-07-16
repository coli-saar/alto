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
        
        String one = "a:1 a:2";
        String two = "a a:1 b:2";
        
        Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> parts = saa.decomposePair(one, two);
        
        TreeAutomaton ta1 = mp.introduce(parts.getRight().getLeft(),parts.getLeft(),0);
        TreeAutomaton ta2 = mp.introduce(parts.getRight().getRight(),parts.getLeft(),1);
        
        Pair<TreeAutomaton,Pair<Homomorphism,Homomorphism>> result = rgen.makeInverseIntersection(ta1,ta2,parts.getLeft());
        Homomorphism hom1 = result.getRight().getLeft();
        Homomorphism hom2 = result.getRight().getRight();
        
        System.out.println("obtained result");
           
        TreeAutomaton ta = result.getLeft();
        
        Set<String> s = new TreeSet<>(hom1.getSourceSignature().getSymbols());
        s.addAll(hom1.getSourceSignature().getSymbols());
        
        for(Rule r : ta.getRuleIterable()){
            r.setWeight(0.5);
        }        
        
        SubtreePairer sp = new SubtreePairer(hom1,hom2);
        
        Iterator<Tree<String>> it = ta.languageIterator();
        for(int i=0;i<50;++i){
            Tree<String> t = it.next();
            System.out.println(t);
            System.out.println(hom1.apply(t));
            System.out.println(hom2.apply(t));
            
            System.out.println(t.map(sp));
        }
        // TODO review the generated test code and remove the default call to fail.
    }
    
}
