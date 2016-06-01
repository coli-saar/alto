/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.edit_distance;

import com.google.common.collect.ImmutableList;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * Works for the binary StringAlgebra.
 *
 * @author teichmann
 */
public class EditDistanceTreeAutomaton extends ConcreteTreeAutomaton<EditDistanceTreeAutomaton.EditDistanceState> {

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
     */
    public EditDistanceTreeAutomaton(Signature signature, List<String> inputSentence) {
        this(signature, inputSentence, (int pos) -> -1, (int pos) -> -1, (int pos) -> -1);
    }

    /**
     *
     * @param signature
     * @param inputSentence
     * @param delete
     * @param insert
     * @param substitute
     */
    public EditDistanceTreeAutomaton(Signature signature, List<String> inputSentence,
            IntToDoubleFunction delete, IntToDoubleFunction insert, IntToDoubleFunction substitute) {
        super(signature);

        if (inputSentence.size() < 1) {
            throw new IllegalArgumentException("This class expects at least one word as input");
        }

        this.inputSentence = inputSentence;

        int state = this.addState(new EditDistanceState(0, this.inputSentence.size()));
        this.addFinalState(state);

        this.delete = delete;
        this.insert = insert;
        this.substitute = substitute;
        
        for(int sym=1;sym<=signature.getMaxSymbolId();++sym) {
            if(signature.getArity(sym)== 0) {
                this.addRules(sym);
            }
        }
        
        int lab = signature.addSymbol(StringAlgebra.CONCAT, 2);
        for(int leftStart=0;leftStart<=this.inputSentence.size();++leftStart) {
            for(int leftEnd=leftStart;leftEnd<=this.inputSentence.size();++leftEnd) {
                for(int rightStart=leftEnd;rightStart<=this.inputSentence.size();++rightStart) {
                    for(int rightEnd=rightStart;rightEnd<=this.inputSentence.size();++rightEnd) {
                        this.addRules(lab, this.getIdForState(new EditDistanceState(leftStart, leftEnd)), this.getIdForState(new EditDistanceState(rightStart, rightEnd)));
                    }
                }
            }
        }
    }

