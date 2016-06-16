/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.util.IntTupleIterator;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * A tree automaton that describes the homomorphic pre-image of the language of
 * another tree automaton. This class only functions correctly if the
 * homomorphism is non-deleting.
 *
 * This automaton has the same states as the base automaton.
 * 
 * 
 * @author koller
 * @param <State>
 */
public class NondeletingInverseHomAutomaton<State> extends TreeAutomaton<State> {
    /**
     * 
     */
    private final static Int2DoubleMap EMPTY = new Int2DoubleOpenHashMap();
    
    
    private final boolean debug = false;

    private final TreeAutomaton<State> rhsAutomaton;
    private final Homomorphism hom;
    private final int[] labelsRemap; // hom-target(id) = rhs-auto(labelsRemap[id])
    private final Int2ObjectMap<Int2ObjectMap<Set<Rule>>> termIDCache;    // termid -> hash(childstates) -> rules
    private final Int2ObjectMap<Int2ObjectMap<Set<Rule>>> parentToTermID;

    public NondeletingInverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;

        labelsRemap = hom.getTargetSignature().remap(rhsAutomaton.getSignature());

        this.stateInterner = (Interner) rhsAutomaton.stateInterner;

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

    // as far as I can tell this method is never used in this class or elsewhere - christoph
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

    // used for debugging purposes, does not seem to handle the case where childStates == null; maybe it would be more reliable to use Arrays.toString
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
     * As far as I can tell, this is never used here or elsewhere - christoph
     * 
     * @param termID
     * @param childStates
     * @return
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
        //int termID = hom.getTermID(label);
        
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

            Int2DoubleMap weightedStates = runWeighted(hom.get(label), f -> {
                if (f.isVariable()) {                      // variable ?i
                    int child = childStates[f.getValue()]; // -> i-th child state (= this state ID)
                    return child;                                     // = rhsAuto state ID
//                        return rhsState.get(child);
                } else {
                    return 0;
                }
            });
            
            IntIterator resultStates = weightedStates.keySet().iterator();

            while(resultStates.hasNext()) {
                int resultState = resultStates.nextInt();
                double weight = weightedStates.get(resultState);
                
                for (int newLabel : hom.getLabelSetForLabel(label)) {
                    Rule rule = createRule(resultState, newLabel, childStates, weight);
                    storeRuleBottomUp(rule);

                    if (newLabel == label) {
                        ret.add(rule);
                    }
                }
            }

