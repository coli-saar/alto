/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.*;
import com.google.common.base.Function;
import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A tree automaton that describes the homomorphic
 * pre-image of the language of another tree automaton.
 * This class only functions correctly if the homomorphism
 * is non-deleting.
 * 
 * This automaton has the same states as the base automaton,
 * converted into strings.
 * 
 * @author koller
 */
public class CondensedNondeletingInverseHomAutomaton<State> extends CondensedTreeAutomaton<Object> {
    private final boolean debug = false;

    private TreeAutomaton<State> rhsAutomaton;
    private Homomorphism hom;
//    private Map<String, State> rhsState;
    private int[] labelsRemap; // hom-target(id) = rhs-auto(labelsRemap[id])
    private Function<HomomorphismSymbol,Integer> remappingHomSymbolToIntFunction;   
    

    public CondensedNondeletingInverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
        isCondensedExplicit = false;
        
        labelsRemap = hom.getTargetSignature().remap(rhsAutomaton.getSignature());
        
        remappingHomSymbolToIntFunction = new Function<HomomorphismSymbol, Integer>() {
            public Integer apply(HomomorphismSymbol f) {
                return labelsRemap[HomomorphismSymbol.getHomSymbolToIntFunction().apply(f)];
            }
        };

        assert hom.isNonDeleting();

