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
/**
 *
 * @author koller
 */
class MaximumEntropyIrtgTest {
    @Test
    public void testMaxentIrtgParsing() {
        InterpretedTreeAutomaton irtg = iparse(CFG_STR);
        assert irtg instanceof MaximumEntropyIrtg;
        assertEquals( new ArrayList(["f1","f2"]), irtg.getFeatureNames());

        irtg.readWeights(new StringReader(WEIGHTS_STR));
        TreeAutomaton chart = irtg.parseFromReaders(i:new StringReader(SENTENCE_STR));
        Set<Rule> rules = chart.getRuleSet();
        Iterator<Rule> ruleIter = rules.iterator();
        while (ruleIter.hasNext()) {
            Rule<String> rule = ruleIter.next();
            assert rule.getWeight() > 0.0, "Rule weight must be greater than 0.0";
        }
    }
    
    @Test
    public void testFeatures() {
        FeatureFunction featureFunction = new AlignPPtoNFeature();
        de.saar.basic.Pair parent1 = new de.saar.basic.Pair<String,String>("N","0-4");
        de.saar.basic.Pair[] children = new de.saar.basic.Pair<String,String>[2];
        children[0] = new de.saar.basic.Pair<String,String>("N","0-2");
        children[1] = new de.saar.basic.Pair<String,String>("PP","2-4");
        Rule r1 = new Rule<de.saar.basic.Pair<String,String>>(parent1, "r1", children, 0.0);
        assert featureFunction.evaluate(r1) == 1.0, "feature weight shall be 1.0";

        de.saar.basic.Pair parent2 = new de.saar.basic.Pair<String,String>("VP","0-4");
        Rule r2= new Rule<de.saar.basic.Pair<String,String>>(parent2, "r2", children, 0.0);
        assert featureFunction.evaluate(r2) == 0.0, "feature weight shall be 0.0";

        children[1] = new de.saar.basic.Pair<String,String>("XX","0-0");
        Rule r3= new Rule<de.saar.basic.Pair<String,String>>(parent1, "r2", children, 0.0);
        assert featureFunction.evaluate(r3) == 0.0, "feature weight shall be 0.0";
    }
    
    @Test
    public void testMaxEntTraining() {
        InterpretedTreeAutomaton irtg = iparse(CFG_STR);
        assert irtg instanceof MaximumEntropyIrtg;
        AnnotatedCorpus anCo = AnnotatedCorpus.readAnnotatedCorpus(new StringReader(TRAIN1_STR), irtg);
        irtg.train(anCo);
        double[] fWeights = irtg.getFeatureWeights();
        assert (fWeights[0] > fWeights[1]), "weights are not optimized";

        anCo = AnnotatedCorpus.readAnnotatedCorpus(new StringReader(TRAIN2_STR), irtg);
        irtg.train(anCo);
        fWeights = irtg.getFeatureWeights();
        assert (fWeights[0] < fWeights[1]), "weights are not optimized";
    }
    
    private static final String CFG_STR = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra
feature f1: de.up.ling.irtg.maxent.AlignPPtoVPFeature
feature f2: de.up.ling.irtg.maxent.AlignPPtoNFeature
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
    
    private static InterpretedTreeAutomaton iparse(String s) {
        return IrtgParser.parse(new StringReader(s));
    }
}
