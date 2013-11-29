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
class GraphAlgebraTest {
    @Test
    public void testEvaluate() {
        GraphAlgebra alg = new GraphAlgebra()
        Tree<String> term = Tree.create("(w / want-01  :ARG0 (b)  :ARG1 (g)) + ?1(b) + ?2(g, b)",
            [Tree.create("\\x (x / boy)"), Tree.create("\\g, b (g / go-01  :ARG0 (b))")]);
                                
        LambdaGraph result = alg.evaluate(term);
            
        assertIsomorphic(pg(IsiAmrParserTest.AMR1), result);
    }
        
    @Test
    public void testDecompose() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(HRG));
        TreeAutomaton decomp = irtg.getInterpretation("graph").getAlgebra().decompose(pg(IsiAmrParserTest.AMR1));
            
        assert decomp.accepts(pt("'(w / want-01  :ARG0 (b)  :ARG1 (g)) + ?1(b) + ?2(g, b)'('\\x (x / boy)', '\\x, y (x / go-01  :ARG0 (y))')"))
    }
        
    @Test
    public void testParse() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(HRG));
        TreeAutomaton chart = irtg.parse(["graph": IsiAmrParserTest.AMR1]);
            
        assertEquals(new HashSet([pt("want(boy,go)")]), chart.language())
    }
        
    @Test
    public void testParse2() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(HRG2));
        TreeAutomaton chart = irtg.parse(["graph": " (g / go :ARG0 (b / boy))"])
            
        assertEquals(new HashSet([pt("go_main(boy)")]), chart.language())
    }
    
    @Test
    public void testParse3() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(HRG2));
        TreeAutomaton chart = irtg.parse(["graph": "(w / want :ARG0 (b :ARG0 (g)) :ARG1 (g))"])
            
        assertEquals(new HashSet([pt("want(arg0)")]), chart.language())
    }
    
    
    
    private static final String HRG = '''
    interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

S! -> want(NP, VP)
[string] *(?1, *(wants, *(to, ?2)))
[graph]  "(w / want-01  :ARG0 (b)  :ARG1 (g)) + ?1(b) + ?2(g, b)" (?1, ?2)

NP -> boy
[string] *(the, boy)
[graph]  "\\x (x / boy)"

VP -> go
[string] go
[graph]  "\\x, y (x / go-01  :ARG0 (y))"


''';
    
    private static final String HRG2 = '''
    
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

X! -> want(X)
[graph]  "(w / want :ARG0 (b) :ARG1 (g)) + ?1(b,g))"(?1)


X -> arg0
[graph] "\\x, y (x :ARG0 y)"

X! -> boy
[graph] "\\x (x / boy)"


X -> go_main(X)
[graph] "(g / go :ARG0 (b)) + ?1(b)"(?1)
''';
}