        this.stateInterner = rhsAutomaton.getStateInterner();
        finalStates.addAll(rhsAutomaton.getFinalStates());
      
    }
   
    /**
     * This class should be used with this method for greatest efficency (eg in IntersectionAutomaton).
     * Returns an Iterable over CondensedRules, that have the given parentSate.
     * Calculation happens on the fly, there is no caching involved, 
     * because usually this method is called only once for each parentState.
     * @param parentState
     * @return
     */
    @Override
    public Iterable<CondensedRule> getCondensedRulesByParentState(int parentState) {
        Set<CondensedRule> ret = new HashSet<CondensedRule>();
        IntSet processedLabelSets = new IntOpenHashSet();
//        System.err.println("Parent: " + getStateInterner().resolveId(parentState).toString());
        // Iterate over all labels for a parentState and add all calcualted rules to the return set.
         
        for(Integer label : getLabelsTopDown(parentState)) {
//            System.err.println("Label: " + getSignature().resolveSymbolId(label)+ "(" + label + ")");
            // First, use the homomorphism to get the ID of the LabelSet, 
            // to wich the current label belongs.
            int labelSetID = hom.getLabelSetID(label);
//            System.err.println("Parent State: " + parentState + " - Label: " + label + " ID " + labelSetID);
//            System.err.println("It belongs to the LabelSet: " + labelSetID);
//            System.err.println("Processed? " + processedLabelSets.contains(labelSetID));

            // Proceed only if we have not processed any other labels, that belong
            // to this labelset.
      
            if (!processedLabelSets.contains(labelSetID)) {
                processedLabelSets.add(labelSetID);
                Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(labelSetID);
                
//                System.err.println("RHS: " + hom.rhsAsString(rhs));

                // Find childstates
                for (List<Integer> substitutionTuple : grtdDfs(rhs, parentState, getRhsArity(rhs))) {
                    if (isCompleteSubstitutionTuple(substitutionTuple)) {
                        // TODO: weights
                        // Transform the labelSetID from the homomorphism to one of this automaton.
                        int newLabelSetID = this.getLabelSetID(hom.getLabelSetForLabel(label));
                        CondensedRule cr = new CondensedRule(parentState, newLabelSetID, intListToArray(substitutionTuple), 1); //createRuleRaw(parentState, newLabelSetID, intListToArray(substitutionTuple), 1);
                        ret.add(cr);
                    }
                }
            }
        }
        return ret;
    }

    public void makeAllRulesCondensedExplicit() {
        if (!isCondensedExplicit) {
            
//            System.err.println("This  Signature:   \n" + getSignature().toString());
//            System.err.println("Rhs  Signature:   \n" + rhsAutomaton.getSignature().toString());
//            System.err.println("Hom SRC Signature: \n" + hom.getSourceSignature().toString());
//            System.err.println("Hom TGT Signature: \n" + hom.getTargetSignature().toString());
//            
            isCondensedExplicit = true;

            for (int state : rhsAutomaton.getAllStates()) {
                for (CondensedRule cr : getCondensedRulesByParentState(state)) {
//                    System.err.println("storing " + cr.toString(this));
                    storeRule(cr);
                }
            }

        }
    }
    

    private boolean isCompleteSubstitutionTuple(List<Integer> tuple) {
        for (Integer s : tuple) {
            if (s == null) {
                return false;
            }
        }

        return true;
    }

    private int getRhsArity(Tree<HomomorphismSymbol> rhs) {
        int max = -1;

        for (HomomorphismSymbol sym : rhs.getLeafLabels()) {
            if (sym.isVariable() && (sym.getValue() > max)) {
                max = sym.getValue();
            }
        }

        return max + 1;
    }

    private Set<List<Integer>> grtdDfs(Tree<HomomorphismSymbol> rhs, int state, int rhsArity) {
        Set<List<Integer>> ret = new HashSet<List<Integer>>();

        switch (rhs.getLabel().getType()) {
            case CONSTANT:
                for (Rule rhsRule : rhsAutomaton.getRulesTopDown(labelsRemap[rhs.getLabel().getValue()], state)) {
                    List<Set<List<Integer>>> childrenSubstitutions = new ArrayList<Set<List<Integer>>>(); // len = #children

                    for (int i = 0; i < rhsRule.getArity(); i++) {
                        childrenSubstitutions.add(grtdDfs(rhs.getChildren().get(i), rhsRule.getChildren()[i], rhsArity));
                    }

                    CartesianIterator<List<Integer>> it = new CartesianIterator<List<Integer>>(childrenSubstitutions);
                    while (it.hasNext()) {
                        List<List<Integer>> tuples = it.next();  // len = # children x # variables
                        List<Integer> merged = mergeSubstitutions(tuples, rhsArity);
                        if (merged != null) {
                            ret.add(merged);
                        }
                    }
                }
                break;

            case VARIABLE:
                List<Integer> rret = new ArrayList<Integer>(rhsArity);
                int varnum = rhs.getLabel().getValue();

                for (int i = 0; i < rhsArity; i++) {
                    if (i == varnum) {
                        rret.add(state);
                    } else {
                        rret.add(null);
                    }
                }

                ret.add(rret);
        }

//        System.err.println(state + "/" + rhs + "  ==> " + ret);
        return ret;
    }

    private String childStatesToString(int[] childStates) {
        if (childStates.length == 0) {
            return "{}";
        }
        StringBuilder buf = new StringBuilder("{");
        for (int i = 0; i < childStates.length; i++) {
            buf.append(childStates[i]).append(",");
        }
        buf.setLength(buf.length() - 1);

        return buf.toString() + "}";
    }
    
    // tuples is an n-list of m-lists of output states, where
    // n is number of children, and m is number of variables in homomorphism
    // If n = 0, the method returns [null, ..., null]
    private List<Integer> mergeSubstitutions(List<List<Integer>> tuples, int rhsArity) {
        List<Integer> merged = new ArrayList<Integer>();  // one entry per variable

//        System.err.println("    merge: " + tuples);

        for (int i = 0; i < rhsArity; i++) {
            merged.add(null);
        }

        for (int i = 0; i < tuples.size(); i++) {
            for (int j = 0; j < rhsArity; j++) {
                Integer state = tuples.get(i).get(j);
                if (state != null) {
                    if (merged.get(j) != null && !merged.get(j).equals(state)) {
                        return null;
                    } else {
                        merged.set(j, state);
                    }
                }
            }
        }

//        System.err.println("    --> merged: " + merged);

        return merged;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return rhsAutomaton.isBottomUpDeterministic();
    }

    @Override
    public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates) {
        return getCondensedRuleBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState) {
        return getCondensedRulesTopDownFromExplicit(labelId, parentState);
    }
    
    
}
