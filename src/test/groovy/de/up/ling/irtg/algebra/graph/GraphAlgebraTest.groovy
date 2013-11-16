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
}

