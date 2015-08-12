/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.alignment_marking;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SpanAlignerTest {
    /**
     * 
     */
    private String sentence = "a b c d e f g h i j k l m n o p";
    
    /**
     * 
     */
    private StringAlgebra sal;
    
    /**
     * 
     */
    private SpanAligner sp;
    
    /**
     * 
     */
    private TreeAutomaton decomp;
    
    @Before
    public void setUp() {
       sal = new StringAlgebra();
       List<String> l = sal.parseString(sentence);
        
       
       decomp = sal.decompose(l);
       
       sp = new SpanAligner("   0:2:17 0:2:5 1:2:7 // 9:10:12", decomp);
    }

    /**
     * Test of getAlignmentMarkers method, of class SpanAligner.
     */
    @Test
    public void testGetAlignmentMarkers() {
        decomp.foreachStateInBottomUpOrder((int state, Iterable<Rule> rulesTopDown) -> {
            Span s = (Span) decomp.getStateForId(state);
            IntSet ins = sp.getAlignmentMarkers(s);
            if(s.start == 0 && s.end == 2){
                assertEquals(ins.size(),2);
                assertTrue(ins.contains(17));
                assertTrue(ins.contains(5));
            }else if(s.start == 1 && s.end == 2){
                assertEquals(ins.size(),1);
                assertTrue(ins.contains(7));
            }else if(s.start == 9 && s.end == 10){
                assertEquals(ins.size(),1);
                assertTrue(ins.contains(12));
            }else{
                assertTrue(ins.isEmpty());
            }
            
            for(Rule r : rulesTopDown){
                assertEquals(sp.getAlignmentMarkers(s),sp.evaluateRule(r));
            }
        });
    }

    /**
     * Test of makeMap method, of class SpanAligner.
     */
    @Test
    public void testMakeMap() {
        Object2ObjectMap<Span,IntSet> map = SpanAligner.makeMap("%%8:9:19%%0:7()()()4:7:1 0:1:9");
        assertEquals(map.size(),3);
        
        ObjectIterator<Object2ObjectMap.Entry<Span, IntSet>> it = map.object2ObjectEntrySet().iterator();
        while(it.hasNext()){
            Object2ObjectMap.Entry<Span, IntSet> e = it.next();
            Span s = e.getKey();
            IntSet in = e.getValue();
            
            if(s.start == 4 && s.end == 7){
                assertEquals(in.size(),1);
                assertTrue(in.contains(1));
            }else if(s.start == 8 && s.end == 9){
                assertEquals(in.size(),1);
                assertTrue(in.contains(19));
            }else if(s.start == 0 && s.end == 1){
                assertEquals(in.size(),1);
                assertTrue(in.contains(9));
            }else{
                assertTrue(in.isEmpty());
            }
        }
    }
    
}