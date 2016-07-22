/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class MakeIdIRTGTest {
    /**
     * 
     */
    private MakeMonolingualAutomaton mma;
    
    /**
     * 
     */
    private TreeAutomaton<StringAlgebra.Span> aut;
    
    /**
     * 
     */
    private Function<Object,String> mapping;
    
    /**
     * 
     */
    private TreeAutomaton t;
    
    /**
     * 
     */
    private StringAlgebra salg;
    
    @Before
    public void setUp() {
        mma = new MakeMonolingualAutomaton();
        
        StringAlgebra sal = new StringAlgebra();
        
        List<String> words = sal.parseString("a b c");
        
        aut = sal.decompose(words);
        
        mapping = (Object o) -> {
            StringAlgebra.Span span = (StringAlgebra.Span) o;
            int l  = span.start;
            int r = span.end-1;
            
            return words.get(l)+" "+words.get(r);
        };
        
        t = mma.introduce(aut, mapping, "ROOT");
        salg = new StringAlgebra();
    }

    /**
     * Test of makeIRTG method, of class MakeIdIRTG.
     */
    @Test
    public void testMakeIRTG() {
        InterpretedTreeAutomaton tia = MakeIdIRTGForSingleSide.makeIRTG(t, "string", salg);
        
        Iterable<Tree<String>> ts = tia.getAutomaton().languageIterable();
                
        for(Tree<String> q : ts) {
            assertEquals(q,tia.getInterpretation("string").getHomomorphism().apply(q));
        }
    }
}
