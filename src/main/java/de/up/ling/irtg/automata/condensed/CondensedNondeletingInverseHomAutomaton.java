/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.FunctionToInt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    private int[] labelsRemap; // hom-target(id) = rhs-auto(labelsRemap[id])
    private IntSet labelSetsWithVariables;  // stores all the other labelsets
    private IntSet validLabelSetIDs;
    private Int2ObjectMap<IntSet> stateToNewLabelSetIDs; // maps a parentstate that derives no children to the coresponding labelsetids

    

    public CondensedNondeletingInverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        assert hom.isNonDeleting();

        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
        isCondensedExplicit = false;
        
        labelsRemap = hom.getTargetSignature().remap(rhsAutomaton.getSignature());
        
        this.stateInterner = rhsAutomaton.getStateInterner();
        finalStates.addAll(rhsAutomaton.getFinalStates());
        
        FunctionToInt<Tree<HomomorphismSymbol>> RETURN_ZERO = new FunctionToInt<Tree<HomomorphismSymbol>>() {
            public int applyInt(Tree<HomomorphismSymbol> f) {
                return 0;
            }
        };
        
        // Get only the labelsetIDs that we actual need according to the given automaton.
        validLabelSetIDs = hom.getLabelsetIDsForTgtSymbols(rhsAutomaton.getAllLabels());
        stateToNewLabelSetIDs = new Int2ObjectOpenHashMap<IntSet>();
        // Seperate between labelsetIDs that stand for symbols, wich resolve into trees with an arity of 0 and others.
        for (int labelSetID : validLabelSetIDs) {
            Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(labelSetID);
          
            if (rhs.getMaximumArity() == 0) {                
                // iterate over all states that can be reached buttom-up from the the rhs.
                for (int state : rhsAutomaton.run(rhs, HomomorphismSymbol.getHomSymbolToIntFunction(), RETURN_ZERO)) {

                    // save the labelSetID in the signature of this automaton
//                    int newLabelSetID = this.addLabelSetID(hom.getLabelSetByLabelSetID(labelSetID)); // AK
                    int newLabelSetID = labelSetID;

                    if (stateToNewLabelSetIDs.containsKey(state)) {
                        stateToNewLabelSetIDs.get(state).add(newLabelSetID);
                    } else {
                        IntSet insert = new IntOpenHashSet();
                        insert.add(newLabelSetID);
                        stateToNewLabelSetIDs.put(state, insert);
                    }
                }
            }
        }
        // Create a set to iterate over in case the state does not derive to a terminal symbol
        labelSetsWithVariables = new IntOpenHashSet(validLabelSetIDs);
        labelSetsWithVariables.removeAll(stateToNewLabelSetIDs.keySet());   
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
//        System.err.println("Parent: " + getStateInterner().resolveId(parentState).toString());
        
        // Check if this state derives a span of the length 0
        IntSet newLabelSetIDs = stateToNewLabelSetIDs.get(parentState);
        if (newLabelSetIDs != null) {
            for (int newLabelSetID : newLabelSetIDs) {
                CondensedRule cr = new CondensedRule(parentState, newLabelSetID, new int[0], 1);
                ret.add(cr);
//                System.err.println("Creating Rule for arity0");
//                System.err.println(cr.toString(this));
            }
        } else {
            // Iterate over all labels for a parentState and add all calcualted rules to the return set.
            for (int labelSetID : labelSetsWithVariables) {
//                System.err.println("New labelSetID: " + labelSetID);          

                Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(labelSetID);

                // Find childstates
                for (List<Integer> substitutionTuple : grtdDfs(rhs, parentState, getRhsArity(rhs))) {
                    if (isCompleteSubstitutionTuple(substitutionTuple)) {
                        // TODO: weights
                        // Transform the labelSetID from the homomorphism to one of this automaton.
                        
//                        int newLabelSetID = this.addLabelSetID(hom.getLabelSetByLabelSetID(labelSetID)); // AK
                        int newLabelSetID = labelSetID;
                        CondensedRule cr = new CondensedRule(parentState, newLabelSetID, intListToArray(substitutionTuple), 1); //createRuleRaw(parentState, newLabelSetID, intListToArray(substitutionTuple), 1);
                        ret.add(cr);
                        
//                        System.err.println("Creating Rule for arity>0");
//                        System.err.println(cr.toString(this));
                    }
                }
            }
        }
        
        
        return ret;
    }
    
    // Returns the ID for a labelset, but does not add it! Returns 0 if it is not 
    // represented in the interner
    @Override
    protected int getLabelSetID(IntSet labels) {
        return hom.getLabelSetIDByLabelSet(labels);
    }
    
    // Adds a given labelSet to the interner and returns the int value representing it. 
    // This should be called while creating a rule for this automaton.
    @Override
    protected int addLabelSetID(IntSet labels) {
        throw new UnsupportedOperationException("cannot add label set IDs to invhom automaton");
    }
    
    // Reverse function of getLabelSetID. Shold be used by a CondensedRule Object.
    @Override
    protected IntSet getLabelsForID(int labelSetID) {
        return hom.getLabelSetByLabelSetID(labelSetID);
    }

    public void makeAllRulesCondensedExplicit() {
        if (!isCondensedExplicit) {
            
            System.err.println("*** EXPLICIT ");
            
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
