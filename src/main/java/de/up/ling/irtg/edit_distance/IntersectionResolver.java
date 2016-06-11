/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.edit_distance;

import com.google.common.base.Function;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.EditDistanceState;

/**
 *
 * @author teichmann
 */
public class IntersectionResolver implements Function<Rule,Pair<EditDistanceState,String>> {

    /**
     * 
     */
    private final TreeAutomaton<Pair<? extends Object, ? extends EditDistanceState>> basis; 

    /**
     * 
     * @param basis 
     */
    public IntersectionResolver(TreeAutomaton<Pair<? extends Object, ? extends EditDistanceState>> basis) {
        this.basis = basis;
    }
    
    
    
    
    @Override
    public Pair<EditDistanceState, String> apply(Rule f) {
        EditDistanceState eds = basis.getStateForId(f.getParent()).getRight();
        String label = basis.getSignature().resolveSymbolId(f.getLabel());
        
        return new Pair<>(eds,label);
    }
}
