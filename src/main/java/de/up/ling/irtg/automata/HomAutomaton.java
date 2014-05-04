/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A tree automaton that describes the homomorphic
 * image of the language of another tree automaton.
 * 
 * The states of this automaton are strings. Every state
 * of the base automaton, converted to its string representation,
 * is a state of this automaton. There are also further states
 * that represent intermediate steps in reading the right-hand
 * sides of homomorphisms.
 * 
 * @author koller
 */
class HomAutomaton extends TreeAutomaton<Object> {
    private TreeAutomaton base;
    private Homomorphism hom;
    private int gensymNext = 1;

    public HomAutomaton(TreeAutomaton base, Homomorphism hom) {
        super(hom.getTargetSignature());
        
        this.base = base;
        this.hom = hom;
        
        this.stateInterner = (Interner) base.stateInterner;
        this.allStates = new IntOpenHashSet(base.getAllStates());
        
        finalStates.addAll(base.getFinalStates());
    }

    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Iterable<Rule> baseRuleSet = base.getRuleSet();
            SetMultimap<Integer, Integer> chainRules = HashMultimap.create();  // maps base state IDs to sets of base state IDs
            final Set<Integer> labels = new HashSet<Integer>();

            for (final Rule rule : baseRuleSet) {
                final Tree<HomomorphismSymbol> homImage = hom.get(rule.getLabel());

                if (homImage.getLabel().isVariable()) {
                    // special case for homomorphisms of the form ?1 or ?2 etc.: store chain rule

                    int childPosition = homImage.getLabel().getValue();
                    chainRules.put(rule.getChildren()[childPosition], rule.getParent());
                } else {
                    // otherwise, iterate over homomorphic image of rule label and
                    // introduce rules as we go along
                    homImage.dfs(new TreeVisitor<HomomorphismSymbol, Void, Integer>() {
                        @Override
                        public Integer combine(Tree<HomomorphismSymbol> node, List<Integer> childrenValues) {
                            HomomorphismSymbol label = node.getLabel();

                            if (label.isVariable()) {
                                return rule.getChildren()[label.getValue()];
                            } else {
                                int parentState = 0;
                                double weight = 0;

                                if (node == homImage) {
                                    parentState = copyState(rule.getParent());
                                    weight = rule.getWeight();
                                } else {
                                    parentState = gensymState();
                                    weight = 1;
                                }

                                Rule newRule = createRule(parentState, label.getValue(), childrenValues, weight);
                                storeRule(newRule);
                                labels.add(label.getValue());
                                return parentState;
                            }
                        }
                    });
                }
                
                // now process chain rules
                for( Entry<Integer,Integer> entry : chainRules.entries() ) {
                    int lowerParent = entry.getKey();
                    int upperParent = entry.getValue();
                    
                    for( int label : labels ) {
                        for( Rule ruleForEntry : getRulesTopDownFromExplicit(label, lowerParent) ) {
                            // TODO: correct weight
                            storeRule(createRule(upperParent, label, ruleForEntry.getChildren(), 1)); 
                        }
                    }
                }
            }

            isExplicit = true;
        }
    }
    
    // note that this breaks the invariant that the state IDs in the interner
    // are a contiguous interval
    private int copyState(int state) {
        stateInterner.addObjectWithIndex(state, base.getStateForId(state).toString());
        return state;
    }
    
    
    /*
    protected Rule createRuleI(int parentState, int label, List<Integer> childStates, double weight) {
        return createRuleI(parentState, label, intListToArray(childStates), weight);
    }
    
    protected Rule createRuleI(int parentState, int label, int[] childStates, double weight) {
        stateInterner.addObjectWithIndex(parentState, base.getStateForId(parentState).toString());
        
        for( int child : childStates ) {
            stateInterner.addObjectWithIndex(child, base.getStateForId(child).toString());
        }
        
        return super.createRule(parentState, label, childStates, weight);
    }
    */

    private int gensymState() {
        return addState("qh" + (gensymNext++));
    }

    @Override
    public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
        makeAllRulesExplicit();
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        makeAllRulesExplicit();
        return getRulesTopDownFromExplicit(label, parentState);
    }

//
//    @Override
//    public IntSet getAllStates() {
//        makeAllRulesExplicit();
//        return super.getAllStates();
//    }

    @Override
    public boolean isBottomUpDeterministic() {
        return base.isBottomUpDeterministic();
    }
}
