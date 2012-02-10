/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg


import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.saar.basic.tree.*;
import de.saar.penguin.irtg.algebra.*;
import de.saar.penguin.irtg.hom.*;
import static de.saar.penguin.irtg.hom.HomomorphismTest.hom;
import static de.saar.penguin.irtg.InterpretedTreeAutomatonTest.parseTree;

/**
 *
 * @author koller
 */
class IrtgParserTest {
    @Test
    public void testParser() {
        String grammarstring = '''
            /* declarating the interpretations */
            interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra  /* another comment */

            /* automaton starts here */

r1(NP,VP) -> S!
  [i] *(?1,?2)


r2(Det,N) -> NP
  [i] *(?1,?2)


r3(N,PP) -> N
  [i] *(?1,?2)


r4(V,NP) -> VP [.6]
  [i] *(?1,?2)


r5(VP,PP) -> VP [0.4]
  [i] *(?1,?2)


r6(P,NP) -> PP
  [i] *(?1,?2)


r7 -> NP
  [i] john


r8 -> V
  [i] watches


r9 -> Det
  [i] the


r10 -> N
  [i] woman


r11 -> N
  [i] telescope


r12 -> P
  [i] with





        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));

        String string = "john watches the woman with the telescope";
        List words = irtg.parseString("i", string);
        BottomUpAutomaton chart = irtg.parse(["i": words]);
        chart.makeAllRulesExplicit();

//        System.err.println("\n\nreduced:\n" + chart.reduce());

        assert chart.accepts(parseTree("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        assert chart.accepts(parseTree("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"));

        assertEquals(2, chart.countTrees());

        assertEquals( irtg.getAutomaton().getRulesTopDown("r4", "VP").iterator().next().getWeight(), 0.6, 0.001);
        assertEquals( irtg.getAutomaton().getRulesTopDown("r5", "VP").iterator().next().getWeight(), 0.4, 0.001);
        assertEquals( irtg.getAutomaton().getRulesTopDown("r1", "S").iterator().next().getWeight(), 1.0, 0.001);
    }
    
    @Test
    public void testQuotedName() {
        String grammarstring = '''
            interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra

            a -> Foo [i] 'foo bar'
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }

    @Test(expected=ParseException.class)
    public void testIllegalInterpretation() {
        String grammarstring = "interpretation 1: java.lang.String";
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }

    @Test(expected=ParseException.class)
    public void testUndeclaredInterpretation() {
        String grammarstring = '''
            interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra

            a -> Foo [j] bar
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }

    @Test(expected=ParseException.class)
    public void testInconsistentHoms() {
        String grammarstring = '''
            interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra

            a -> Foo [i] bar
            a -> Fooo [i] baz
        ''';
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }
}

