/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * A tree automaton that describes the homomorphic pre-image of the language of
 * another tree automaton. This class only functions correctly if the
 * homomorphism is non-deleting.
 *
 * This automaton has the same states as the base automaton, converted into
 * strings.
 *
 * @author koller
 */
public class NondeletingInverseHomAutomaton<State> extends TreeAutomaton<State> {

    private final boolean debug = false;

    private TreeAutomaton<State> rhsAutomaton;
    private Homomorphism hom;
//    private Map<String, State> rhsState;
    private int[] labelsRemap; // hom-target(id) = rhs-auto(labelsRemap[id])
    private ToIntFunction<HomomorphismSymbol> remappingHomSymbolToIntFunction;
    private Int2ObjectMap<Int2ObjectMap<Set<Rule>>> termIDCache;    // termid -> hash(childstates) -> rules
    private Int2ObjectMap<Int2ObjectMap<Set<Rule>>> parentToTermID;

    public NondeletingInverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;

        labelsRemap = hom.getTargetSignature().remap(rhsAutomaton.getSignature());

        remappingHomSymbolToIntFunction = f -> labelsRemap[HomomorphismSymbol.getHomSymbolToIntFunction().applyAsInt(f)]; // TODO replace by sig mapper

        this.stateInterner = rhsAutomaton.stateInterner;

        assert hom.isNonDeleting();

        termIDCache = new Int2ObjectOpenHashMap<>();
        parentToTermID = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public IntSet getFinalStates() {
        return rhsAutomaton.getFinalStates();
    }

    @Override
    public IntSet getAllStates() {
        return rhsAutomaton.getAllStates();
    }

    public Set<Rule> getRulesBottomUpFromExplicitWithTermID(int termID, int[] childStates) {
        int childHash = Arrays.hashCode(childStates);
        if (debug) {
            System.err.println("Getting for termID " + termID + " and CS: " + childStatesToString(childStates));
        }
        Int2ObjectMap<Set<Rule>> childToRules = termIDCache.get(termID);
        if (childToRules != null) {
            return childToRules.get(childHash);
        } else {
            return null;
        }
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

    /**
     * Checks whether the cache contains a bottom-up rule for the given termID
     * and children states.
     *
     */
    protected boolean useCachedRuleBottomUpWithTermID(int termID, int[] childStates) {
        int childHash = Arrays.hashCode(childStates);
        Int2ObjectMap<Set<Rule>> childToRules = termIDCache.get(termID);
        if (childToRules != null) {
            return childToRules.get(childHash) != null;
        } else {
            return false;
        }
    }

    /*
    @Override
    protected void storeRule(Rule rule) {
//        if (useCachedRuleBottomUpWithTermID(hom.getTermID(rule.getLabel()), rule.getChildren())) {
//            System.err.println("Why is termID and " + hom.getTermID(rule.getLabel()) + " and CS: " + childStatesToString(rule.getChildren()) + " mapped again?");
//            System.err.println("-> " + rule.toString());
//        }

        // store as bottom-up rule
        int termID = hom.getTermID(rule.getLabel());
        int childHash = Arrays.hashCode(rule.getChildren());
        Int2ObjectMap<Set<Rule>> childToRules = termIDCache.get(termID);
        if (childToRules != null) {
            Set<Rule> ruleSet = childToRules.get(childHash);
            if (ruleSet != null) {
                ruleSet.add(rule);
            } else {
                ruleSet = new HashSet<Rule>();
                ruleSet.add(rule);
                childToRules.put(childHash, ruleSet);
            }
        } else {
            childToRules = new Int2ObjectArrayMap<Set<Rule>>();
            Set<Rule> ruleSet = new HashSet<Rule>();
            ruleSet.add(rule);
            childToRules.put(childHash, ruleSet);
            termIDCache.put(termID, childToRules);
        }

        // store as top-down rule
        Int2ObjectMap<Set<Rule>> termIDToRules = parentToTermID.get(rule.getParent());
        if (termIDToRules != null) {
            Set<Rule> ruleSet = termIDToRules.get(termID);
            if (ruleSet != null) {
                ruleSet.add(rule);
            } else {
                ruleSet = new HashSet<Rule>();
                ruleSet.add(rule);
                termIDToRules.put(termID, ruleSet);
            }
        } else {
            termIDToRules = new Int2ObjectArrayMap<Set<Rule>>();
            Set<Rule> ruleSet = new HashSet<Rule>();
            ruleSet.add(rule);
            termIDToRules.put(termID, ruleSet);
            parentToTermID.put(rule.getParent(), termIDToRules);
        }

//        // remember that rules also need to be stored top-down
//        unprocessedUpdatesForTopDown.add(rule);
        // remember that rules need to be indexed for RHS -> rule
//        unprocessedUpdatesForRulesForRhsState.add(rule);
        super.storeRule(rule);
    }
    */

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, final int[] childStates) {
        if (debug) {
            System.err.println("Handling label " + label + " and CS : " + childStatesToString(childStates));
        }
        
        
        
        // lazy bottom-up computation of bottom-up rules
        int termID = hom.getTermID(label);
        
        if( useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        }
        
        
        // TODO - this code caused multiple copies of the same rule to be generated,
        // one for each label with the same termID. This is obviously wrong.
        // That said, it would probably okay to only cache a single rule for
        // each term ID, and then replace the labels by the requested label
        // on the fly. We should do this sometime.
        
//        if (useCachedRuleBottomUpWithTermID(termID, childStates)) {
//            return getRulesBottomUpFromExplicitWithTermID(termID, childStates);
        
        
        else {
            Set<Rule> ret = new HashSet<>();

            IntIterable resultStates = rhsAutomaton.run(hom.get(label), remappingHomSymbolToIntFunction, f -> {
                if (f.getLabel().isVariable()) {                      // variable ?i
                    int child = childStates[f.getLabel().getValue()]; // -> i-th child state (= this state ID)
                    return child;                                     // = rhsAuto state ID
//                        return rhsState.get(child);
                } else {
                    return 0;
                }
            });

            for (int r : resultStates) {
                // TODO: weight
                for (int newLabel : hom.getLabelSetForLabel(label)) {
                    Rule rule = createRule(r, newLabel, childStates, 1);
                    storeRuleBottomUp(rule);

                    if (newLabel == label) {
                        ret.add(rule);
                    }
                }
            }

            return ret;
        }
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return rhsAutomaton.supportsBottomUpQueries();
    }

