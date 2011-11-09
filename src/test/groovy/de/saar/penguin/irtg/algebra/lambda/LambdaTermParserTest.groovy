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
import java.util.ArrayList;


/**
 *
 * @author koller, melzer
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
    
    
    @Test
    public void testComplicatedTerm() {
        LambdaTerm parsed = p("""((lambda \$f (lambda \$x (\$n \$f (\$f \$x)))) (lambda \$a (lambda \$b \$b)))""");
        LambdaTerm b = LambdaTerm.apply(LambdaTerm.lambda(   "\$f",
                                                        LambdaTerm.lambda(  "\$x",
                                                                                a(  v("n"),
                                                                                    v("f"),
                                                                                    a(  v("f"),
                                                                                        v("x"))))),
                                    LambdaTerm.lambda("\$a",LambdaTerm.lambda("\$b",v("b"))));
                                
        assertEquals(b,parsed);
    }
    

    @Test
    public void testBeta(){
        LambdaTerm lx = p("((lambda \$x (lambda \$f (\$x \$f))) (lambda \$a (\$a \$a)))");
        LambdaTerm y = p("(lambda \$f (\$f \$f))")

       assertEquals(y,lx.reduce());

    }

    @Test
    public void testEvaluate(){
        Tree<LambdaTermAlgebraSymbol> t1 = new Tree<LambdaTermAlgebraSymbol>();
        Tree<LambdaTermAlgebraSymbol> t2 = new Tree<LambdaTermAlgebraSymbol>();

        LambdaTermAlgebraSymbol func = LambdaTermAlgebraSymbol.functor();

        LambdaTermAlgebraSymbol lt1 = LambdaTermAlgebraSymbol.lterm(p("(lambda \$x (lambda \$f (\$x \$f)))"));
        LambdaTermAlgebraSymbol lt2 = LambdaTermAlgebraSymbol.lterm(p("(lambda \$a (\$a \$a))"));
        //LambdaTermAlgebraSymbol lt3 = LambdaTermAlgebraSymbol.lterm(p("(love:i)"));
        // LambdaTermAlgebraSymbol lt3 = LambdaTermAlgebraSymbol.lterm(c("love"));
        LambdaTermAlgebraSymbol lt3 = LambdaTermAlgebraSymbol.lterm(p("(lambda \$r (\$r \$x))"));

        t1.addNode(null,func,null);
        t1.addNode(lt1,t1.getRoot());
        t1.addNode(lt2,t1.getRoot());

        t2.addNode(null,func,null);
        t2.addSubTree(t1,t2.getRoot());
        t2.addNode(lt3,t2.getRoot());


        LambdaTermAlgebra algebra = new LambdaTermAlgebra();

        LambdaTerm test = a(v("x"),v("x"));

        assertEquals(test,algebra.evaluate(t2));

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

