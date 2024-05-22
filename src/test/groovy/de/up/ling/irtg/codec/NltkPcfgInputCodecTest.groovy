package de.up.ling.irtg.codec

import de.up.ling.irtg.InterpretedTreeAutomaton
import org.junit.Test

import static de.up.ling.irtg.util.TestingTools.pt
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue;


class NltkPcfgInputCodecTest {
    @Test
    public void testToyPcfg() {
        // from https://www.nltk.org/howto/grammar.html
        String PCFG = """
S -> NP VP [1.0]
NP -> Det N [0.5] | NP PP [0.25] | 'John' [0.1] | 'I' [0.15]
Det -> 'the' [0.8] | 'my' [0.2]
N -> 'man' [0.5] | 'telescope' [0.5]
VP -> VP PP [0.1] | V NP [0.7] | V [0.2]
V -> 'ate' [0.35] | 'saw' [0.65]
PP -> P NP [1.0]
P -> 'with' [0.61] | 'under' [0.39]""";

        NltkPcfgInputCodec codec = new NltkPcfgInputCodec();
        InterpretedTreeAutomaton irtg = codec.read(PCFG);

        System.err.println(irtg);

        Set decoded = irtg.decode("tree", ["string": "John ate my telescope"]);
        assertEquals(pt("S(NP(John), VP(V(ate), NP(Det(my), N(telescope))))"), decoded.iterator().next());
    }

    @Test(expected = CodecParseException.class)
    public void testEmptyProduction() {
        String PCFG = """
S -> A B
A -> 'a'
# An empty production:
B -> 'b' |
""";

        NltkPcfgInputCodec codec = new NltkPcfgInputCodec();
        InterpretedTreeAutomaton irtg = codec.read(PCFG);

        System.err.println(irtg);
    }
}
