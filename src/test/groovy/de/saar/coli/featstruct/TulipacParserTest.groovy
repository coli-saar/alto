package de.saar.coli.featstruct

import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.codec.InputCodec
import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.graph.SGraph
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;


class TulipacParserTest {
    @Test
    public void testIssue46() {
        String filename = "46.tag";
        InputCodec<InterpretedTreeAutomaton> ic = InputCodec.getInputCodecByNameOrExtension(filename, null);
        InputStream is = rs(filename);
        InterpretedTreeAutomaton irtg = ic.read(is);
        TagStringAlgebra sa = (TagStringAlgebra) irtg.getInterpretation("string").getAlgebra();
        Homomorphism fh = irtg.getInterpretation("ft").getHomomorphism();
        FeatureStructureAlgebra fsa = (FeatureStructureAlgebra) irtg.getInterpretation("ft").getAlgebra();

        Object inp = sa.parseString("mer a a b c a a b c");
        TreeAutomaton chart = irtg.parseWithSiblingFinder("string", inp);
        TreeAutomaton filtered = chart;

        Tree<String> dt = chart.viterbi();
        TreeAutomaton ftFilter = fsa.nullFilter().inverseHomomorphism(fh);
        filtered = chart.intersect(ftFilter);

        assertEquals(chart.viterbi(), pt("subj-mer__(*NOP*_N_A,copy_a-a__(*NOP*_A_A,just_a-a__(*NOP*_A_leaf_A),aux_a-a__(*NOP*_A_A,just_a-a__(*NOP*_A_leaf_A),aux_b-b__(*NOP*_B_A,just_b-b__(*NOP*_B_leaf_A),aux_c-c__(*NOP*_C_A,just_c-c__(*NOP*_C_leaf_A),*NOP*_VP_A)))),*NOP*_S_A)"))
    }

}
