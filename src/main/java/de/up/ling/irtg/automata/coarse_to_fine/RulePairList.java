/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;

/**
 *
 * @author koller
 */
public class RulePairList {
    private List<RuleRefinementNode> grammarNodes;
    private IntList invhomStates;

    public RulePairList(List<RuleRefinementNode> grammarNodes, IntList invhomStates) {
        this.grammarNodes = grammarNodes;
        this.invhomStates = invhomStates;
    }

    public List<RuleRefinementNode> getGrammarNodes() {
        return grammarNodes;
    }

    public IntList getInvhomStates() {
        return invhomStates;
    }
    
    
}
