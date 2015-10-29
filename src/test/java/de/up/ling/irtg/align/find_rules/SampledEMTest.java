/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.align.creation.CreateCorpus;
import de.up.ling.irtg.align.find_rules.sampling.InterpretingModel;
import de.up.ling.irtg.align.find_rules.sampling.Model;
import de.up.ling.irtg.align.find_rules.sampling.SplitIndicatedImage;
import de.up.ling.irtg.align.pruning.StringLeftOrRight;
import de.up.ling.irtg.align.pruning.StringLeftXORRight;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
        
        final Propagator propDef = new Propagator();
        
        SampledEM.SampledEMFactory fact = new SampledEM.SampledEMFactory();
        fact.setBatchSize(1);
        fact.setNumberOfThreads(2).setScaling(30);
        
        
        Function<List<String>,Propagator> funct = (List<String> in) -> {
            return propDef;
        };
        
        SpanAligner.Factory spanFactory = new SpanAligner.Factory();
        
        for(String[] p : new String[][] {{"Jede Nacht geht John Hering angeln",
            "0:1:0 1:2:1 2:3:3 3:4:4 4:5:5 5:6:6 5:6:7"},
            {"Täglich geht Mary Lachs angeln",
                "0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 4:5:6"}}){
            stringInputs.add(cc.makePackage(p[0], p[1], funct, spanFactory));     
        }
        
        
        Propagator prop = new Propagator(new StringLeftOrRight());
        Function<List<String>, Propagator> f = (List<String> in) -> {
            return prop;
        };
        //AddressAligner.Factory addFactory = new AddressAligner.Factory();
        
        
        for(String[] p : new String[][] {{"Nightly John goes fishing for herring",
            "0:1:0 0:1:1 1:2:4 2:3:3 3:4:6 4:5:7 5:6:5"},
            {"Daily Mary goes fishing for salmon",
                "0:1:1 1:2:3 2:3:2 3:4:5 4:5:6 5:6:4"}}){
            treeInput.add(cc.makePackage(p[0],p[1],f,spanFactory));
        }
        
        data = cc.makeDataSet(stringInputs, treeInput);
        
        this.sem = fact.getInstance();
        
        LearningInstance.LearningInstanceFactory fac = new LearningInstance.LearningInstanceFactory();
        fac.setSamplerAdaptionRounds(5);
        fac.setAdaptionSmooth(1.0);
        
        this.semle = fac.makeInstances(20, 2000, data);
        
        IntSet iset = new IntAVLTreeSet(data.get(0).getAllLabels());
        iset.retainAll(data.get(1).getAllLabels());
        System.out.println(iset.size());
        
        
        
    }

    /**
     * Test of makeGrammar method, of class SampledEM.
     * @throws java.lang.Exception
     */
    @Test
    public void testMakeGrammar() throws Exception {
        /*
        InterpretingModel im = 
                new InterpretingModel(cc.getMainManager(), 0.1, Math.log(1E-2), Math.log(1E-2));
        Model mod = new Model() {
            
            private double d = 1E12;
            
            @Override
            public double getLogWeight(Tree<Rule> t) {
                double q = im.getLogWeight(t);
                
                MutableDouble md = new MutableDouble(0.0);
                
                extend(t,md);
                
                return md.getValue()+q;
            }

            @Override
            public void add(Tree<Rule> t, double amount) {
                im.add(t, amount);
                //this.d = Math.max(1.0, d / 10.0);
            }

            
            private void extend(Tree<Rule> t, MutableDouble md) {
                if(cc.getMainManager().isVariable(t.getLabel().getLabel())){
                    md.add(Math.log(d));
                }else{
                    //md.add(Math.log(1E-50));
                }
                
                t.getChildren().stream().forEach((q) -> {
                    extend(q,md);
                });
            }
        };
        
        List<List<Tree<Rule>>> result = sem.makeTrees(1, semle, mod);
        SplitIndicatedImage sii = new SplitIndicatedImage(cc.getMainManager(), "----------");
        
        //Tree<String> rules = ta.viterbi();
        
        for(int i=0;i<2;++i)
        {
            TreeAutomaton ta = this.data.get(i);
            System.out.println(ta.language().size());
            String s = sii.convert(result.get(i));
        
            System.out.println(s);
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
