/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg


import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.codec.InputCodec
import de.up.ling.irtg.codec.TemplateIrtgInputCodec
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg
import de.up.ling.irtg.util.FirstOrderModel
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class TemplateInterpretedTreeAutomatonTest {
    @Test
    public void testParse() {
        InputCodec<TemplateInterpretedTreeAutomaton> codec = new TemplateIrtgInputCodec();
        TemplateInterpretedTreeAutomaton tirtg = codec.read(TIRTG);
//        System.out.println(tirtg)
    }
    
    @Test
    public void testInstantiate() {
        InputCodec<TemplateInterpretedTreeAutomaton> codec = new TemplateIrtgInputCodec();
        TemplateInterpretedTreeAutomaton tirtg = codec.read(TIRTG);
        
        FirstOrderModel model = FirstOrderModel.read(new StringReader(MODEL));
        
        tirtg.instantiate(model)
    }
    
    @Test
    public void testMaxent() {
        InputCodec<TemplateInterpretedTreeAutomaton> codec = new TemplateIrtgInputCodec();
        TemplateInterpretedTreeAutomaton tirtg = codec.read(MAXENT_TIRTG);
        
        FirstOrderModel model = FirstOrderModel.read(new StringReader(MODEL));
        MaximumEntropyIrtg mirtg = (MaximumEntropyIrtg) tirtg.instantiate(model)
        
        assertEquals(0.1, mirtg.getFeatureFunction("f1").evaluate(null, null, null), 0.01)
    }
    
    private final static String MODEL = '''{"sleep": [["e", "r1"]], "takefrom": [["e2", "r1", "h"]], "rabbit": [["r1"], ["r2"]], "white": [["r1"], ["b"]], "brown": [["r2"]], "in": [["r1","h"], ["f","h2"]], "hat": [["h"], ["h2"]] }''';
    
    private final static String TIRTG = '''\n\

interpretation sem: de.up.ling.irtg.algebra.SetAlgebra
interpretation string: de.up.ling.irtg.algebra.StringAlgebra


foreach {a, b | in(a,b) and rabbit(a)}:
  N_$a ! -> leftof_$a_$b(N_$a, NP_$b, $a) [0.2]
  [sem] project_1(intersect_2(intersect_1(left_of, ?1), ?2))
  [string] *(?1, *("left of", ?2))\n\

A -> f(B,C)\n\
[sem] intersect_1(?1,?2)\n\
[string] *(?1,?2)\n\


    ''';
    
    private final static String MAXENT_TIRTG = """\n\

interpretation i: de.up.ling.irtg.algebra.StringAlgebra

feature f1: de.up.ling.irtg.util.TestingTools::makeTestFeature('0.1')

S! -> r1(NP,VP)
  [i] *(?1,?2)


    """;
}

