/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Logging;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * This is an extension of CondensedTreeAutomaton for which all rules can and must
 * be added externally and are then stored explicitly for quick access.
 * 
 * @author johannes
 * @param <State>
 */
public class ConcreteCondensedTreeAutomaton<State> extends CondensedTreeAutomaton<State> {
    
    /**
     * Constructs a new instance which is backed by the given signature.
     * 
     * The automaton will contain no rules or final states.
     * The automaton might add to the signature when it creates new rules.
     * 
     */
    public ConcreteCondensedTreeAutomaton(Signature sig) {
        super(sig);
        
        // Do NOT set isExplicit to true here! This means that all the condensed
        // rules have been spelled out explicitly as uncondensed rules, which is
        // something we want to avoid.
        
        isCondensedExplicit = true;
    }
    
    /**
     * Creates a new instance which will have its own signature and contains no
     * rules or final states.
     */
    public ConcreteCondensedTreeAutomaton() {
        super(new Signature());
        
        // Do NOT set isExplicit to true here! This means that all the condensed
        // rules have been spelled out explicitly as uncondensed rules, which is
        // something we want to avoid.
        
        isCondensedExplicit = true;
    }
    
    /**
     * Creates a new CondensedTreeAutomaton based on the rules and final states of another TreeAutomaton.
     * 
     * Rules in the original TreeAutomaton that have the same child states and the the same parent state, 
     * but differ in their label, will be merged to form a CondensedRule
     *
     * @param <E>
     */
    public static <E> ConcreteCondensedTreeAutomaton<E> fromTreeAutomaton(TreeAutomaton<E> origin) {
        ConcreteCondensedTreeAutomaton<E> ret = new ConcreteCondensedTreeAutomaton<>();
        
        ret.signature = origin.getSignature();
        ret.stateInterner = origin.getStateInterner();
        
        ret.ruleTrie = new CondensedRuleTrie();
        ret.topDownRules = new Int2ObjectOpenHashMap<>();
        ret.absorbTreeAutomaton(origin);
//        
//        System.err.println("original: \n" + origin);
//        
//        System.err.println("condensed: \n" + ret.toStringCondensed());
//        
//        System.err.println("condensed as ordinary automaton: \n" + ret);
//        
        Logging.get().setLevel(Level.ALL);
        return ret;
    }
    
    @Override
    public int addState(State state) {
        return super.addState(state); 
    }
    
    @Override
    public void addFinalState(int state) {
        super.addFinalState(state); 
    }
        
    public void addRule(CondensedRule rule) {
        storeRule(rule);
    }
    
    @Override
    public Iterable<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates) {
        return getCondensedRuleBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState) {
        return getCondensedRulesTopDownFromExplicit(labelId, parentState);
    }

    /**
     * Copies all rules from a TreeAutomaton to this automaton. Merges rules which can be condensed.
     */
    public void absorbTreeAutomaton(TreeAutomaton<State> auto) {
        for (Rule rule : auto.getRuleSet()) {
//            System.err.println("absorb rule: " + rule.toString(auto));
            storeRule(rule, auto);
        }
    }
    
    /**
     * Creates a condensed rule based on a given rule and the automaton, that has created it.
     * The new rule will be stored in the internal data structures, see storeRule
     */
    private void storeRule(Rule rule, TreeAutomaton<State> auto) {
        // condense
        int[] newChildren = rule.getChildren(); // convertChildrenStates(rule.getChildren(), auto);
        int newParent = rule.getParent(); // addState(auto.getStateForId(rule.getParent()));
        int newLabel = rule.getLabel(); // signature.addSymbol(rule.getLabel(auto), newChildren.length);
        
        int newLabelSetID = -1;
        
        CondensedRuleTrie finalTrie = ruleTrie.getFinalTrie(newChildren);
        Int2ObjectMap<Set<CondensedRule>> ruleMap = finalTrie.getLabelSetIDToRulesMap();
        
        // maintain invariant: for each children list and parent, there is only one label set which connects them
        // on the other hand, may have to search for parent in more than just the first rule
        
        // iterate over all labelSetIDs for the current children.
        for (int possibleLabelSetID : finalTrie.getStoredKeys()) {
            // if the parentstate for the current labelSetID matches, we found or labelSet
            IntSet parents =  finalTrie.getParents(possibleLabelSetID);
            if (parents.contains(newParent)) {
                newLabelSetID = possibleLabelSetID;
            }
        }
        
        // In case there is no existing entry for the childs and parent state of the given rule,
        // we have to create one.
        if (newLabelSetID == -1) {
            IntSet newLabels = new IntOpenHashSet();
            newLabels.add(newLabel);
            newLabelSetID = labelSetInterner.addObject(newLabels);
        } else {
            // add current label to the existing labelset
            labelSetInterner.addValueToSetByID(newLabelSetID, newLabel);
//            labelSetInterner.resolveId(newLabelSetID).add(newLabel);
        }
        
        CondensedRule newRule = createRuleRaw(newParent, newLabelSetID, newChildren, rule.getWeight());
        
        newRule.setExtra(rule.getExtra());

        storeRule(newRule);
        
        // Absorb final states
        if (auto.getFinalStates().contains(rule.getParent())) {
            finalStates.add(newParent);
        }
//        System.err.println("created Rule: " + newRule.toString(this));

//            System.err.println("OUT: " + newRule.toString(this));
    }

    @Override
    public void makeAllRulesCondensedExplicit() {
        isCondensedExplicit = true; 
    }
    
}
