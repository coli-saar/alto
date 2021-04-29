package de.up.ling.irtg.codec

import de.up.ling.irtg.Interpretation
import de.up.ling.irtg.InterpretedTreeAutomaton
import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.TreeAutomaton
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.algebra.graph.*;
import static de.up.ling.irtg.util.TestingTools.*;


class SynchronousCfgInputCodecTest {
    @Test
    public void testScfg() {
        InterpretedTreeAutomaton irtg = new SynchronousCfgInputCodec().read(VALID_GRAMMAR);
        TreeAutomaton chart = irtg.parse(["left": "30 duonianlai de youhao hezuo"]);
        assert chart.accepts(pt("r1(r2(r4(r5),r3(r6,r7)))"));
    }

    @Test
    public void testDecode() {
        InterpretedTreeAutomaton irtg = new SynchronousCfgInputCodec().read(VALID_GRAMMAR);
        Interpretation rightInterp = irtg.getInterpretation("right");
        Tree gold = pt("r1(r2(r4(r5),r3(r6,r7)))");

        assertEquals("friendly cooperation over the past 30 years", rightInterp.getAlgebra().representAsString(rightInterp.interpret(gold)));
    }

    private static final String VALID_GRAMMAR = """
S

S -> X
S -> X

X -> X de X
X -> X[2] X[1]

X -> X X
X -> X[1] X[2]

X -> X duonianlai
X -> over the past X years

X -> 30
X -> 30

X -> youhao
X -> friendly

X -> hezuo
X -> cooperation
""";
}
