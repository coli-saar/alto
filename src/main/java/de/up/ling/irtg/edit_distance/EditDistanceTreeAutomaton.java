/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.edit_distance;

import com.google.common.collect.ImmutableList;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * Works for the binary StringAlgebra.
 * 
 * @author teichmann
 */
public class EditDistanceTreeAutomaton extends TreeAutomaton<EditDistanceTreeAutomaton.EditDistanceState> {
    /**
     * 
     */
    private static final Iterable<Rule> EMPTY = ImmutableList.of();
    
    /**
     *
     */
    private final List<String> inputSentence;

    /**
     *
     */
    private final IntToDoubleFunction substitute;

    /**
     *
     */
    private final IntToDoubleFunction insert;

    /**
     *
     */
    private final IntToDoubleFunction delete;
    

    /**
     *
     * @param signature
     * @param inputSentence
     * @param delete
     * @param insert
     * @param substitute
     */
    public EditDistanceTreeAutomaton(Signature signature, List<String> inputSentence,
            IntToDoubleFunction delete, IntToDoubleFunction insert,
            IntToDoubleFunction substitute) {
        super(signature);

        this.inputSentence = inputSentence;

        int state = this.addState(new EditDistanceState(0, this.inputSentence.size()));
        this.addFinalState(state);
        
        this.delete = delete;
        this.insert = insert;
        this.substitute = substitute;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        if (!this.useCachedRuleBottomUp(labelId, childStates)) {
            //TODO
            if (childStates.length == 0) {
                // first we have to handle inserting
                for(int i=0;i<this.inputSentence.size();++i) {
                    //First we do substitution
                    String s = this.inputSentence.get(i);
                    
                    EditDistanceState eds = new EditDistanceState(i, i+1);
                    int state = this.addState(eds);
                    
                    double weight = s.equals(this.getSignature().resolveSymbolId(labelId)) ? 0 : this.substitute.applyAsDouble(i);
                    
                    Rule r = this.createRule(state, labelId, childStates, weight);
                    storeRuleBottomUp(r);
                    
                    double deleteCost = this.makeDeleteCosts(eds);
                    eds = new EditDistanceState(0, this.inputSentence.size());
                    state = this.addState(eds);
                    deleteCost += weight;
                    
                    r = this.createRule(state, labelId, childStates, deleteCost);
                    storeRuleBottomUp(r);
                    
                    //Then we do insertions
                    eds = new EditDistanceState(i, i);
                    state = this.addState(eds);
                    weight = this.insert.applyAsDouble(i);
                    
                    r = this.createRule(state, labelId, childStates, weight);
                    storeRuleBottomUp(r);
                    
                    deleteCost = this.makeDeleteCosts(eds);
                    eds = new EditDistanceState(0, this.inputSentence.size());
                    state = this.addState(eds);
                    deleteCost += weight;
                    
                    r = this.createRule(state, labelId, childStates, deleteCost);
                    storeRuleBottomUp(r);
                }
            } else {
                // TODO
                if(childStates.length != 2 || !this.getSignature().resolveSymbolId(labelId).equals(StringAlgebra.CONCAT)) {
                    return EMPTY;
                }
                
                EditDistanceState left = this.getStateForId(childStates[0]);
                EditDistanceState right = this.getStateForId(childStates[1]);
                
                if(left.readSpanEnd > right.readSpanStart) {
                    return EMPTY;
                }
                
                
                
                //TODO
            }
        }

        return this.getRulesBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {       
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }

    /**
     * 
     * @param eds
     * @return 
     */
    private double makeDeleteCosts(EditDistanceState eds) {
        double value = 0.0;
        for(int i=eds.readSpanEnd;i<this.inputSentence.size();++i) {
            value += this.delete.applyAsDouble(i);
        }
        
        for(int i=0;i<eds.readSpanStart;++i) {
            value+= this.delete.applyAsDouble(i);
        }
        
        return value;
    }

    /**
     * 
     */
    public class EditDistanceState {

        /**
         *
         */
        private final int readSpanStart;

        /**
         *
         */
        private final int readSpanEnd;

        /**
         *
         * @param readSpanStart
         * @param readSpanEnd
         * @param isFinished
         */
        private EditDistanceState(int readSpanStart, int readSpanEnd) {
            this.readSpanStart = readSpanStart;
            this.readSpanEnd = readSpanEnd;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + this.readSpanStart;
            hash = 37 * hash + this.readSpanEnd;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EditDistanceState other = (EditDistanceState) obj;
            if (this.readSpanStart != other.readSpanStart) {
                return false;
            }
            
            return this.readSpanEnd == other.readSpanEnd;
        }
    }
}
