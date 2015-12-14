/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import static de.up.ling.irtg.util.TestingTools.pa;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class MinimalTreeAlgebraTest {
    
    /**
     * 
     */
    private MinimalTreeAlgebra mta;
    
    @Before
    public void setUp() {
        mta = new MinimalTreeAlgebra();
    }

    /**
     * Test of parseString method, of class MinimalTreeAlgebra.
     */
    @Test
    public void testParseString() throws Exception {
        String t = "zz(j(m,l(b,v)),k)";
        assertEquals(mta.parseString(t),TreeParser.parse(t));
    }

    /**
     * 
     */
    private final static String SOLUTION = "'0-0-0-2' -> c [1.0]\n"
            + "'0-0-0-0' -> a [1.0]\n"
            + "'0-0-0' -> zz [1.0]\n"
            + "'0-0-0-2-0' -> f [1.0]\n"
            + "'0-0-0-1' -> b [1.0]\n"
            + "'1-2-0' -> __LR__('0-0-0-1', '0-0-0') [1.0]\n"
            + "'1-3-0' -> __LR__('0-0-0-1', '2-3-0') [1.0]\n"
            + "'1-2-0' -> __RL__('0-0-0', '0-0-0-1') [1.0]\n"
            + "'0-1-0' -> __RL__('0-0-0', '0-0-0-0') [1.0]\n"
            + "'2-3-0' -> __RL__('0-0-0', '0-1-0-2') [1.0]\n"
            + "'0-1-0-2' -> __RL__('0-0-0-2', '0-0-0-2-0') [1.0]\n"
            + "'0-2-0' -> __RL__('0-1-0', '0-0-0-1') [1.0]\n"
            + "'1-3-0' -> __RL__('1-2-0', '0-1-0-2') [1.0]\n"
            + "'0-3-0'! -> __RL__('0-2-0', '0-1-0-2') [1.0]\n"
            + "'0-1-0' -> __LR__('0-0-0-0', '0-0-0') [1.0]\n"
            + "'0-3-0'! -> __LR__('0-0-0-0', '1-3-0') [1.0]\n"
            + "'0-2-0' -> __LR__('0-0-0-0', '1-2-0') [1.0]\n"
            + "'0-1-0-2' -> __LR__('0-0-0-2-0', '0-0-0-2') [1.0]\n"
            + "'2-3-0' -> __LR__('0-1-0-2', '0-0-0') [1.0]";

    /**
     * Test of decompose method, of class MinimalTreeAlgebra.
     */
    @Test
    public void testDecompose() throws ParserException, IOException {
        Tree<String> input = mta.parseString("zz(a,b,c(f))");

        TreeAutomaton<String> t = mta.decompose(input);

        for (Tree<String> form : t.language()) {
            assertEquals(mta.evaluate(form), input);
        }

        assertEquals(t, pa(SOLUTION));
    }
    
    
    @Test
    public void testNumbers() throws ParserException {
        String input = "zz(j(m,l(b,v)),k)";
        Tree<String> t = this.mta.parseString(input);
        
        assertEquals(t.toString(),"zz(j(m,l(b,v)),k)");
        
        input = "00zz( 77 8 8 88( 7 ( 7 8 ( 12j(5  , 6l (4, 3v)),  12k  3) )),'hhhh')";
        t = this.mta.parseString(input);
        
        assertEquals(t.toString(),"'00zz'('77 8 8 88'('7'('7 8'('12j'('5','6l'('4','3v')),'12k  3'))),__QUOTE__hhhh__QUOTE__)");
            
        input = "answer(highest(place(loc_2(state(loc_1(place(elevation_2(0))))))))";
        t = this.mta.parseString(input);
        
        assertEquals(t.toString(),"answer(highest(place(loc_2(state(loc_1(place(elevation_2('0'))))))))");
        
        input = "answer(size(city(cityid('new york', _))))";
        t = this.mta.parseString(input);
        
        assertEquals(t.toString(),"answer(size(city(cityid('__QUOTE__new york__QUOTE__',_))))");
        
        TreeAutomaton ta = this.mta.decompose(t);
        
        Tree<String> gen = ta.getRandomTree();
        t = this.mta.evaluate(gen);
        
        assertEquals(t.toString(),"answer(size(city(cityid(\"'new york'\",_))))");
    }
}