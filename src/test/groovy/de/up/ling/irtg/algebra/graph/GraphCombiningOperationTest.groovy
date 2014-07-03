/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*

import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*


import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

import static Util.*;

/**
 *
 * @author koller
 */
class GraphCombiningOperationTest {
    @Test
    public void testEvaluate() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("\\x (x / boy)"));
        LambdaGraph g3 = IsiAmrParser.parse(new StringReader("\\x, y (x / go-01  :ARG0 (y))"));
        
        Tree term = Tree.create(GraphCombiningOperation.opCombine(["g", "b"]),
            Tree.create(GraphCombiningOperation.opCombine(["b"]),
                Tree.create(GraphCombiningOperation.opGraph(g1)),
                Tree.create(GraphCombiningOperation.opVar(0))),
            Tree.create(GraphCombiningOperation.opVar(1)));

        GraphCombiningOperation gco = new GraphCombiningOperation(term)
        
        LambdaGraph result = gco.evaluate([g2, g3]);
        
        assertIsomorphic(pg(IsiAmrParserTest.AMR1), result);
    }
    
    @Test
    public void testParse() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        GraphCombiningOperation gco = IsiAmrParser.parseOperation(new StringReader(OP1));
        
        Tree gold = Tree.create(GraphCombiningOperation.opCombine(["g", "b"]),
            Tree.create(GraphCombiningOperation.opCombine(["b"]),
                Tree.create(GraphCombiningOperation.opGraph(g1)),
                Tree.create(GraphCombiningOperation.opVar(0))),
            Tree.create(GraphCombiningOperation.opVar(1)));
        
        assertEquals(gold, gco.getOps());
    }
    
    @Test
    public void testParseEvaluate() {
        GraphCombiningOperation gco = IsiAmrParser.parseOperation(new StringReader(OP1));
        
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("\\x (x / boy)"));
        LambdaGraph g3 = IsiAmrParser.parse(new StringReader("\\x, y (x / go-01  :ARG0 (y))"));

        LambdaGraph result = gco.evaluate([g2, g3]);
        
        assertIsomorphic(pg(IsiAmrParserTest.AMR1), result);
    }
    
    @Test
    public void testParseNullary() {
        GraphCombiningOperation gco = IsiAmrParser.parseOperation(new StringReader(OP2));

        LambdaGraph result = gco.evaluate([]);
        
        assertIsomorphic(pg(OP2), result);
    }
    
    @Test
    public void testEmptyGraph() {
        GraphCombiningOperation gco = IsiAmrParser.parseOperation(new StringReader("\\x ( ) + ?1(x)"));
        LambdaGraph g = IsiAmrParser.parse(new StringReader("\\x (x / boy)"));
        LambdaGraph result = gco.evaluate([g]);
        assertIsomorphic(g, result)        
    }
    
    @Test
    public void testCombine() {
        GraphCombiningOperation gco1 = IsiAmrParser.parseOperation(new StringReader("(g / go :ARG0 (b)) + ?1(b)"));
        GraphCombiningOperation gco2 = IsiAmrParser.parseOperation(new StringReader("\\x (x / boy)"));
        
        LambdaGraph result = gco1.evaluate([gco2.evaluate([])])
        LambdaGraph g = IsiAmrParser.parse(new StringReader("(g / go :ARG0 (b / boy))"));
        
        assertIsomorphic(g, result)
    }
    
    @Test
    public void testCombine2() {
        GraphCombiningOperation gco1 = IsiAmrParser.parseOperation(new StringReader("(w / want :ARG0 (b) :ARG1 (g)) + ?1(b,g))"));
        GraphCombiningOperation gco2 = IsiAmrParser.parseOperation(new StringReader("\\x, y (x :ARG0 y)"));
        
        LambdaGraph result = gco1.evaluate([gco2.evaluate([])])
        LambdaGraph g = IsiAmrParser.parse(new StringReader("(w / want :ARG0 (b :ARG0 (g)) :ARG1 (g))"));
        
        assertIsomorphic(g, result)
    }
    
    @Test
    public void testCombine3() {
        GraphCombiningOperation gco1 = IsiAmrParser.parseOperation(new StringReader("\\x, y () + ?1(x,y) + ?2(x)"));
        GraphCombiningOperation gco2 = IsiAmrParser.parseOperation(new StringReader("\\x, y (x :ARG0 y)"));
        GraphCombiningOperation gco3 = IsiAmrParser.parseOperation(new StringReader("\\x (x / boy)"));
        
//        System.err.println(gco1)
//        System.err.println(gco2)
//        System.err.println(gco3)
        
        LambdaGraph result = gco1.evaluate([gco2.evaluate([]), gco3.evaluate([])])
        LambdaGraph g = IsiAmrParser.parse(new StringReader("\\x, y (x / boy :ARG0 y)"));
        
//        System.err.println(result)
        
        assertIsomorphic(g, result)
    }
    
    private static final String OP1 = "(w / want-01  :ARG0 (b)  :ARG1 (g)) + ?1(b) + ?2(g, b)";
    private static final String OP2 = "\\g, b (g / go-01  :ARG0 (b))"
}

