/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import com.google.common.base.Function;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.align.find_rules.TreeAddingAutomaton;
import de.up.ling.irtg.align.find_rules.VariableIndication;
import de.up.ling.irtg.align.pruning.RemoveDead;
import de.up.ling.irtg.align.pruning.StringLeftOrRight;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntDoubleFunction;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RuleCountMalignTest {
    
        /**
     * 
     */
    private StringAlgebra alg1;
    
    /**
     * 
     */
    private StringAlgebra alg2;
    
    /**
     * 
     */
    private HomomorphismManager homa;
    
    /**
     * 
     */
    private TreeAutomaton sampAut;
    
    @Before
    public void setUp() {
        alg1 = new StringAlgebra();
        alg2 = new StringAlgebra();
        
        TreeAutomaton t1 = alg1.decompose(alg1.parseString("More recently several authors have explored supervised"
                + " methods"));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString("Seit neuestem haben einige Authoren überwachte Methoden"
                + " erforscht"));
        
        
        SpanAligner spa1 = new SpanAligner("0:1:1 0:1:2 1:2:3 1:2:4 2:3:5 3:4:6 4:5:7 5:6:8 6:7:9 7:8:10", t1);
        SpanAligner spa2 = new SpanAligner("0:1:1 1:2:2 0:1:3 1:2:4 3:4:5 4:5:6 2:3:7 7:8:8 5:6:9 6:7:10", t2);
        
        Propagator prop1 = new Propagator(new StringLeftOrRight());
        Propagator prop2 = new Propagator();
        
        t1 = prop1.convert(t1, spa1);
        t2 = prop2.convert(t2, spa2);
        
        homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
        homa.update(t1.getAllLabels(), t2.getAllLabels());
        
        
        RuleFindingIntersectionAutomaton rfa = 
                new RuleFindingIntersectionAutomaton(t1, t2, homa.getHomomorphism1(), homa.getHomomorphism2());
        sampAut = new TopDownIntersectionAutomaton(rfa, homa.getRestriction());
        
        sampAut = RemoveDead.reduce(sampAut);
    }

    @Test
    public void testSampling() {
        RuleCountMalign scm = new RuleCountMalign(new RuleCountBenign(1.0, 234294299234924L), 1.0, 234294299234924L);
        SampleMalign.SamplingConfiguration cof = new SampleMalign.SamplingConfiguration();
        
        cof.outerPreSamplingRounds = 1;
        cof.outerSampleSize = 200;
        cof.rounds = 2;
        cof.label2TargetLabel = (Function<Rule,Integer>) (Rule input) -> {
            if(homa.isVariable(input.getLabel())){
                return homa.getDefaultVariable();
            }else{
                return input.getLabel();
            }
        };
        cof.sampleSize = (int value) -> 500;
        
        VariableIndication vi = new VariableIndication() {

            @Override
            public boolean isVariable(int label) {
                return homa.isVariable(label);
            }

            @Override
            public boolean isIgnorableVariable(int i) {
                return homa.isVariable(i) && i != homa.getDefaultVariable();
            }
        };
        
        IntDoubleFunction smooth = new IntDoubleFunction() {

            @Override
            public double apply(int value) {
                if(homa.isVariable(value)){
                    return 1.0;
                }else{
                    return 0.01;
                }
            }
        };
        scm.setMalign(sampAut);
        
        cof.target = new TreeAddingAutomaton(homa.getSignature(), smooth, vi);
        List<Tree<Rule>> samp = scm.createSample(cof);
        
        Function<Rule,Integer> intermediate = (Function<Rule,Integer>) (Rule input) -> {
            if(homa.isVariable(input.getLabel())){
                return homa.getDefaultVariable();
            }else{
                return input.getLabel();
            }
        };
        
        Object2IntOpenHashMap<Tree<Rule>> map = new Object2IntOpenHashMap<>();
        Object2DoubleOpenHashMap<Tree<Rule>> exp = new Object2DoubleOpenHashMap<>();
        double sum = 0.0;
        
        for(Tree<Rule> t : samp){
            map.addTo(t, 1);
            
            if(exp.containsKey(t)){
                continue;
            }
            
            Tree<Integer> ti = t.map(cof.label2TargetLabel);
            double d = cof.target.getWeightRaw(ti);
            sum += d;
            exp.put(t, d);
        }
        
        for(Tree<Rule> t : map.keySet()){
            assertEquals(map.getInt(t) / (double) samp.size(),exp.getDouble(t) / sum, 0.1);
        }
        assertTrue(map.size() > 1);
    }   
}