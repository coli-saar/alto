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
    
    @Test
    public void testVariable() {
        assertEquals(LambdaTerm.variable("\$0"), p("\$0"));
    }
    
    @Test
    public void testComplex() {
        assertEquals(LambdaTerm.lambda("\$0", LambdaTerm.argmax("\$1",
                        LambdaTerm.conj(a(c("state"), v("0")), a(c("next"), v("0"), v("1")), a(c("foo"), v("1"))),
                        a(c("elevation"), v("1"))
                )),
            p("(lambda \$0 (argmax \$1 (and (state:a \$0) (next:a \$0 \$1) (foo:a \$1)) (elevation:i \$1)))"));
    }
    
    @Test
    public void testGeo880_1() {
        assertEquals(a(c("population"), a(c("capital"), 
                        LambdaTerm.argmax("\$1", LambdaTerm.conj(a(c("state"),v("1")), a(c("loc"), c("mississippi_river"), v("1"))), a(c("size"), v("1")))
                    )),
        p("(population:i (capital:c (argmax \$1 (and (state:t \$1) (loc:t mississippi_river:r \$1)) (size:i \$1))))")
        )
    }
    
    private static LambdaTerm a(f, LambdaTerm... a) {
        return LambdaTerm.apply(f,Arrays.asList(a));
    }
    
    private static LambdaTerm c(x) {
        return LambdaTerm.constant(x);
    }
    
    private static LambdaTerm v(x) {
        return LambdaTerm.variable("\$" + x);
    }
    
    private static LambdaTerm p(s) {
        return LambdaTermParser.parse(new StringReader(s));
    }
}

