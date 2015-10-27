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
import de.up.ling.irtg.align.find_rules.sampling.InterpretingModel;
import de.up.ling.irtg.align.find_rules.sampling.Model;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntDoubleFunction;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.irtg.util.MutableDouble;
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
    private CreateCorpus<List<String>, List<String>> cc;
    
    /**
     * 
     */
    private List<LearningInstance> semle;
    
    /**
     * 
     */
    private List<TreeAutomaton> data;
    
    /**
     * 
     */
    private SampledEM sem;
    
    @Before
    public void setUp() throws ParserException {
        StringAlgebra sal = new StringAlgebra();
        StringAlgebra sap = new StringAlgebra();
        //MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        ArrayList<CreateCorpus.InputPackage> stringInputs = new ArrayList<>();
        ArrayList<CreateCorpus.InputPackage> treeInput = new ArrayList<>();
        
        cc = new CreateCorpus<>(sal,sap);
        
        Propagator propDef = new Propagator();
        
        SampledEM.SampledEMFactory fact = new SampledEM.SampledEMFactory();
        fact.setBatchSize(2);
        fact.setNumberOfThreads(2).setScaling(1);
        
        
        Function<List<String>,Propagator> funct = (List<String> in) -> {
            return propDef;
        };
        
        SpanAligner.Factory spanFactory = new SpanAligner.Factory();
        
        for(String[] p : new String[][] {{"Resumption of the session","0:1:0 2:3:1 3:4:2"},
            {"( The House rose and observed a minute ' s silence )","0:1:0 1:2:1 2:3:2 3:4:3 5:6:4 7:8:5 7:8:6 10:11:7 11:12:8 11:12:9"}}){
            stringInputs.add(cc.makePackage(p[0], p[1], funct, spanFactory));     
        }
        
        /*
        Function<Tree<String>,Propagator> f = (Tree<String> in) -> {
            return propDef;
        };*/
        
        //AddressAligner.Factory addFactory = new AddressAligner.Factory();
        
        for(String[] p : new String[][] {{"Wiederaufnahme der Sitzungsperiode","0:1:0 1:2:1 2:3:2"},
            {"( Das Parlament erhebt sich zu einer Schweigeminute . )","0:1:0 1:2:1 2:3:2 3:4:3 4:5:4 5:6:5 6:7:6 7:8:7 8:9:8 9:10:9"}}){
            treeInput.add(cc.makePackage(p[0],p[1],funct,spanFactory));
        }
        
        data = cc.makeDataSet(stringInputs, treeInput);
        
        this.sem = fact.getInstance();
        
        LearningInstance.LearningInstanceFactory fac = new LearningInstance.LearningInstanceFactory();
        
        this.semle = fac.makeInstances(5, 500, data);
    }

    /**
     * Test of makeGrammar method, of class SampledEM.
     * @throws java.lang.Exception
     */
    @Test
    public void testMakeGrammar() throws Exception {
        InterpretingModel im = 
                new InterpretingModel(cc.getMainManager(), 1.0, Math.log(1E-2), Math.log(1E-2));
        Model mod = new Model() {

            @Override
            public double getLogWeight(Tree<Rule> t) {
                double d = im.getLogWeight(t);
                
                MutableDouble md = new MutableDouble(0.0);
                
                extend(t,md);
                
                return md.getValue()+d;
            }

            @Override
            public void add(Tree<Rule> t, double amount) {
                im.add(t, amount);
            }

            /**
             * 
             * @param t
             * @param md 
             */
            private void extend(Tree<Rule> t, MutableDouble md) {
                if(cc.getMainManager().isVariable(t.getLabel().getLabel())){
                    md.add(Math.log(1E-40));
                }
                
                t.getChildren().stream().forEach((q) -> {
                    extend(q,md);
                });
            }
        };
        
        
        List<List<Tree<Rule>>> result = sem.makeGrammar(cc, 1, semle, mod);
        
        com.google.common.base.Function<Rule,String> f = (Rule r) -> {
            return cc.getMainManager().getSignature().resolveSymbolId(r.getLabel());
        };
        
        TreeAutomaton ta = data.get(0);
        Iterable<Rule> it = ta.getAllRulesTopDown();
        
        for(Rule r : it){
            if(cc.getMainManager().isVariable(r.getLabel())){
                r.setWeight(20.0);
            }else{
                r.setWeight(0.1);
            }
        }
        
        Tree<String> rules = ta.viterbi();
        
        //for(Tree<Rule> t : result.get(0)){
        {
            System.out.println("----------------");
            //Tree<String> rules = t.map(f);
            Tree<String> left = cc.getMainManager().getHomomorphism1().apply(rules);
            Tree<String> right = cc.getMainManager().getHomomorphism2().apply(rules);
        
            System.out.println(left);
            System.out.println(right);
        }
        
        
        
        /**
        System.out.println("----------");
        rules = result.get(1).get(0).map((com.google.common.base.Function<Rule, String>) f);
        left = cc.getMainManager().getHomomorphism1().apply(rules);
        right = cc.getMainManager().getHomomorphism2().apply(rules);
        
        System.out.println(left);
        System.out.println(right);
        */
    }

    /**
     * Test of makeInstances method, of class SampledEM.
     */
    @Test
    public void testMakeInstances() {
        LearningInstance.LearningInstanceFactory fac = new LearningInstance.LearningInstanceFactory();
        
        this.semle = fac.makeInstances(5, 100, data);
        assertEquals(semle.size(),2);
    }
}