            return ret;
        }
    }
    
    /**
     * 
     * @param node
     * @param subst
     * @return 
     */
    public Int2DoubleMap runWeighted(final Tree<HomomorphismSymbol> node, final ToIntFunction<HomomorphismSymbol> subst) {
        if (isBottomUpDeterministic()) {
            Pair<Integer,Double> result = runDeterministicWeighted(node, subst);

            if (result == null) {
                return EMPTY;
            } else {
                Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
                map.addTo(result.getLeft(), result.getRight());
                
                return map;
            }
        } else {
            return runDirectlyWeighted(node, subst);
        }
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return rhsAutomaton.supportsBottomUpQueries();
    }

    /**
     * 
     * @param termID
     * @param parentState
     * @return 
     */
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

            //TODO: merge duplicate rules
            for (Object2DoubleMap.Entry<List<Integer>> substitutionTuple : grtdDfs(rhs, parentState, getRhsArity(rhs)).object2DoubleEntrySet()) {
                if (isCompleteSubstitutionTuple(substitutionTuple.getKey())) {
                    for (int newLabel : hom.getLabelSetForLabel(label)) {
                        Rule rule = createRule(parentState, newLabel, substitutionTuple.getKey(), substitutionTuple.getDoubleValue());
                        
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
        for (int pos=0;pos<tuple.size();++pos) {
            if (tuple.get(pos) == null) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * 
     * @param rhs
     * @return 
     */
    private int getRhsArity(Tree<HomomorphismSymbol> rhs) {
        int max = -1;

        for (HomomorphismSymbol sym : rhs.getLeafLabels()) {
            if (sym.isVariable() && (sym.getValue() > max)) {
                max = sym.getValue();
            }
        }

        return max + 1;
    }

    /**
     * 
     * @param rhs
     * @param state
     * @param rhsArity
     * @return 
     */
    private Object2DoubleOpenHashMap<List<Integer>> grtdDfs(Tree<HomomorphismSymbol> rhs, int state, int rhsArity) {
        Object2DoubleOpenHashMap<List<Integer>> ret = new Object2DoubleOpenHashMap<>();

        switch (rhs.getLabel().getType()) {
            case CONSTANT:
                for (Rule rhsRule : rhsAutomaton.getRulesTopDown(labelsRemap[rhs.getLabel().getValue()], state)) {
                    List<Object2DoubleOpenHashMap<List<Integer>>> childrenSubstitutions = new ArrayList<>(); // len = #children
                    List<Collection<List<Integer>>> keys = new ArrayList<>();
                    
                    for (int i = 0; i < rhsRule.getArity(); i++) {
                        Object2DoubleOpenHashMap<List<Integer>> set = grtdDfs(rhs.getChildren().get(i), rhsRule.getChildren()[i], rhsArity);
                        
                        childrenSubstitutions.add(set);
                        keys.add(set.keySet());
                    }

                    CartesianIterator<List<Integer>> it = new CartesianIterator<>(keys);
                    while (it.hasNext()) {
                        List<List<Integer>> tuples = it.next();  // len = # children x # variables
                        double weight = rhsRule.getWeight();
                        
                        for(int i=0;i<tuples.size();++i) {
                            weight *= childrenSubstitutions.get(i).getDouble(keys.get(i));
                        }
                        System.out.println(rhs);
                        System.out.println(childrenSubstitutions);
                        System.out.println(weight);
                        
                        List<Integer> merged = mergeSubstitutions(tuples, rhsArity);
                        if (merged != null) {
                            //TODO compute correct weight
                            ret.addTo(merged,weight);
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

                ret.addTo(rret,1.0);
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
            List<Integer> inter = tuples.get(i);
            
            for (int j = 0; j < rhsArity; j++) {
                Integer state = inter.get(j);
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

    /**
     * 
     * @param node
     * @param subst
     * @return 
     */
    private Pair<Integer, Double> runDeterministicWeighted(Tree<HomomorphismSymbol> node, ToIntFunction<HomomorphismSymbol> subst) {
        HomomorphismSymbol f = node.getLabel();
        int substState = subst.applyAsInt(f);

        if (substState != 0) {
            return new Pair<>(substState,1.0);
        } else {
            int[] childStates = new int[node.getChildren().size()];
            double weight = 1.0;
            
            for (int i = 0; i < node.getChildren().size(); i++) {
                Pair<Integer,Double> child = runDeterministicWeighted(node.getChildren().get(i), subst);
                if (child == null) {
                    return null;
                } else {
                    childStates[i] = child.getLeft();
                    weight *= child.getRight();
                }
            }

            Iterable<Rule> rules = this.rhsAutomaton.getRulesBottomUp(this.labelsRemap[f.getValue()], childStates);
            Iterator<Rule> it = rules.iterator();

            if (it.hasNext()) {
                Rule r = it.next();
                
                return new Pair<>(r.getParent(),r.getWeight()*weight);
            } else {
                return null;
            }
        }
    }

    /**
     * 
     * @param node
     * @param subst
     * @return 
     */
    private Int2DoubleMap runDirectlyWeighted(Tree<HomomorphismSymbol> node, ToIntFunction<HomomorphismSymbol> subst) {
        HomomorphismSymbol f = node.getLabel();
        Int2DoubleOpenHashMap states = new Int2DoubleOpenHashMap();
        int substState = subst.applyAsInt(f);

        if (substState != 0) {
            states.addTo(substState,1.0);
        } else if (node.getChildren().isEmpty()) {
            for (Rule rule : this.rhsAutomaton.getRulesBottomUp(this.labelsRemap[f.getValue()], new int[0])) {
                states.addTo(rule.getParent(), rule.getWeight());
            }
        } else {
            List<Int2DoubleMap> stateSetsPerChild = new ArrayList<>();

            D1aResult ret = runD1aWeighted(node, subst, stateSetsPerChild);

            if (ret != D1aResult.EMPTY) {
                if (ret != D1aResult.NON_SINGLETON) {
                    runD1SingletonWeighted(f, states, stateSetsPerChild);
                } else {
                    runD2NonsingWeighted(f, states, stateSetsPerChild);
                }
            }
        }

        return states;
    }

    /**
     * 
     * @param node
     * @param subst
     * @param stateSetsPerChild
     * @return 
     */
    private D1aResult runD1aWeighted(Tree<HomomorphismSymbol> node, ToIntFunction<HomomorphismSymbol> subst, List<Int2DoubleMap> stateSetsPerChild) {
        D1aResult ret = null;

        for (int i = 0; i < node.getChildren().size(); i++) {
            Tree<HomomorphismSymbol> child = node.getChildren().get(i);
            Int2DoubleMap childStates = runDirectlyWeighted(child, subst);

            if (childStates.isEmpty()) {
                return D1aResult.EMPTY;
            } else if (childStates.size() > 1) {
                ret = D1aResult.NON_SINGLETON;
            }

            stateSetsPerChild.add(childStates);
        }

        if (ret == null) {
            return D1aResult.OK;
        } else {
            return ret;
        }
    }

    /**
     * 
     * @param f
     * @param states
     * @param stateSetsPerChild 
     */
    private void runD1SingletonWeighted(HomomorphismSymbol f, Int2DoubleOpenHashMap states, List<Int2DoubleMap> stateSetsPerChild) {
        int[] children = new int[stateSetsPerChild.size()];
        double weight = 1.0;

        for (int i = 0; i < stateSetsPerChild.size(); i++) {
            int entry = stateSetsPerChild.get(i).keySet().iterator().nextInt();
            
            children[i] = entry;
            weight *= stateSetsPerChild.get(i).get(entry);
        }
        
        for (Rule rule : this.rhsAutomaton.getRulesBottomUp(this.labelsRemap[f.getValue()], children)) {
            states.addTo(rule.getParent(),weight*rule.getWeight());
        }
    }

    /**
     * 
     * @param f
     * @param states
     * @param stateSetsPerChild 
     */
    private void runD2NonsingWeighted(HomomorphismSymbol f, Int2DoubleOpenHashMap states, List<Int2DoubleMap> stateSetsPerChild) {
        List<IntIterable> basis = new ArrayList<>();
        for(int i=0;i<stateSetsPerChild.size();++i) {
            basis.add(stateSetsPerChild.get(i).keySet());
        }
        
        IntTupleIterator iti = new IntTupleIterator(basis);
        
        while(iti.hasNext()) {
            int[] children = iti.next();
            
            double weight = 1.0;
            for(int pos=0;pos < children.length;++pos) {
                weight *= stateSetsPerChild.get(pos).get(children[pos]);
            }
            
            for (Rule rule : this.rhsAutomaton.getRulesBottomUp(this.labelsRemap[f.getValue()], children)) {
                states.addTo(rule.getParent(), weight*rule.getWeight());
            }
        }
    }
}
