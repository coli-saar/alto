/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.maxent


import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.saar.penguin.irtg.algebra.*;
import de.saar.penguin.irtg.hom.*;
import static de.saar.penguin.irtg.util.TestingTools.*;
import de.saar.penguin.irtg.*
/**
 *
 * @author koller
 */
class MaximumEntropyIrtgTest {
    @Test
    public void testMaxentIrtgParsing() {
        InterpretedTreeAutomaton irtg = iparse(CFG_STR);
        assert irtg instanceof MaximumEntropyIrtg;
        assertEquals( new HashSet(["f1","f2"]), irtg.getFeatureNames());

        irtg.readWeights(new StringReader(WEIGHTS_STR));
        TreeAutomaton chart = irtg.parseFromReaders(i:new StringReader(SENTENCE_STR));
        Set<Rule> rules = chart.getRuleSet();
        Iterator<Rule> ruleIter = rules.iterator();
        while (ruleIter.hasNext()) {
            Rule<String> rule = ruleIter.next();
            assert rule.getWeight() > 0.0, "Rule weight must be greater than 0.0";
        }
    }
    
    
    private static final String CFG_STR = """
interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra
feature f1: de.saar.penguin.irtg.maxent.StaticFeature
feature f2: de.saar.penguin.irtg.maxent.DetFeature
r1(NP,VP) -> S!
  [i] *(?1,?2)
r4(V,NP) -> VP 
  [i] *(?1,?2)
r5(VP,PP) -> VP
  [i] *(?1,?2)
r6(P,NP) -> PP
  [i] *(?1,?2)
r7 -> NP
  [i] john
r2(Det,N) -> NP
  [i] *(?1,?2)
r8 -> V
  [i] watches
r9 -> Det
  [i] the
r10 -> N
  [i] woman
r11 -> N
  [i] telescope
r3(N,PP) -> N
  [i] *(?1,?2)
r12 -> P
  [i] with""";

    private static final String WEIGHTS_STR = """
f1 = 0.2
f2 = 0.8""";
    private static final String SENTENCE_STR = "john watches the woman with the telescope";
	
    
    private static InterpretedTreeAutomaton iparse(String s) {
        return IrtgParser.parse(new StringReader(s));
    }
}

public class StaticFeature implements FeatureFunction {
    private double staticFeatureValue;
    public StaticFeatureFunction(){
        this.staticFeatureValue = 0.5;
    }

    public double evaluate(Rule rule){
        return this.staticFeatureValue;
    }
}

public class DetFeature implements FeatureFunction {
    private static final double DEFAULT_VALUE = 0.5;

    public double evaluate(Rule rule){
        String label = rule.getLabel();
        if(label.equals("r1")){
            return 0.1;
        }else if(label.equals("r2")){
            return 0.2;
        }else if(label.equals("r3")){
            return 0.7;
        }else if(label.equals("r4")){
            return 0.3;
        }else if(label.equals("r5")){
            return 0.08;
        }else if(label.equals("r6")){
            return 0.24;
        }else if(label.equals("r7")){
            return 0.8;
        }else if(label.equals("r8")){
            return 0.54;
        }else if(label.equals("r9")){
            return 0.66;
        }else{
            return DEFAULT_VALUE;
        }
    }
}
