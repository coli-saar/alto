/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.ConcreteCondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author christoph_teichmann
 */
public class RestrictionManager {
 
    /**
     * Ensures that there is no sequence of variable nodes and that we never start with a variable.
     */
    private final ConcreteTreeAutomaton<Boolean> variableSequenceing;
    
    /**
     * Makes sure that sequence of symbols that are only productive on one side are ordered
     * to have first the left productive and then the right productive ones.
     */
    private final ConcreteTreeAutomaton<Boolean> ordering;
    
    /**
     * Ensures that once we have produced a constant on one side, the only symbol
     * we accept, that is productive on that side is the terminator.
     */
    private final ConcreteTreeAutomaton<Pair<Boolean,Boolean>> termination;
    
    /**
     * ensures that whenever we see a multi-way split symbol, then there are at least
     * two children that have a variable
     */
    private final ConcreteTreeAutomaton<Boolean> splitCheck1;
    
    /**
     * ensures that whenever we have a multi-way split, then all the children that are
     * not optimally paired have a variable.
     */
    private final ConcreteTreeAutomaton<Boolean> splitCheck2;
    
    /**
     * 
     */
    private CondensedTreeAutomaton fullRestriction = null;
    
    
    /**
     * 
     */
    public RestrictionManager(){
        this.ordering = new ConcreteTreeAutomaton<>();
        this.splitCheck1 = new ConcreteTreeAutomaton<>();
        this.splitCheck2 = new ConcreteTreeAutomaton<>();
        this.termination = new ConcreteTreeAutomaton<>();
        this.variableSequenceing = new ConcreteTreeAutomaton<>();
    }
    
    /**
     * 
     * @return 
     */
    public TreeAutomaton getRestriction(){
        if(fullRestriction != null){
            return fullRestriction;
        }else{
            TreeAutomaton cta = this.variableSequenceing.intersect(ordering)
                    .intersect(this.termination).intersect(this.splitCheck1)
                    .intersect(this.splitCheck2);
            return this.fullRestriction = ConcreteCondensedTreeAutomaton.fromTreeAutomaton(cta);
        }
    }
}
