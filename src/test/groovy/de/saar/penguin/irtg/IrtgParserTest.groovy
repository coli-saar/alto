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
            // declarating the interpretations
            interpretation 1: de.saar.penguin.irtg.algebra.StringAlgebra  // another comment

            // automaton starts here

r1(NP,VP) -> S!
  [1] *(?1,?2)


r2(Det,N) -> NP
  [1] *(?1,?2)


r3(N,PP) -> N
  [1] *(?1,?2)


r4(V,NP) -> VP
  [1] *(?1,?2)


r5(VP,PP) -> VP
  [1] *(?1,?2)


r6(P,NP) -> PP
  [1] *(?1,?2)


r7 -> NP
  [1] john


r8 -> V
  [1] watches


r9 -> Det
  [1] the


r10 -> N
  [1] woman


r11 -> N
  [1] telescope


r12 -> P
  [1] with





        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));

        String string = "john watches the woman with the telescope";
        BottomUpAutomaton chart = irtg.parse(["1": string]);
        chart.makeAllRulesExplicit();

//        System.err.println("\n\nreduced:\n" + chart.reduce());

        assert chart.accepts(parseTree("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"));
        assert chart.accepts(parseTree("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"));

        assertEquals(2, chart.countTrees());
    }

    @Test(expected=ParseException.class)
    public void testIllegalInterpretation() {
        String grammarstring = "interpretation 1: java.lang.String";
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }

    @Test(expected=ParseException.class)
    public void testUndeclaredInterpretation() {
        String grammarstring = '''
            interpretation 1: de.saar.penguin.irtg.algebra.StringAlgebra

            a -> Foo [2] bar
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }

    @Test(expected=ParseException.class)
    public void testInconsistentHoms() {
        String grammarstring = '''
            interpretation 1: de.saar.penguin.irtg.algebra.StringAlgebra

            a -> Foo [1] bar
            a -> Fooo [1] baz
        ''';
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
    }
}

