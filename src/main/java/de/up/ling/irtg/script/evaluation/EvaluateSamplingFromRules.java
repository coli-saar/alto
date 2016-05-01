/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph
 */
public class EvaluateSamplingFromRules {
    
    
    public static double[][][] makeSmoothedKL(Iterable<TreeAutomaton> auts, int repetitions,
            RulePicker rp) {
        //TODO
        //TODO add tracking of rules used
        //TODO keep track of seed
        
        return null;
    }
    
    
    public interface RulePicker {
        /**
         * 
         * @param ta
         * @return 
         */
        public Rule[] pick(TreeAutomaton ta);
    }
}
