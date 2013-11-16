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
    
    private static final String OP1 = "(w / want-01  :ARG0 (b)  :ARG1 (g)) + apply(?1, b) + apply(?2, g, b)";
    private static final String OP2 = "\\g, b (g / go-01  :ARG0 (b))"
}

