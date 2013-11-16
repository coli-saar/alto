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
class LambdaGraphTest {
    @Test
    public void testIso() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(v / want-01  :ARG0 (b)  :ARG1 (g))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        assertIsomorphic(g1, g2, true);
    }
    
    @Test
    public void testIso2() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-02  :ARG0 (b)  :ARG1 (g))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        assertIsomorphic(g1, g2, false);
    }
    
    @Test
    public void testIso3() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (w)  :ARG1 (g))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        assertIsomorphic(g1, g2, false);
    }
	
    @Test
    public void testMerge() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("\\x (x / boy)"));
        LambdaGraph g3 = IsiAmrParser.parse(new StringReader("\\x, y (x / go-01  :ARG0 (y))"));
        
        LambdaGraph g2b = g2.apply(["b"])
        LambdaGraph g3b = g3.apply(["g", "b"])
        
        g1.merge(g2b);
        g1.merge(g3b);
        
        assertIsomorphic(pg(IsiAmrParserTest.AMR1), g1);
    }
    
    @Test
    public void testRenameApplyMerge() {
        LambdaGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("\\x (x / boy)"));
        LambdaGraph g3 = IsiAmrParser.parse(new StringReader("\\x, y (x / go-01  :ARG0 (y))"));
        
        LambdaGraph g1r = g1.renameNodes();
        LambdaGraph g2r = g2.renameNodes()
        LambdaGraph g2b = g2r.apply(g1r.mapNodeNames(["b"]))
        LambdaGraph g3b = g3.renameNodes().apply(g1r.mapNodeNames(["g", "b"]))
      
        g1r.merge(g2b);
        g1r.merge(g3b);
        
        assertIsomorphic(pg(IsiAmrParserTest.AMR1), g1r);
    }
    
    @Test
    public void testApply() {
        LambdaGraph g3 = IsiAmrParser.parse(new StringReader("\\g, b (g / go-01  :ARG0 (b))"));
        LambdaGraph result = g3.apply(["x", "y"])
        
        assertIsomorphic(pg("(x / go-01 :ARG0 (y))"), result);
    }
    
    @Test
    public void testRename() {
        LambdaGraph g3 = IsiAmrParser.parse(new StringReader("\\g, b (g / go-01  :ARG0 (b))"));
        LambdaGraph result = g3.renameNodes();
        
        assertIsomorphic(g3, result);
        
        result.getGraph().vertexSet().each { assert ! it.getName().equals("g") }
        result.getGraph().vertexSet().each { assert ! it.getName().equals("b") }
    }
    
    @Test
    public void testEquals() {
        LambdaGraph g = IsiAmrParser.parse(new StringReader("\\g, b (g / go-01  :ARG0 (b))"));
        LambdaGraph g2 = g.renameNodes();
        
        assert g.equals(g2)
        assert g2.equals(g)
    }
    
    @Test
    public void testNonequalsVariables() {
        LambdaGraph g = IsiAmrParser.parse(new StringReader("\\g, b (g / go-01  :ARG0 (b))"));
        LambdaGraph g2 = IsiAmrParser.parse(new StringReader("\\b (g / go-01  :ARG0 (b))"));
        
        assert ! g.equals(g2)
        assert ! g2.equals(g)
    }
    
    @Test
    public void testHashcode() {
        LambdaGraph g = IsiAmrParser.parse(new StringReader("\\g, b (g / go-01  :ARG0 (b))"));
        LambdaGraph g2 = g.renameNodes();
        
        Set<LambdaGraph> x = new HashSet();
        x.add(g);
        assert x.contains(g2);
        
        x.clear();
        x.add(g2);
        assert x.contains(g);
    }
    
}

