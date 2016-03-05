/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public enum IntersectionOptions {
    LEXICALIZED {
        @Override
        public Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String configuration) {
            return (TreeAutomaton t) -> new Lexicalized(t.getSignature(), t.getAllLabels());
        }
        
    },
    NO_EMPTY {

        @Override
        public Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String configuration) {
            return (TreeAutomaton t) -> new NoEmpty(t.getSignature(), t.getAllLabels());
        }
        
    },
    RIGHT_BRANCHING_NORMAL_FORM {
        @Override
        public Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String configuration) {
            return (TreeAutomaton t) -> new RightBranchingNormalForm(t.getSignature(), t.getAllLabels());
        }
    },
    NO_LEFT_INTO_RIGHT {
        @Override
        public Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String configuration) {
            return (TreeAutomaton t) -> new NoLeftIntoRight(t.getSignature(), t.getAllLabels());
        }
    },
    ENSURE_ARITIES {
        @Override
        public Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String configuration) {
            return TreeArityEnsure.getRestrictionFactory(configuration);
        }
        
    },
    NO_PRE_CONSTANT_CUT {
        @Override
        public Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String configuration) {
            return (TreeAutomaton t) -> new NoPreConstantCut(t.getSignature(), t.getAllLabels());
        }
    };
    
    /**
     * 
     * @param configuration
     * @return 
     */
    public abstract Function<TreeAutomaton,TreeAutomaton> getRestrictionFactory(String configuration);
    
    /**
     * 
     * @param input
     * @return 
     */
    public static IntersectionOptions[] lookUp(String[] input) {
        IntersectionOptions[] result = new IntersectionOptions[input.length];
        
        for(int i=0;i<input.length;++i) {
            result[i] = valueOf(input[i]);
        }
        
        return result;
    }
}
