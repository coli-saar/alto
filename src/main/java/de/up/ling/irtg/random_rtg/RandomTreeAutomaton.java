/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.random_rtg;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.distribution.GammaDistribution;

/**
 *
 * @author teichmann
 */
public class RandomTreeAutomaton {

    /**
     *
     */
    private final GammaDistribution rg;

    /**
     * 
     * @param seed
     * @param alpha 
     */
    public RandomTreeAutomaton(long seed, double alpha) {
        this.rg = new GammaDistribution(alpha, 1.0);
        this.rg.reseedRandomGenerator(seed);
    }

    /**
     * 
     * @param n
     * @return 
     */
    public TreeAutomaton<Integer> getRandomAutomaton(int n) {
        StringAlgebra sg = new StringAlgebra();
        
        List<String> ls = new ArrayList<>();
        for(int i=0;i<n;++i) {
            ls.add(Integer.toString(i));
        }
        
        TreeAutomaton ta = sg.decompose(ls);
        for(Rule r : (Iterable<Rule>) ta.getRuleSet()) {
            r.setWeight(this.makeWeight());
        }
        
        ta.normalizeRuleWeights();
        return ta;
    }

    /**
     * 
     * @param annealing
     * @return 
     */
    private double makeWeight() {
        double d = this.rg.sample()+Double.MIN_VALUE;
        return d;
    }
}
