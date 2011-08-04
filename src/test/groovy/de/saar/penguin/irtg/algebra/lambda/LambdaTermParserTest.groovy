/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.algebra.lambda


import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.saar.basic.tree.*;


/**
 *
 * @author koller
 */
class LambdaTermParserTest {
    @Test
    public void testConstant() {
        assertEquals(LambdaTerm.constant("a"), p("a:e"));
    }
    
    /*
    @Test
    public void testVariable() {
        assertEquals(LambdaTerm.variable("$$0"), p("$$0"));
    }
    */
    
    private static LambdaTerm p(s) {
        return LambdaTermParser.parse(new StringReader(s));
    }
}

