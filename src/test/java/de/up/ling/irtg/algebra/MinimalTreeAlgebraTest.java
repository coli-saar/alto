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
            + "'1-2-0' -> __LEFT__INTO__RIGHT__('0-0-0-1', '0-0-0') [1.0]\n"
            + "'1-3-0' -> __LEFT__INTO__RIGHT__('0-0-0-1', '2-3-0') [1.0]\n"
            + "'1-2-0' -> __RIGHT__INTO__LEFT__('0-0-0', '0-0-0-1') [1.0]\n"
            + "'0-1-0' -> __RIGHT__INTO__LEFT__('0-0-0', '0-0-0-0') [1.0]\n"
            + "'2-3-0' -> __RIGHT__INTO__LEFT__('0-0-0', '0-1-0-2') [1.0]\n"
            + "'0-1-0-2' -> __RIGHT__INTO__LEFT__('0-0-0-2', '0-0-0-2-0') [1.0]\n"
            + "'0-2-0' -> __RIGHT__INTO__LEFT__('0-1-0', '0-0-0-1') [1.0]\n"
            + "'1-3-0' -> __RIGHT__INTO__LEFT__('1-2-0', '0-1-0-2') [1.0]\n"
            + "'0-3-0'! -> __RIGHT__INTO__LEFT__('0-2-0', '0-1-0-2') [1.0]\n"
            + "'0-1-0' -> __LEFT__INTO__RIGHT__('0-0-0-0', '0-0-0') [1.0]\n"
            + "'0-3-0'! -> __LEFT__INTO__RIGHT__('0-0-0-0', '1-3-0') [1.0]\n"
            + "'0-2-0' -> __LEFT__INTO__RIGHT__('0-0-0-0', '1-2-0') [1.0]\n"
            + "'0-1-0-2' -> __LEFT__INTO__RIGHT__('0-0-0-2-0', '0-0-0-2') [1.0]\n"
            + "'2-3-0' -> __LEFT__INTO__RIGHT__('0-1-0-2', '0-0-0') [1.0]";

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
    
}