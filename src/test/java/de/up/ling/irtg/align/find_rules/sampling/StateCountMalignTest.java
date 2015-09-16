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
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.IntIntFunction;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class StateCountMalignTest {
    
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
    
    /**
     * 
     */
    private TreeAutomaton first;
    
    @Before
    public void setUp() {
        alg1 = new StringAlgebra();
        alg2 = new StringAlgebra();
        
        TreeAutomaton t1 = first = alg1.decompose(alg1.parseString("Example of the first sentence ."));
        TreeAutomaton t2 = alg2.decompose(alg2.parseString("first sentence . of the Example"));
        
        
        SpanAligner spa1 = new SpanAligner("0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 5:6:6", t1);
        SpanAligner spa2 = new SpanAligner("0:1:4 1:2:5 2:3:6 3:4:2 4:5:3 5:6:1", t2);
        
        
        Propagator prop = new Propagator();
        
        t1 = prop.convert(t1, spa1);
        t2 = prop.convert(t2, spa2);
        
        homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
        
        RuleFindingIntersectionAutomaton rfa = new RuleFindingIntersectionAutomaton(t1, t2, homa.getHomomorphism1(), homa.getHomomorphism2());
        sampAut = new TopDownIntersectionAutomaton(rfa, homa.getRestriction());
        
    }

    @Test
    public void testSomeMethod() {
        StateCountMalign scm = new StateCountMalign(new StateCountBenign(1.0), 0.1);
        SampleMalign.SamplingConfiguration cof = new SampleMalign.SamplingConfiguration();
        
        cof.outerPreSamplingRounds = 10;
        cof.outerSampleSize = 100;
        cof.rounds = 5;
        cof.label2TargetLabel = (Function<Rule,Integer>) (Rule input) -> {
            if(homa.isVariable(input.getLabel())){
                return homa.getDefaultVariable();
            }else{
                return input.getLabel();
            }
        };
        cof.sampleSize = (int value) -> 1000;
        
        VariableIndication vai =  (int label) -> homa.isVariable(label);
        
        
        TreeAutomaton ta;
        //TODO
    }   
}