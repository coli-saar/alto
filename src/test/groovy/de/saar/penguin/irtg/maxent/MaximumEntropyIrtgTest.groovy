/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.maxent


import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.saar.penguin.irtg.algebra.*;
import de.saar.penguin.irtg.hom.*;
import static de.saar.penguin.irtg.util.TestingTools.*;
import de.saar.penguin.irtg.*
/**
 *
 * @author koller
 */
class MaximumEntropyIrtgTest {
    @Test
    public void testMaxentIrtgParsing() {
        InterpretedTreeAutomaton irtg = iparse(CFG_STR);
        
        assert irtg instanceof MaximumEntropyIrtg;
        
        assertEquals( new HashSet(["f1"]), irtg.getFeatureNames())
    }
    
    
    private static final String CFG_STR = """
interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra

feature f1: de.saar.penguin.irtg.maxent.TestFeature

r1(NP,VP) -> S!
  [i] *(?1,?2)


r4(V,NP) -> VP 
  [i] *(?1,?2)


r5(VP,PP) -> VP
  [i] *(?1,?2)""";
	
    
    private static InterpretedTreeAutomaton iparse(String s) {
        return IrtgParser.parse(new StringReader(s));
    }
}

public class TestFeature implements FeatureFunction {
    public double evaluate(Rule rule) {
        if( rule.getLabel().equals("r1")) {
            return 1;
        } else {
            return 0;
        }
    }
}