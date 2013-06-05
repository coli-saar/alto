/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.maxent


import org.junit.Test
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
import de.up.ling.irtg.corpus.*

/**
 *
 * @author Danilo Baumgarten
 */
class MaximumEntropyIrtgTrainerTest {

    @Test
    public void testMaxEntTraining() {
        InterpretedTreeAutomaton irtg = iparse(CFG_STR);
        assert irtg instanceof MaximumEntropyIrtg;
//        irtg.prepare(false, true);
        MaximumEntropyIrtgTrainer trainer = new MaximumEntropyIrtgTrainer(irtg);

        AnnotatedCorpus anCo = AnnotatedCorpus.readAnnotatedCorpus(new StringReader(TRAIN1_STR), irtg);
        trainer.train(anCo);
        double[] fWeights = irtg.getFeatureWeights();
        assert (fWeights[0] > fWeights[1]), "weights are not optimized";

        anCo = AnnotatedCorpus.readAnnotatedCorpus(new StringReader(TRAIN2_STR), irtg);
        trainer.train(anCo);
        fWeights = irtg.getFeatureWeights();
        assert (fWeights[0] < fWeights[1]), "weights are not optimized";
    }
    
    private static final String CFG_STR = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra
feature f1: de.up.ling.irtg.maxent.ChildOfFeature('VP','PP')
feature f2: de.up.ling.irtg.maxent.ChildOfFeature('N','PP')
S! -> r1(NP,VP)
  [i] *(?1,?2)
VP -> r4(V,NP)
  [i] *(?1,?2)
VP -> r5(VP,PP)
  [i] *(?1,?2)
PP -> r6(P,NP)
  [i] *(?1,?2)
NP -> r7
  [i] john
NP -> r2(Det,N)
  [i] *(?1,?2)
V -> r8
  [i] watches
Det -> r9
  [i] the
N -> r10
  [i] woman
N -> r11
  [i] telescope
N -> r3(N,PP)
  [i] *(?1,?2)
P -> r12
  [i] with""";

    private static final String WEIGHTS_STR = """
f1 = 0.2
f2 = 0.8""";
    private static final String SENTENCE_STR = "john watches the woman with the telescope";
	
    private static final String TRAIN1_STR = """
i
john watches the woman with the telescope
r1(r7,r5( r4(r8, r2(r9,r10)), r6(r12, r2(r9,r11))))
john watches the telescope with the telescope
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r11))))
john watches the telescope with the woman
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r10))))
    """;

    private static final String TRAIN2_STR = """
i
john watches the woman with the telescope
r1(r7,r4( r8, r2(r9,r3(r10, r6(r12, r2(r9,r11))))))
john watches the telescope with the telescope
r1(r7,r4( r8, r2(r9,r3(r11, r6(r12, r2(r9,r11))))))
john watches the telescope with the woman
r1(r7,r4( r8, r2(r9,r3(r11, r6(r12, r2(r9,r10))))))
    """;
    
    private static final String ARGFT_GRAMMAR = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

feature f1: de.up.ling.irtg.maxent.RuleNameFeature('one')
feature f2: de.up.ling.irtg.maxent.RuleNameFeature('two')

    """;
    
    private static InterpretedTreeAutomaton iparse(String s) {
        return IrtgParser.parse(new StringReader(s));
    }
    
    
}
