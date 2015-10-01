/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.AddressAligner;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.pruning.StringLeftXORRight;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SampledEMTest {
    
    /**
     * 
     */
    private CreateCorpus<List<String>, Tree<String>> cc;
    
    @Before
    public void setUp() {
    }

    /**
     * Test of makeGrammar method, of class SampledEM.
     */
    @Test
    public void testMakeGrammar() throws Exception {
        //TODO
        
        
    }

    /**
     * Test of makeInstances method, of class SampledEM.
     */
    @Test
    public void testMakeInstances() {
        //TODO
        
        
    }

    /**
     * Test of makeLearningInstance method, of class SampledEM.
     */
    @Test
    public void testMakeLearningInstance() throws ParserException, Exception {
        StringAlgebra sal = new StringAlgebra();
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        ArrayList stringInputs = new ArrayList<>();
        ArrayList treeInput = new ArrayList<>();
        
        cc = new CreateCorpus<>(sal,mta);
        
        Propagator propDef = new Propagator();
        Propagator propLarge = new Propagator(new StringLeftXORRight());
        
        SampledEM sem = new SampledEM(2, 0.5, 0.01, 6);
        
        Function<List<String>,Propagator> funct = (List<String> in) -> {
            if(in.size() > 0){
                return propLarge;
            }
            
            return propDef;
        };
        
        SpanAligner.Factory spanFactory = new SpanAligner.Factory();
        
        for(String[] p : new String[][] {{"a b c","0:1:1 1:2:2 2:3:3"}}){
            stringInputs.add(new CreateCorpus.InputPackage(p[0], p[1], funct, spanFactory));     
        }
        
        Function<Tree<String>,Propagator> f = (Tree<String> in) -> {
            return propDef;
        };
        
        AddressAligner.Factory addFactory = new AddressAligner.Factory();
        
        for(String[] p : new String[][] {{"c(b,a)","0-0-0-1:1 0-0-0-0:2 0-0-0:3"}}){
            treeInput.add(new CreateCorpus.InputPackage<>(p[0],p[1],f,addFactory));
        }
        
        List<TreeAutomaton> data = cc.makeDataSet(stringInputs, treeInput);
        
        IntIntFunction sampleSizes = (int sampNum) ->{
            if(sampNum == 0){
                return 10;
            }
            return 20;
        };
        
        SampledEM.LearningInstance semle = sem.makeLearningInstance(0.01, 99999L,
                sampleSizes, 15, cc);
        
        //TODO set a model
        System.out.println(semle.call());
        //TODO
    }
    
}
