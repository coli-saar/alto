/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.maxent


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
import de.up.ling.irtg.*
import de.up.ling.irtg.corpus.*
import java.util.logging.Level


/**
 *
 * @author koller
 * @author Danilo Baumgarten
 */
class MaximumEntropyIrtgTest {
    @BeforeClass
    public static void setup() {
        MaximumEntropyIrtg.setLoggingLevel(Level.OFF);
    }
    
//    @Test
    public void testMaxentIrtgParsing() {
        InterpretedTreeAutomaton irtg = pi(CFG_STR);
        assert irtg instanceof MaximumEntropyIrtg;
        
        assertEquals( new ArrayList(["f1","f2"]), irtg.getFeatureNames());

        irtg.readWeights(new StringReader(WEIGHTS_STR));
        TreeAutomaton chart = irtg.parse(i:SENTENCE_STR);
        
        Iterable<Rule> rules = chart.getRuleSet();
        for( Rule<String> rule : rules ) {
            assert rule.getWeight() > 0.0, "Rule weight must be greater than 0.0";
        }
        
        Iterator<WeightedTree> lit = chart.sortedLanguageIterator();
        
        WeightedTree wt = lit.next();
        assertEquals(pt("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"), chart.getSignature().resolve(wt.getTree()));
        assertAlmostEquals(Math.exp(0.8), wt.getWeight());
        
        wt = lit.next();
        assertEquals(pt("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))"), chart.getSignature().resolve(wt.getTree()));
        assertAlmostEquals(Math.exp(0.2), wt.getWeight());
    }
    
    @Test
    public void testFeatureWithArgs() {
        MaximumEntropyIrtg irtg = (MaximumEntropyIrtg) pi(ARGFT_GRAMMAR);
        assertEquals( new ArrayList(["f1","f2"]), irtg.getFeatureNames());

        assertEquals("one", irtg.getFeatureFunction("f1").getX());
        assertEquals("two", irtg.getFeatureFunction("f2").getX());
    }
    
    @Test
    public void testMaxEntTraining() {
        InterpretedTreeAutomaton irtg = pi(CFG_STR);
        assert irtg instanceof MaximumEntropyIrtg;
//        MaximumEntropyIrtg trainer = new MaximumEntropyIrtg(irtg);

        Corpus anCo = Corpus.readCorpus(new StringReader(TRAIN1_STR), irtg);
        irtg.trainMaxent(anCo);
        double[] fWeights = irtg.getFeatureWeights();
        assert (fWeights[0] > fWeights[1]), "weights are not optimized";

        anCo = Corpus.readCorpus(new StringReader(TRAIN2_STR), irtg);
        irtg.trainMaxent(anCo);
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
# IRTG annotated corpus file, v1.0
#
# interpretation i: de.up.ling.irtg.algebra.StringAlgebra

john watches the woman with the telescope
r1(r7,r5( r4(r8, r2(r9,r10)), r6(r12, r2(r9,r11))))
john watches the telescope with the telescope
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r11))))
john watches the telescope with the woman
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r10))))
    """;

    private static final String TRAIN2_STR = """
# IRTG annotated corpus file, v1.0
#
# interpretation i: de.up.ling.irtg.algebra.StringAlgebra

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

N -> r10
  [i] woman

    """;
    
//    private static InterpretedTreeAutomaton iparse(String s) {
//        return pi(new StringReader(s));
//    }
    
    
}
