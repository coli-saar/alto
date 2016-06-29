/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.edit_distance;

import com.google.common.base.Function;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import static de.up.ling.irtg.edit_distance.EditDistanceTreeAutomaton.Status.KEPT;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import java.util.function.Predicate;

/**
 * Works for the binary StringAlgebra.
 *
 * @author teichmann
 */
public class EditDistanceTreeAutomaton extends ConcreteTreeAutomaton<EditDistanceTreeAutomaton.EditDistanceState> {
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
     */
    private final Tree<String> baseTree;
    
    /**
     *
     * @param signature
     * @param inputSentence
     */
    public EditDistanceTreeAutomaton(Signature signature, Tree<String> inputSentence) {
        this(signature, inputSentence, (int pos) -> -1, (int pos) -> -1, (int pos) -> -1);
    }

    /**
     *
     * @param signature
     * @param t
     * @param delete
     * @param insert
     * @param substitute
     */
    public EditDistanceTreeAutomaton(Signature signature, Tree<String> t,
            final IntToDoubleFunction delete,
            IntToDoubleFunction insert, IntToDoubleFunction substitute) {
        super(signature);
        this.delete = (int i) -> Math.exp(delete.applyAsDouble(i));
        this.insert = (int i) -> Math.exp(insert.applyAsDouble(i));
        this.substitute = (int i) -> Math.exp(substitute.applyAsDouble(i));
        this.baseTree = t;
        this.inputSentence = t.getLeafLabels();

        if (inputSentence.size() < 1) {
            throw new IllegalArgumentException("This class expects at least one word as input");
        }

        int state = this.addState(new EditDistanceState(0, this.inputSentence.size()));
        this.addFinalState(state);
        
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
                        this.addRules(lab, this.addState(new EditDistanceState(leftStart, leftEnd)), this.addState(new EditDistanceState(rightStart, rightEnd)));
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

                double weight = s.equals(this.getSignature().resolveSymbolId(labelId)) ? 1 : this.substitute.applyAsDouble(i);

                Rule r = this.createRule(state, labelId, childStates, weight);
                addRule(r);

                double deleteCost = this.computeExternalDelete(eds);

                EditDistanceState fin = new EditDistanceState(0, this.inputSentence.size());
                state = this.getIdForState(fin);
                deleteCost *= weight;

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
                deleteCost *= weight;

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
            deleteCost *= weight;

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

            r = this.createRule(state, labelId, childStates, externalDelete * internalDelete);
            this.addRule(r);
        }
    }

    /**
     *
     * @param eds
     * @return
     */
    private double computeExternalDelete(EditDistanceState eds) {
        double value = 1.0;
        for (int i = eds.readSpanEnd; i < this.inputSentence.size(); ++i) {
            value *= this.delete.applyAsDouble(i);
        }

        for (int i = 0; i < eds.readSpanStart; ++i) {
            value *= this.delete.applyAsDouble(i);
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
        double value = 1.0;
        for (int i = leftEnd; i < rightStart; ++i) {
            value *= this.delete.applyAsDouble(i);
        }

        return value;
    }

    /**
     *
     * @param derivation
     * @return
     */
    public Status[] computeStatus(Tree<Rule> derivation) {
        Function<Rule,Pair<EditDistanceState,String>> mapping = (Rule r ) -> {
            
            return new Pair<>(this.getStateForId(r.getParent()),this.getSignature().resolveSymbolId(r.getLabel()));
        };
        Tree<Pair<EditDistanceState,String>> mapped = derivation.map(mapping);
        
        
        return computeStatusGeneral(mapped);
    }

    /**
     * 
     * @param mapped
     * @return 
     */
    public Status[] computeStatusGeneral(Tree<Pair<EditDistanceState, String>> mapped) {
        Status[] result = new Status[this.inputSentence.size()];
        Arrays.fill(result, Status.DELETED);
        
        addEntries(mapped, result);

        return result;
    }

    /**
     * 
     */
    private final Function<Pair<EditDistanceState, String>,String> stringify =
                            (Pair<EditDistanceState, String> p) -> p.getRight();
    
    /**
     * 
     * @param mapped
     * @return 
     */
    public Set<Tree<String>> selectCoveringSetForFalse(Tree<Pair<EditDistanceState, String>> mapped) {
        return extract(mapped);
    }
    
    /**
     * 
     * @param errors
     * @return 
     */
    public Set<Tree<String>> selectCoveringTreeForCorrect(Status[] errors) {
        return extract(errors, 0, this.baseTree);
    }
    
    /**
     * 
     * @param mapped
     * @param suitable
     * @return 
     */
    private Set<Tree<String>> extract(Tree<Pair<EditDistanceState, String>> mapped) {
        Set<Tree<String>> seen = null;
        
        if(mapped.getChildren().isEmpty()) {
            if(this.isEdited(mapped)) {
                Set<Tree<String>> ret = new HashSet<>();
                Tree<String> t = mapped.map(stringify);
                ret.add(t);
                
                return ret;
            } else {
                return null;
            }
        } else {
            for(Tree<Pair<EditDistanceState, String>> ts : mapped.getChildren()) {
                Set<Tree<String>> other = this.extract(ts);
                
                if(other != null && seen != null) {
                    Set<Tree<String>> ret = new HashSet<>();
                    Tree<String> t = mapped.map(stringify);
                    ret.add(t);
                
                    return ret;
                } else  if(seen == null){
                    seen = other;
                }
            }
            
            if(seen != null) {
                Tree<String> t = mapped.map(stringify);
                seen.add(t);
                
                return seen;
            } else {
                return null;
            }
        }
    }
    
    /**
     *
     * @param derivation
     * @param result
     */
    private void addEntries(Tree<Pair<EditDistanceState,String>> derivation, Status[] result) {
        if (derivation.getChildren().isEmpty()) {
            EditDistanceState eds = derivation.getLabel().getLeft();

            if (eds.distance() == 1) {
                String label = derivation.getLabel().getRight();

                int pos = eds.readSpanStart;
                result[pos] = this.inputSentence.get(pos).equals(label) ? Status.KEPT : Status.SUBSTITUTED;
            }
        } else {
            List<Tree<Pair<EditDistanceState, String>>> children = derivation.getChildren();

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
     * @param derivation
     * @return 
     */
    private boolean isEdited(Tree<Pair<EditDistanceState,String>> derivation) {
        if (derivation.getChildren().isEmpty()) {
            EditDistanceState eds = derivation.getLabel().getLeft();

            if (eds.distance() == 1) {
                String label = derivation.getLabel().getRight();

                int pos = eds.readSpanStart;
                return !this.inputSentence.get(pos).equals(label);
            } else {
                return true;
            }
        } else {
            throw new IllegalArgumentException("This method only tests leafs");
        }
    }

    /**
     * 
     * @param errors
     * @param suitable
     * @param i
     * @param subtree
     * @return 
     */    
    private Set<Tree<String>> extract(Status[] errors, final int i, Tree<String> subtree) {
        Set<Tree<String>> seen = null;
        
        if(subtree.getChildren().isEmpty()) {
            if(this.isEdited(errors,i)) {
                Set<Tree<String>> ret = new HashSet<>();
                ret.add(subtree);
                
                return ret;
            } else {
                return null;
            }
        } else {
            int index = i;
            
            for(Tree<String> ts : subtree.getChildren()) {
                Set<Tree<String>> other = this.extract(errors, index, ts);
                
                index += ts.getLeafLabels().size();
                
                if(other != null && seen != null) {
                    Set<Tree<String>> ret = new HashSet<>();
                    ret.add(subtree);
                
                    return ret;
                } else if(seen == null) {
                    seen = other;
                }
            }
            
            if(seen != null) {
                seen.add(subtree);
                
                return seen;
            } else {
                return null;
            }
        }
    }

    /**
     * 
     * @param errors
     * @param i
     * @return 
     */
    private boolean isEdited(Status[] errors, int i) {
        return errors[i] != KEPT;
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
