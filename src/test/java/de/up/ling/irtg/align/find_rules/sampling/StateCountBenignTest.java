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
import de.up.ling.tree.Tree;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class StateCountBenignTest {
    
    /**
     * 
     */
    private TreeAutomaton<Span> base;
    
    /**
     * 
     */
    private TreeAutomaton<Span> score;
    
    /**
     * 
     */
    private Configuration config;
    
    /**
     * 
     */
    private StateCountBenign acb;
    
    @Before
    public void setUp() {
        StringAlgebra sag = new StringAlgebra();
        List<String> l = sag.parseString("1 2 3 4 5 6 7 8 9 10");
        
        base = sag.decompose(l);
        
        score = sag.decompose(l);
        assertFalse(score == base);
        
        Iterable<Rule> it = score.getAllRulesTopDown();
        for(Rule r : it){
            if(r.getArity() != 2){
                continue;
            }
            
            Span left = this.score.getStateForId(r.getChildren()[0]);
            
            if((left.end-left.start) == 3){
                r.setWeight(5.0);
                continue;
            }
            
            r.setWeight(0.01);
        }
        
        config = new Configuration();
        config.label2TargetLabel = (Function<Rule,Integer>) (Rule input) -> input.getLabel();
        config.rounds = 5;
        config.target = score;
        config.sampleSize = (int value) -> 2000;
        
        this.acb = new StateCountBenign(0.1,928892349279L);
        this.acb.setAutomaton(base);
    }

    @Test
    public void testGetSample() {
        List<Tree<Rule>> result = this.acb.getSample(config);
        double sum = 0.0;
        
        for(Tree<Rule> t : result){
            sum += (countLefties(t));
        }
        
        assertTrue(sum / result.size() >= 2.4);
        System.out.println(sum / result.size());
        
        sum = 0.0;
        this.acb.clear();
        config.rounds = 1;
        result = this.acb.getSample(config);
        
        for(Tree<Rule> t : result){
            sum += (countLefties(t));
        }
        
        assertTrue(sum / result.size() >= 2.0);
        System.out.println(sum / result.size());
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
            result += (left.end - left.start == 3) ? 1.0 : 0.0;
        }
        
        for(Tree<Rule> q : t.getChildren()){
            result += countLefties(q);
        }
        
        return result;
    }   
}