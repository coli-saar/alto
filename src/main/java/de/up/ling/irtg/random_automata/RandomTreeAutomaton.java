/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.random_automata;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.distribution.GammaDistribution;

/**
 * This class is used to create simple, random tree automata that are similar to of parse charts.
 * 
 * The rules of the parse chart are those of a decomposition automaton for a sequence of numbers
 * that is determined by the input. The probabilities for the rules are drawn from a Dirichlet distribution.
 * Note that we ensure that there a no 0 probabilities.
 * 
 * @author teichmann
 */
public class RandomTreeAutomaton {

    /**
     * The based for sampling the Dirichlet weights.
     * 
     * This is based on the fact that we can draw from a Dirichlet by drawing a vector of 
     * weights from a Gamma distribution and then normalizing them.
     */
    private final GammaDistribution rg;

    /**
     * Produces a new generator for random tree automata.
     * 
     * 
     * @param seed The random number seed that will be used.
     * @param alpha The concentration parameter used for the Dirichlet distributions for the rule probabilities.
     */
    public RandomTreeAutomaton(long seed, double alpha) {
        this.rg = new GammaDistribution(alpha, 1.0);
        this.rg.reseedRandomGenerator(seed);
    }

    /**
     * Returns a new random tree automaton that is based on a decomposition automaton for the sequence '0 1 ... n-1'
     * 
     * @param n How many leaf the generated trees should have.
     * @return A random tree automaton.
     */
    public TreeAutomaton<Integer> getRandomAutomaton(int n) {
        StringAlgebra sg = new StringAlgebra();
        
        List<String> ls = new ArrayList<>();
        for(int i=0;i<n;++i) {
            ls.add(Integer.toString(i));
        }
        
        TreeAutomaton ta = sg.decompose(ls);
        //we set the rule weight to draws from the gamma distribution with a small threshold added.
        for(Rule r : (Iterable<Rule>) ta.getRuleSet()) {
            r.setWeight(this.makeWeight());
        }
        
        //then normalize the rule weights so the results are distributed according to a Dirichlet distribution.
        ta.normalizeRuleWeights();
        return ta;
    }

    /**
     * Generates a weight for a rule by drawing from the gamma distribution and adding a minimum value to make sure it is non-zero.
     * 
     * @return 
     */
    private double makeWeight() {
        double d = this.rg.sample()+Double.MIN_VALUE;
        return d;
    }
}
