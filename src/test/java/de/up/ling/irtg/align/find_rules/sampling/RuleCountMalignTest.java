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
import de.up.ling.irtg.align.Pruner;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.align.find_rules.TreeAddingAutomaton;
import de.up.ling.irtg.align.find_rules.VariableIndication;
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
        
        TreeAutomaton t1 = alg1.decompose(alg1.parseString("Different aspects can be studied this way ."));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString("Auf diese Art können verschiedene aspekte studiert werden ."));
        
        
        SpanAligner spa1 = new SpanAligner("0:1:1 1:2:2 5:6:7 5:6:3", t1);
        SpanAligner spa2 = new SpanAligner("4:5:1 5:6:2 1:2:7 0:1:3", t2);
        
        Pruner prune = new StringLeftOrRight();
        Propagator prop = new Propagator(prune);
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
        homa.update(t1.getAllLabels(), t2.getAllLabels());
        
        
        RuleFindingIntersectionAutomaton rfa = 
                new RuleFindingIntersectionAutomaton(t1, t2, homa.getHomomorphism1(), homa.getHomomorphism2());
        sampAut = new TopDownIntersectionAutomaton(rfa, homa.getRestriction());
        
    }

    @Test
    public void testSampling() {
        RuleCountMalign scm = new RuleCountMalign(new RuleCountBenign(1.0), 1.0);
        SampleMalign.SamplingConfiguration cof = new SampleMalign.SamplingConfiguration();
        
        cof.outerPreSamplingRounds = 3;
        cof.outerSampleSize = 500;
        cof.rounds = 3;
        cof.label2TargetLabel = (Function<Rule,Integer>) (Rule input) -> {
            if(homa.isVariable(input.getLabel())){
                return homa.getDefaultVariable();
            }else{
                return input.getLabel();
            }
        };
        cof.sampleSize = (int value) -> 5000;
        
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
                    return 40.0;
                }else{
                    return 0.1;
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
            
            Tree<String> h = t.map(cof.label2TargetLabel).map(new Function<Integer,String>() {

                @Override
                public String apply(Integer input) {
                    return homa.getSignature().resolveSymbolId(input);
                }
                
            });
            
            System.out.println("------------");
            System.out.println(homa.getHomomorphism1().apply(h));
            System.out.println(homa.getHomomorphism2().apply(h));
        }
        
        for(Tree<Rule> t : map.keySet()){
            /**
            System.out.println("--------");
            System.out.println(map.getInt(t) / (double) samp.size());
            System.out.println(exp.getDouble(t) / sum);
            */
        }
        
        System.out.println(map.size());
        //TODO
    }   
}