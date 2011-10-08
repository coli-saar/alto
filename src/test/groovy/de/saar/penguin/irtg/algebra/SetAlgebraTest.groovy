/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.algebra

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
class SetAlgebraTest {
    @Test
    public void testParse() {
        SetAlgebra a = new SetAlgebra([:]);
        String s = "{a,b,c}"
        Set<List<String>> result = a.parseString(s);
        Set<List<String>> gold = new HashSet([["a"], ["b"], ["c"]]);
        assertEquals(gold, result);
    }

    @Test
    public void testParse2() {
        SetAlgebra a = new SetAlgebra([:]);
        String s = "{(a,b),(c,d)}"
        Set<List<String>> result = a.parseString(s);
        Set<List<String>> gold = sl([["a", "b"], ["c", "d"]]);
        assertEquals(gold, result);
    }

    @Test
    public void testEvaluate() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("rabbit"))
        Set<List<String>> gold = sl([["r1"], ["r2"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate2() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("intersect_1(rabbit, white)"))
        Set<List<String>> gold = sl([["r1"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate3() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("project_1(intersect_1(in, rabbit))"))
        Set<List<String>> gold = sl([["r1"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate4() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("uniq_r1(project_1(intersect_1(in, rabbit)))"))
        Set<List<String>> gold = sl([["r1"]])
        assertEquals(gold, result)
    }
    
    @Test
    public void testEvaluate5() {
        SetAlgebra a = new SetAlgebra(["rabbit" : sl([["r1"], ["r2"]]), "white" : sl([["r1"], ["b"]]), "in": sl([["r1", "h"], ["f", "h2"]]), "hat": sl([["h"], ["h2"]])])
        Set<List<String>> result = a.evaluate(pt("uniq_r1(rabbit)"))
        Set<List<String>> gold = sl([])
        assertEquals(gold, result)
    }
    

    private Set<List<String>> sl(List<List<String>> ll) {
        return new HashSet<List<String>>(ll);
    }
	
    private static Tree pt(String s) {
        TermParser.parse(s).toTree()
    }
}

