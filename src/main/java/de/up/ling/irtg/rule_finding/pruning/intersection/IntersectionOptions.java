/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 */
public enum IntersectionOptions {
    LEXICALIZED {

        @Override
        public TreeAutomaton getAutomaton(TreeAutomaton input) {
            return new Lexicalized(input.getSignature(), input.getAllLabels());
        }
    },
    NO_EMPTY {

        @Override
        public TreeAutomaton getAutomaton(TreeAutomaton input) {
            return new NoEmpty(input.getSignature(), input.getAllLabels());
        }
    },
    RIGHT_BRANCHING_NORMAL_FORM {

        @Override
        public TreeAutomaton getAutomaton(TreeAutomaton input) {
            return new RightBranchingNormalForm(input.getSignature(), input.getAllLabels());
        }
    }
    ;
    
    
    public abstract TreeAutomaton getAutomaton(TreeAutomaton input);
}