    /**
     * 
     * @param labelId
     * @param childStates 
     */
    private void addRules(int labelId, int... childStates) {
        if (childStates.length == 0) {
            // first we have to handle inserting
            for (int i = 0; i < this.inputSentence.size(); ++i) {
                //First we do substitution
                String s = this.inputSentence.get(i);

                EditDistanceState eds = new EditDistanceState(i, i + 1);
                int state = this.addState(eds);

                double weight = s.equals(this.getSignature().resolveSymbolId(labelId)) ? 0 : this.substitute.applyAsDouble(i);

                Rule r = this.createRule(state, labelId, childStates, weight);
                addRule(r);

                double deleteCost = this.computeExternalDelete(eds);

                EditDistanceState fin = new EditDistanceState(0, this.inputSentence.size());
                state = this.getIdForState(fin);
                deleteCost += weight;

                r = this.createRule(state, labelId, childStates, deleteCost);
                addRule(r);

                //Then we do insertions
                eds = new EditDistanceState(i, i);
                state = this.addState(eds);
                weight = this.insert.applyAsDouble(i);

                r = this.createRule(state, labelId, childStates, weight);
                addRule(r);

                deleteCost = this.computeExternalDelete(eds);
                state = this.getIdForState(fin);
                deleteCost += weight;

                r = this.createRule(state, labelId, childStates, deleteCost);
                addRule(r);
            }

                //WE ALSO NEED TO DO INSERTIONS AFTER THE END
            int max = this.inputSentence.size();
            EditDistanceState eds = new EditDistanceState(max, max);
            int state = this.addState(eds);
            double weight = this.insert.applyAsDouble(max);

            Rule r = this.createRule(state, labelId, childStates, weight);
            addRule(r);

            double deleteCost = this.computeExternalDelete(eds);
            EditDistanceState fin = new EditDistanceState(0, this.inputSentence.size());
            state = this.getIdForState(fin);
            deleteCost += weight;

            r = this.createRule(state, labelId, childStates, deleteCost);
            addRule(r);

        } else {
            // check that this is possible in the string algebra
            if (childStates.length != 2 || !this.getSignature().resolveSymbolId(labelId).equals(StringAlgebra.CONCAT)) {
                return;
            }

            EditDistanceState left = this.getStateForId(childStates[0]);
            EditDistanceState right = this.getStateForId(childStates[1]);

            // check that the states are properly ordered
            if (left.readSpanEnd > right.readSpanStart) {
                return;
            }

            // now compute how much we need to delete in order to combine the two spans
            double internalDelete = this.computeInternalDelete(left.readSpanEnd, right.readSpanStart);

            EditDistanceState combined = new EditDistanceState(left.readSpanStart, right.readSpanEnd);
            int state = this.addState(combined);

            Rule r = this.createRule(state, labelId, childStates, internalDelete);
            this.addRule(r);

            // we could also finish here, if we pay the price for deleting everything outside
            double externalDelete = this.computeExternalDelete(combined);

            EditDistanceState fin = new EditDistanceState(0, this.inputSentence.size());
            state = this.getIdForState(fin);

            r = this.createRule(state, labelId, childStates, externalDelete + internalDelete);
            this.addRule(r);
        }
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return true;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return false;
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
    private double computeExternalDelete(EditDistanceState eds) {
        double value = 0.0;
        for (int i = eds.readSpanEnd; i < this.inputSentence.size(); ++i) {
            value += this.delete.applyAsDouble(i);
        }

        for (int i = 0; i < eds.readSpanStart; ++i) {
            value += this.delete.applyAsDouble(i);
        }

        return value;
    }

    /**
     *
     * @param leftEnd
     * @param rightStart
     * @return
     */
    private double computeInternalDelete(int leftEnd, int rightStart) {
        double value = 0.0;
        for (int i = leftEnd; i < rightStart; ++i) {
            value += this.delete.applyAsDouble(i);
        }

        return value;
    }

    /**
     *
     * @param derivation
     * @return
     */
    public Status[] computeStatus(Tree<Rule> derivation) {
        Status[] result = new Status[this.inputSentence.size()];
        Arrays.fill(result, Status.DELETED);

        addEntries(derivation, result);

        return result;
    }

    /**
     *
     * @param derivation
     * @param result
     */
    private void addEntries(Tree<Rule> derivation, Status[] result) {
        if (derivation.getChildren().isEmpty()) {
            EditDistanceState eds = this.getStateForId(derivation.getLabel().getParent());

            if (eds.distance() == 1) {
                String label = this.getSignature().resolveSymbolId(derivation.getLabel().getLabel());

                int pos = eds.readSpanStart;
                result[pos] = this.inputSentence.get(pos).equals(label) ? Status.KEPT : Status.SUBSTITUTED;
            }
        } else {
            List<Tree<Rule>> children = derivation.getChildren();

            for (int i = 0; i < children.size(); ++i) {
                this.addEntries(children.get(i), result);
            }
        }
    }

    @Override
    public void addRule(Rule rule) {
        //We need to make sure that we only add rules to our storage if they do not already exist with higher weight
        Iterable<Rule> rs = this.getRulesBottomUp(rule.getLabel(), rule.getChildren());

        for (Rule r : rs) {
            if (r.getParent() == rule.getParent()) {
                r.setWeight(Math.max(r.getWeight(), rule.getWeight()));
                return;
            }
        }

        super.addRule(rule);
    }

    /**
     *
     */
    public enum Status {

        /**
         *
         */
        KEPT,
        /**
         *
         */
        DELETED,
        /**
         *
         */
        SUBSTITUTED;
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
            if (readSpanStart > readSpanEnd) {
                throw new IllegalStateException("Incorrectly ordered start and end point.");
            }

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

        private int distance() {
            return this.readSpanEnd - this.readSpanStart;
        }

        @Override
        public String toString() {
            return "<" + readSpanStart + "," + readSpanEnd + ">";
        }

        /**
         * 
         * @return 
         */
        public int getReadSpanStart() {
            return readSpanStart;
        }

        /**
         * 
         * @return 
         */
        public int getReadSpanEnd() {
            return readSpanEnd;
        }
    }
}