    private boolean useCachedRuleTopDownWithTermID(int termID, int parentState) {
        Int2ObjectMap<Set<Rule>> termIDToRules = parentToTermID.get(parentState);
        if (termIDToRules != null) {
            return termIDToRules.get(termID) != null;
        } else {
            return false;
        }
    }

    private Set<Rule> getRulesTopDownFromExplicitWithTermID(int termID, int parentState) {
        Int2ObjectMap<Set<Rule>> termIDToRules = parentToTermID.get(parentState);
        if (termIDToRules != null) {
            return termIDToRules.get(termID);
        } else {
            return null;
        }

    }

    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        int termID = hom.getTermID(label);
        if (useCachedRuleTopDownWithTermID(termID, parentState)) {
            return getRulesTopDownFromExplicitWithTermID(termID, parentState);
        } else {
            Tree<HomomorphismSymbol> rhs = hom.get(label);
            Set<Rule> ret = new HashSet<>();

            for (List<Integer> substitutionTuple : grtdDfs(rhs, parentState, getRhsArity(rhs))) {
                if (isCompleteSubstitutionTuple(substitutionTuple)) {
                    // TODO: weights
                    for (int newLabel : hom.getLabelSetForLabel(label)) {
                        Rule rule = createRule(parentState, newLabel, substitutionTuple, 1);
                        storeRuleTopDown(rule);
                        ret.add(rule);
                    }
                }
            }

            return ret;
        }
    }

    @Override
    public boolean supportsTopDownQueries() {
        return rhsAutomaton.supportsTopDownQueries();
    }

    /*
     // note that this breaks the invariant that the state IDs in the interner
     // are a contiguous interval
     protected Rule createRuleI(int parentState, int label, List<Integer> childStates, double weight) {
     return createRuleI(parentState, label, intListToArray(childStates), weight);
     }
    
     protected Rule createRuleI(int parentState, int label, int[] childStates, double weight) {
     stateInterner.addObjectWithIndex(parentState, rhsAutomaton.getStateForId(parentState).toString());
        
     for( int child : childStates ) {
     stateInterner.addObjectWithIndex(child, rhsAutomaton.getStateForId(child).toString());
     }
        
     return super.createRule(parentState, label, childStates, weight);
     }
     */
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
        Set<List<Integer>> ret = new HashSet<>();

        switch (rhs.getLabel().getType()) {
            case CONSTANT:
                for (Rule rhsRule : rhsAutomaton.getRulesTopDown(labelsRemap[rhs.getLabel().getValue()], state)) {
                    List<Set<List<Integer>>> childrenSubstitutions = new ArrayList<>(); // len = #children

                    for (int i = 0; i < rhsRule.getArity(); i++) {
                        childrenSubstitutions.add(grtdDfs(rhs.getChildren().get(i), rhsRule.getChildren()[i], rhsArity));
                    }

                    CartesianIterator<List<Integer>> it = new CartesianIterator<>(childrenSubstitutions);
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
                List<Integer> rret = new ArrayList<>(rhsArity);
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

    // tuples is an n-list of m-lists of output states, where
    // n is number of children, and m is number of variables in homomorphism
    // If n = 0, the method returns [null, ..., null]
    private List<Integer> mergeSubstitutions(List<List<Integer>> tuples, int rhsArity) {
        List<Integer> merged = new ArrayList<>();  // one entry per variable

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
}
