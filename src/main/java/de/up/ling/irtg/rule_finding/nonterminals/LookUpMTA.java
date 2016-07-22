/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class LookUpMTA extends StateValueComputation {
    @Override
    protected String computeValue(Rule r, Map<String, String> labels, TreeAutomaton mtaAutomaton) {
        if(r.getArity() == 0) {
            return mtaAutomaton.getSignature().resolveSymbolId(r.getLabel());
        } else {
            String label = mtaAutomaton.getSignature().resolveSymbolId(r.getLabel());
            
            switch (label) {
                case MinimalTreeAlgebra.LEFT_INTO_RIGHT:
                {
                    String child = mtaAutomaton.getStateForId(r.getChildren()[1]).toString();
                    
                    return labels.get(child);
                }
                case MinimalTreeAlgebra.RIGHT_INTO_LEFT:
                {
                    String child = mtaAutomaton.getStateForId(r.getChildren()[0]).toString();
                    
                    return labels.get(child);
                }
                default:
                    return null;
            }
        }
    }    
}
