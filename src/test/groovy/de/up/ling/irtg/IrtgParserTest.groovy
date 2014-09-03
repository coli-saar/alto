/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg


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

/**
 *
 * @author koller
 */
class IrtgParserTest {
    @Test
    public void testParser() {
        String grammarstring = '''
            /* declarating the interpretations */
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra  /* another comment */

            /* automaton starts here */

S! -> r1(NP,VP)
  [i] *(?1,?2)


NP -> r2(Det,N)
  [i] *(?1,?2)


N -> r3(N,PP)
  [i] *(?1,?2)


VP -> r4(V,NP) [.6]
  [i] *(?1,?2)


VP -> r5(VP,PP) [0.4]
  [i] *(?1,?2)


PP -> r6(P,NP) 
  [i] *(?1,?2)


NP -> r7 
  [i] john


V -> r8 
  [i] watches


Det -> r9
  [i] the


N -> r10
  [i] woman


N -> r11
  [i] telescope


P -> r12
  [i] with





        ''';

        InterpretedTreeAutomaton irtg = pi(grammarstring)
        
//        System.err.println("parsed: " + irtg.getAutomaton())

        String string = "john watches the woman with the telescope";
        List words = irtg.parseString("i", string);
        TreeAutomaton chart = irtg.parseInputObjects(["i": words]);

//                System.err.println("\n\nreduced:\n" );

//                System.err.println("chart: " + chart);

        chart.makeAllRulesExplicit();

//                System.err.println("\n\nreduced:\n" + chart.reduceBottomUp());

        assert chart.accepts(pt("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        assert chart.accepts(pt("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"));

        assertEquals(2, chart.countTrees());

        assertEquals( gfrtd("r4", "VP", irtg).getWeight(), 0.6, 0.001);
        assertEquals( gfrtd("r5", "VP", irtg).getWeight(), 0.4, 0.001);
        assertEquals( gfrtd("r1", "S", irtg).getWeight(), 1.0, 0.001);
    }
    
    private Rule gfrtd(String label, String parentState, InterpretedTreeAutomaton irtg) {
        TreeAutomaton auto = irtg.getAutomaton();
        return auto.getRulesTopDown(auto.getSignature().getIdForSymbol(label), auto.addState(parentState)).iterator().next();
    }
    
    @Test
    public void testQuotedName() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a [i] 'foo bar'
        ''';

        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }

    @Test
    public void testQuotedName2() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a [i] '"'
        ''';

        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }
    
    @Test
    public void testDoubleQuotedName() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a [i] "foo bar"
        ''';

        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }
    
    @Test
    public void testDoubleQuotedName2() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a [i] "'"
        ''';

        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }

    @Test(expected=de.up.ling.irtg.codec.ParseException.class)
    public void testIllegalInterpretation() {
        String grammarstring = "interpretation 1: java.lang.String";
        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }

    @Test(expected=de.up.ling.irtg.codec.ParseException.class)
    public void testUndeclaredInterpretation() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a [j] bar
        ''';

        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }

    @Test(expected=de.up.ling.irtg.codec.ParseException.class)
    public void testInconsistentHoms() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a  [i] bar
            Fooo -> a  [i] baz
        ''';
        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }
    
    @Test(expected=de.up.ling.irtg.codec.ParseException.class)
    public void testTooManyVariables0() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a
            [i] bar(?1)
        ''';
        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }
    
    @Test(expected=de.up.ling.irtg.codec.ParseException.class)
    public void testTooManyVariables2() {
        String grammarstring = '''
            interpretation i: de.up.ling.irtg.algebra.StringAlgebra

            Foo -> a(B,C)
            [i] bar(?1, ?2, ?3)
        ''';
        InterpretedTreeAutomaton irtg = pi(grammarstring)
    }
}

