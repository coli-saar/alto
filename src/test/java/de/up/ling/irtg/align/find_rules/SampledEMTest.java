/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.AddressAligner;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntDoubleFunction;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    /**
     * 
     */
    private SampledEM.LearningInstance semle;
    
    /**
     * 
     */
    private List<TreeAutomaton> data;
    
    /**
     * 
     */
    private IntIntFunction sampleSize;
    
    /**
     * 
     */
    private SampledEM sem;
    
    @Before
    public void setUp() throws ParserException {
        StringAlgebra sal = new StringAlgebra();
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        ArrayList<CreateCorpus.InputPackage> stringInputs = new ArrayList<>();
        ArrayList<CreateCorpus.InputPackage> treeInput = new ArrayList<>();
        
        cc = new CreateCorpus<>(sal,mta);
        
        Propagator propDef = new Propagator();
        Propagator propLarge = new Propagator();
        
        sem = new SampledEM(2, 0.05, 150, 4, 15);
        
        Function<List<String>,Propagator> funct = (List<String> in) -> {
            if(in.size() > 0){
                return propLarge;
            }
            
            return propDef;
        };
        
        SpanAligner.Factory spanFactory = new SpanAligner.Factory();
        
        for(String[] p : new String[][] {{"a b c","0:1:1 1:2:2 2:3:3"},{"a b d","0:1:1 1:2:2 2:3:3"}}){
            stringInputs.add(new CreateCorpus.InputPackage(p[0], p[1], funct, spanFactory));     
        }
        
        Function<Tree<String>,Propagator> f = (Tree<String> in) -> {
            return propDef;
        };
        
        AddressAligner.Factory addFactory = new AddressAligner.Factory();
        
        for(String[] p : new String[][] {{"c(b,a)","0-0-0-1:1 0-0-0-0:2 0-0-0:3"},
            {"d(b,a)","0-0-0-1:1 0-0-0-0:2 0-0-0:3"}}){
            treeInput.add(new CreateCorpus.InputPackage<>(p[0],p[1],f,addFactory));
        }
        
        data = cc.makeDataSet(stringInputs, treeInput);
        
        this.sampleSize = (int sampNum) ->{
            if(sampNum == 0){
                return 10;
            }
            
            return 20;
        };
    }

    /**
     * Test of makeGrammar method, of class SampledEM.
     */
    @Test
    public void testMakeGrammar() throws Exception {
        List<SampledEM.LearningInstance> insts = sem.makeInstances(500, 50, cc, data, 0.01, 9999999999L, 5);
        
        InterpretedTreeAutomaton ita = sem.makeGrammar(cc, 2, insts);
        
        TreeAutomaton ta = ita.getAutomaton();
        
        Tree<String> t = ta.viterbi();
        
        assertTrue(ita.getInterpretation("right").interpret(t) instanceof Tree);
        
        Map<String,String> input = new HashMap<>();
        input.put("left", "a b c");
        ta = ita.parse(input);
        
        t = ta.viterbi();
        
        assertEquals(ita.getInterpretation("left").interpret(t).toString(),"[a, b, c]");
        assertEquals(ita.getInterpretation("right").interpret(t).toString(),"c(b,a)");
        
        input.clear();
        input.put("right", "d(b,a)");
        
        
        ta = ita.parse(input);
        
        t = ta.viterbi();
        
        assertEquals(ita.getInterpretation("left").interpret(t).toString(),"[a, b, d]");
        assertEquals(ita.getInterpretation("right").interpret(t).toString(),"d(b,a)");
        System.out.println(t);
        //TODO
        
        
    }

    /**
     * Test of makeInstances method, of class SampledEM.
     */
    @Test
    public void testMakeInstances() {
        List<SampledEM.LearningInstance> insts = sem.makeInstances(10, 20, cc, data, 0.01, 9999999999L, 3);
        
        assertEquals(insts.size(),2);
    }

    /**
     * Test of makeLearningInstance method, of class SampledEM.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testMakeLearningInstance() throws ParserException, Exception {
        semle = sem.makeLearningInstance(this.data.get(0), 0.1, 9999999999L, sampleSize, 3, cc);
        
        VariableIndication vi = new VariableIndicationByLookUp(cc.getMainManager());
        IntDoubleFunction smooth = (int entry) -> {
            return 1.0;
        };
        
        semle.setModel(new TreeAddingAutomaton(cc.getMainManager().getSignature(), smooth, vi));
        
        assertEquals(semle.call().size(),10);
    }
    
}
