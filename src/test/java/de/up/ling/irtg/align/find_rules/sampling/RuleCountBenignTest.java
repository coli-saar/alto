/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import com.google.common.base.Function;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.align.find_rules.sampling.SampleBenign.Configuration;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.IntIntFunction;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author christoph_teichmann
 */
public class RuleCountBenignTest {
    
    /**
     * 
     */
    private final static int TARGET_DISTANCE = 4;
    
    /**
     * 
     */
    private TreeAutomaton<Span> base;
    
    /**
     * 
     */
    private Model score;
    
    /**
     * 
     */
    private Configuration config;
    
    /**
     * 
     */
    private RuleCountBenign rcb;
    
    @Before
    public void setUp() {
        StringAlgebra sag = new StringAlgebra();
        List<String> l = sag.parseString("1 2 3 4 5 6 7 8 9 10");
        
        base = sag.decompose(l);
        
        score = new Model() {
            @Override
            public double getLogWeight(Tree<Rule> t) {
                MutableDouble md = new MutableDouble(0.0);
                
                getWidthAndWeight(md,t);
                
                return md.getValue();
            }

            @Override
            public void add(Tree<Rule> t, double amount) {
            }

            private int getWidthAndWeight(MutableDouble md, Tree<Rule> t) {
                if(t.getChildren().isEmpty()){
                    return 1;
                }else{
                    int sum = 0;
                    int length = 0;
                    
                    for(int i=0;i<t.getChildren().size();++i){
                        Tree<Rule> child = t.getChildren().get(i);
                        
                        int size = getWidthAndWeight(md, child);
                        if(i == 0){
                            length = size;
                        }
                        
                        sum += size;
                    }
                    
                    if(length == TARGET_DISTANCE){
                        md.add(0.0);
                    }else{
                        md.add(-5.0);
                    }
                    
                    return sum;
                }
            }
        };
        
        IntIntFunction sampleSize = (int value) -> 15;
        
        config = new Configuration(score);
        config.setRounds(12);
        config.setSampleSize(sampleSize);
        
        this.rcb = new RuleCountBenign(1.0, 87764L, null);
        this.rcb.setAutomaton(base);
    }

    @Test
    public void testGetSample() {
        List<Tree<Rule>> result = this.rcb.getSample(config);
        double sum = 0.0;
        
        for(Tree<Rule> t : result){
            sum += this.countLefties(t);
        }
        
        assertTrue(sum / result.size() >= 1.9);
        
        StringAlgebra sag = new StringAlgebra();
        List<String> l = sag.parseString("1 2 3 4");
        
        base = sag.decompose(l);
        
        config.setSampleSize((int i) -> 20);
        config.setRounds(5);
        this.rcb.setAutomaton(base);
        
        result = this.rcb.getSample(config);
        
        Function<Rule,String> f = (Rule r) -> {
            return base.getSignature().resolveSymbolId(r.getLabel());
        };
        
        Object2DoubleOpenHashMap<Tree<String>> map = new Object2DoubleOpenHashMap<>();
        
        for(Tree<Rule> t : result){
            Tree<String> q = t.map(f);
            
            map.addTo(q, 1.0);
        }
        
        assertEquals(map.size(),5);
        
        for(Tree<String> t : map.keySet()){
            assertEquals(map.get(t) / result.size(),0.2,0.2);
        }
    }

    /**
     * 
     * @param t
     * @return 
     */
    private double countLefties(Tree<Rule> t) {
        double result= 0.0;
        
        if(t.getLabel().getArity() == 2){
            Span left = this.base.getStateForId(t.getLabel().getChildren()[0]);
            result += (left.end - left.start == TARGET_DISTANCE) ? 1.0 : 0.0;
        }
        
        for(Tree<Rule> q : t.getChildren()){
            result += countLefties(q);
        }
        
        return result;
    }
}