/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*;
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*

import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

/**
 *
 * @author koller
 */
class GraphAlgebraTest {
    @Test
    public void testEvaluate1() {
        GraphAlgebra alg = new GraphAlgebra();
        
        Tree<String> term = pt("merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj('(x<root> / boy)'))");
        SGraph gold = pg("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))")
        
        assertEquals(gold, alg.evaluate(term))
    }
    
    @Test
    public void testEvaluate2() {
        GraphAlgebra alg = new GraphAlgebra();
        
        Tree<String> term = pt("merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj('(x<root> / boy)')), r_vcomp(r_subj_subj('(g<root> / go-01  :ARG0 (s<subj>))')))");
        SGraph gold = pg("(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        
        assertEquals(gold, alg.evaluate(term))
    }
}

