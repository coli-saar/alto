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
import java.util.HashSet;


/**
 *
 * @author koller, melzer
 */
class LambdaTermParserTest {
    @Test
    public void testConstant() {
        assertEquals(LambdaTerm.constant("a", "e"), p("a:e"));
    }
    
    @Test
    public void testVariable() {
        assertEquals(LambdaTerm.variable("\$0"), p("\$0"));
    }
    
    @Test
    public void testComplex() {
        assertEquals(LambdaTerm.lambda("\$0", LambdaTerm.argmax("\$1",
                        LambdaTerm.conj(a(c("state","a"), v("0")), a(c("next","a"), v("0"), v("1")), a(c("foo","a"), v("1"))),
                        a(c("elevation","i"), v("1"))
                )),
            p("(lambda \$0 (argmax \$1 (and (state:a \$0) (next:a \$0 \$1) (foo:a \$1)) (elevation:i \$1)))"));
    }
    
    @Test
    public void testGeo880_1() {
        assertEquals(a(c("population","i"), a(c("capital","c"), 
                        LambdaTerm.argmax("\$1", LambdaTerm.conj(a(c("state","t"),v("1")), a(c("loc","t"), c("mississippi_river","r"), v("1"))), a(c("size","i"), v("1")))
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
    public void testToStringParse() {
        LambdaTerm test = p("((lambda \$f (lambda \$x (\$n \$f (\$f \$x)))) (lambda \$a (lambda \$b \$b)))");
	LambdaTerm parsed=p(test.toString());        
	assertEquals(test, parsed);
    }


    @Test
    public void testToStringParseGeo() {
        LambdaTerm test = p("(population:i (capital:c (argmax \$1 (and (state:t \$1) (loc:t mississippi_river:r \$1)) (size:i \$1))))");
	LambdaTerm parsed=p(test.toString());        
	assertEquals(test, parsed);
    }

    @Test
    public void testToStringParseGeo2() {
        LambdaTerm test = p("(argmin \$0 (and (place:t \$0) (loc:t \$0 california:s)) (elevation:i \$0))");
	LambdaTerm parsed=p(test.toString());        
	assertEquals(test, parsed);
    }
    
    @Test
    public void testToStringParseWithConstants() {
        LambdaTerm test = p("((lambda \$f (lambda \$x (\$n \$f (\$f a:e)))) (lambda \$a (lambda \$b \$b)))");
	LambdaTerm parsed=p(test.toString());        
	assertEquals(test, parsed);
    }

    //@Test
    public void testParseGeo1() {
        LambdaTerm test = p("(argmax \$0 (and (river:t \$0) (exists \$1 (and (state:t \$1) (loc:t (argmax \$2 (place:t \$2) (elevation:i \$2)) \$1)) (loc:t \$0 \$1))) (len:i \$0))");
	//LambdaTerm parsed=p(test.toString());        
	//assertEquals(test, parsed);
    }

	
    //@Test
    public void testParseGeo2() {
        LambdaTerm test = p("(count \$0 (and (state:t \$0) (exists \$1 (and (city:t \$1) (named:t \$1 rochester:n) (loc:t \$1 \$0)))))");
	//LambdaTerm parsed=p(test.toString());        
	//assertEquals(test, parsed);
    }



    @Test
    public void testParseGeo3() {
        LambdaTerm test = p("(lambda \$0 (and (capital:t \$0) (not (and (major:t \$0) (city:t \$0)))))");
	//LambdaTerm parsed=p(test.toString());        
	//assertEquals(test, parsed);
    }

    @Test
    public void testParseGeo4() {
        LambdaTerm test = p("(lambda \$0 (exists \$1 (and (state:t \$1) (loc:t mississippi_river:r \$1) (= (population:i \$1) \$0))))");
	//LambdaTerm parsed=p(test.toString());        
	//assertEquals(test, parsed);
    }

    @Test
    public void testParseGeo5() {
        LambdaTerm test = p("(lambda \$0 (and (state:t \$0) (> (count \$1 (and (major:t \$1) (river:t \$1) (loc:t \$1 \$0))) 0:i)))");
	//LambdaTerm parsed=p(test.toString());        
	//assertEquals(test, parsed);
    }


    private static LambdaTerm a(f, LambdaTerm... a) {
        return LambdaTerm.apply(f,Arrays.asList(a));
    }
    
    private static LambdaTerm c(x,type) { // c(x,type)
        return LambdaTerm.constant(x, type);
    }
    
    private static LambdaTerm v(x) {
        return LambdaTerm.variable("\$" + x);
    }
    
    private static LambdaTerm p(s) {
        return LambdaTermParser.parse(new StringReader(s));
    }
}

