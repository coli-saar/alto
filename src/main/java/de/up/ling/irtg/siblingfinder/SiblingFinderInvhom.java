
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.siblingfinder;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author groschwitz
 * @param <State>
 */
public class SiblingFinderInvhom<State> {

    private final Homomorphism hom;
    private final TreeAutomaton<State> decompAuto;
    private final ConcreteTreeAutomaton<State> seenRulesAuto;
    
    private final IntTrie<List<Rule>> child2label2pos2rules;
    private final Int2ObjectMap<Map<String, SiblingFinder>> term2pos2PF;
    Int2ObjectMap<Map<String, Int2ObjectMap<List<Pair<int[], Double>>>>> labelAndPosAndStateToVariablesAndWeights;
    private final Int2ObjectMap<String[]> termAndLeafNr2Path;
    private final Int2ObjectMap<int[]> termAndVarPos2LeafNr;
    private final BitSet isPrecomputed;
    
    @OperationAnnotation(code="veryLazyInvhom")
    public SiblingFinderInvhom(TreeAutomaton<State> decompositionAutomaton, Homomorphism hom) {
        
        child2label2pos2rules = new IntTrie<>();
        seenRulesAuto = new ConcreteTreeAutomaton<>(hom.getSourceSignature());
        
        this.isPrecomputed = new BitSet();
        this.hom = hom;
        this.decompAuto = decompositionAutomaton;
        
        term2pos2PF = new ArrayMap<>();
        for (int labelID = 1; labelID <= hom.getSourceSignature().getMaxSymbolId(); labelID++) {
            term2pos2PF.put(labelID, new HashMap<>());
        }
        labelAndPosAndStateToVariablesAndWeights = new Int2ObjectOpenHashMap<>(); //first int is labelID of this.automaton, then map position in term (represented with a string) to a map from states to all possible variable assignments below that produce this state.
        
        termAndLeafNr2Path = new ArrayMap<>();//is dense
        termAndVarPos2LeafNr = new ArrayMap<>();//is dense
        for (int labelID = 1; labelID <= hom.getSourceSignature().getMaxSymbolId(); labelID++) {
            Tree<HomomorphismSymbol> term = hom.get(labelID);
            if (term != null) { //hom might not map all of the signature
                Collection<String> allPaths2Leaves = term.getAllPathsToLeaves();
                String[] leafNr2Path = new String[allPaths2Leaves.size()];
                int[] pos2leafNr = new int[hom.getSourceSignature().getArity(labelID)];
                int leafNr = 0;
                for (String path : term.getAllPathsToLeaves()) {
                    leafNr2Path[leafNr] = path;

                    //check if it is a variable
                    HomomorphismSymbol leaf = term.select(path, 0).getLabel();
                    if (leaf.isVariable()) {
                        pos2leafNr[leaf.getValue()] = leafNr;
                    }
                    leafNr++;
                }
                termAndLeafNr2Path.put(labelID, leafNr2Path);
                termAndVarPos2LeafNr.put(labelID, pos2leafNr);
            }
        }
        
        
    }
    
    /**
     * Returns all rules with label 'label' and child childState at position pos.
     * The rules are then explicitly stored.
     * The returned iterable is the same object as is stored, so do not modify it.
     * @param childState
     * @param pos
     * @param label
     * @return 
     */
    public Iterable<Rule> getRulesBottomUp(int childState, int pos, int label) {
        addState(childState);
        int[] trieKey = new int[]{childState, label, pos};
        List<Rule> ret = child2label2pos2rules.get(trieKey);
        if (ret == null) {
            //then we have not asked this question before
            ret = new ArrayList<>();
            child2label2pos2rules.put(trieKey, ret);
            if (!termAndLeafNr2Path.containsKey(label) || !termAndVarPos2LeafNr.containsKey(label)) {
                return ret;
            } else {
                String leafPath = termAndLeafNr2Path.get(label)[termAndVarPos2LeafNr.get(label)[pos]];
                Pair<IntSet, Int2ObjectMap<List<Pair<int[], Double>>>> parentsAndChildren = iterateThroughTerm(leafPath, childState, label, 1);
                for (int parent : parentsAndChildren.left) {
                    addState(parent);
                    for (Pair<int[], Double> children : parentsAndChildren.right.get(parent)) {
                        Rule rule = seenRulesAuto.createRule(parent, label, children.left, children.right);
                        seenRulesAuto.addRule(rule);
                        ret.add(rule);
                        for (int otherPos = 0; otherPos < rule.getArity(); otherPos++) {
                            if (rule.getChildren()[otherPos] != 0) {//in case of deleting rules
                                child2label2pos2rules.get(new int[]{rule.getChildren()[otherPos], label, otherPos}).add(rule);//we know that this is not null, since we must have asked the corresponding question before
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    private void addState(int childState) {
        if (seenRulesAuto.getStateForId(childState) == null) {
            seenRulesAuto.getStateInterner().addObjectWithIndex(childState, decompAuto.getStateForId(childState));
            if (decompAuto.getFinalStates().contains(childState)) {
                seenRulesAuto.addFinalState(childState);
            }
        }
    }
    
    private Pair<IntSet, Int2ObjectMap<List<Pair<int[], Double>>>> iterateThroughTerm(String leafPath, int rhsGivenState, int ruleLabel, double baseWeight) {
        
        Tree<HomomorphismSymbol> term = hom.get(ruleLabel);
        int arity = hom.getSourceSignature().getArity(ruleLabel);
        
        if (!isPrecomputed.get(ruleLabel)) {
            isPrecomputed.set(ruleLabel);
            for (String path : termAndLeafNr2Path.get(ruleLabel)) {
                HomomorphismSymbol leaf = term.select(path, 0).getLabel();
                if (!leaf.isVariable()) {
                    for (Rule rule : decompAuto.getRulesBottomUp(leaf.getValue(), new int[0])) {
                        iterateThroughTerm(path, rule.getParent(), ruleLabel, rule.getWeight());
                    }
                }
            }
        }
        
        String pathHere = leafPath;

        //introduce rhsState at variable
        Map<String, Int2ObjectMap<List<Pair<int[], Double>>>> pos2State2Vars = labelAndPosAndStateToVariablesAndWeights.get(ruleLabel);
        if (pos2State2Vars == null) {
            pos2State2Vars = new HashMap<>();
            labelAndPosAndStateToVariablesAndWeights.put(ruleLabel, pos2State2Vars);
        }
        Int2ObjectMap<List<Pair<int[], Double>>> state2VarsLeaf = pos2State2Vars.get(leafPath);
        if (state2VarsLeaf == null) {
            state2VarsLeaf = new Int2ObjectOpenHashMap<>();
            pos2State2Vars.put(leafPath, state2VarsLeaf);
        }
        List<Pair<int[], Double>> givenVars;
        HomomorphismSymbol node = hom.get(ruleLabel).select(leafPath, 0).getLabel();
        int[] variables = new int[arity];//initialised to 0, will be filled with states > 0
        if (node.isVariable()) {
            variables[node.getValue()] = rhsGivenState;
        }
        givenVars = Collections.singletonList(new Pair(variables, baseWeight));//cost for a variable is 0, cost for constant is included in baseLogWeight
        state2VarsLeaf.put(rhsGivenState, givenVars);//we know we haven't seen this rhsState at this position yet, so we can just put new set.        
        
        //setup iteration through term
        List<Tree<HomomorphismSymbol>> subtrees = new ArrayList<>();
        Tree<HomomorphismSymbol> currentTree = term;
        subtrees.add(currentTree);
        for (int i = 0; i < pathHere.length(); i++) {
            currentTree = currentTree.getChildren().get(Character.getNumericValue(pathHere.charAt(i)));
            subtrees.add(currentTree);
        }
        IntSet prevRhsStates = new IntOpenHashSet();
        prevRhsStates.add(rhsGivenState);
        Int2ObjectMap<List<Pair<int[], Double>>> prevState2Vars = new Int2ObjectOpenHashMap<>();
        List<Pair<int[], Double>> seedVars = new ArrayList<>();
        seedVars.addAll(givenVars);
        prevState2Vars.put(rhsGivenState, seedVars);

        /*Map<String, PartnerFinder> pos2PF = termPosToPF.get(ruleLabel);
        if (pos2PF == null) {
            pos2PF = new HashMap<>();
            termPosToPF.put(ruleLabel, pos2PF);
        }*/

        //now iterate through term
        String parentPath = leafPath;
        for (int j = pathHere.length()-1; j >= 0; j--) {
            Tree<HomomorphismSymbol> parent = subtrees.get(j);
            int rhsParentLabelID = parent.getLabel().getValue();
            int rhsGivenChildPos = Character.getNumericValue(pathHere.charAt(j));//position of the child we are coming from in this rule
            parentPath = parentPath.substring(0, parentPath.length()-1);
           /* //setup the termPosTo...
            PartnerFinder pf = pos2PF.get(parentPath);
            if (pf == null) {
                pf = decompAuto.makeNewPartnerFinder(rhsParentLabelID);
                pos2PF.put(parentPath, pf);
            }*/
            //add earlier states to pf
            for (int prevState : prevRhsStates) {
                //AverageLogger.increaseValue("addedToPF");
                //pf.addState(prevState, rhsGivenChildPos);
                Map<String, SiblingFinder> pos2PF = term2pos2PF.get(ruleLabel);
                SiblingFinder pf = pos2PF.get(parentPath);
                if (pf == null) {
                    pf = decompAuto.makeNewPartnerFinder(rhsParentLabelID);
                    pos2PF.put(parentPath, pf);
                }
                pf.addState(prevState, rhsGivenChildPos);
            }
            
            //setup the termPosTo...
            Int2ObjectMap<List<Pair<int[], Double>>> state2Vars = pos2State2Vars.get(parentPath);
            if (state2Vars == null) {
                state2Vars = new Int2ObjectOpenHashMap<>();
                pos2State2Vars.put(parentPath, state2Vars);
            }

            //now the actual iteration
            IntSet newRhsStates = new IntOpenHashSet();//these might have already been seen, is that a problem?
            Int2ObjectMap<List<Pair<int[], Double>>> newState2Vars = new Int2ObjectOpenHashMap<>();

            for (int rhsStateID : prevRhsStates) {
                for (int[] children : term2pos2PF.get(ruleLabel).get(parentPath).getPartners(rhsStateID, rhsGivenChildPos)) {
                    Iterable<Rule> rules = decompAuto.getRulesBottomUp(rhsParentLabelID, children);
                    for (Rule rhsRule : rules) {
                        int newRhsParentState = rhsRule.getParent();
                        
                        //add new Rhs state to agenda for next iteration
                        newRhsStates.add(newRhsParentState);

                        //collect variable assignments below parent
                        List<Pair<int[], Double>> tempVarsSet = new ArrayList<>();
                        tempVarsSet.add(new Pair(new int[arity], rhsRule.getWeight()));//add rule weight in the beginning
                        for (int i = 0; i < children.length; i++) {
                            List<Pair<int[], Double>> childVarsSet;
                            if (i == rhsGivenChildPos) {
                                childVarsSet = prevState2Vars.get(rhsStateID);
                            } else {
                                childVarsSet = labelAndPosAndStateToVariablesAndWeights.get(ruleLabel).get(parentPath+i).get(children[i]);
                            }
                            if (!childVarsSet.isEmpty()) {
                                List<Pair<int[], Double>> newTempVarsSet = new ArrayList<>();
                                for (Pair<int[], Double> childVars : childVarsSet) {
                                    for (Pair<int[], Double> tempVars : tempVarsSet) {
                                        int[] combinedVars = new int[arity];
                                        for (int varPos = 0; varPos<arity; varPos++) {
                                            combinedVars[varPos]=Math.max(childVars.left[varPos], tempVars.left[varPos]);//keep the ones that are non-zero
                                        }
                                        newTempVarsSet.add(new Pair(combinedVars, tempVars.right*childVars.right));
                                    }
                                }
                                tempVarsSet = newTempVarsSet;
                            }
                        }
                        
                        
                        List<Pair<int[], Double>> newVars = newState2Vars.get(newRhsParentState);
                        if (newVars == null) {
                            newVars = new ArrayList<>();
                            newState2Vars.put(newRhsParentState, newVars);
                        }
                        newVars.addAll(tempVarsSet);

                        List<Pair<int[], Double>> varsHere = state2Vars.get(newRhsParentState);
                        if (varsHere == null) {
                            varsHere = new ArrayList<>();
                            state2Vars.put(newRhsParentState, varsHere);
                        }
                        varsHere.addAll(tempVarsSet);
                    }
                }
            }
            prevState2Vars = newState2Vars;
            prevRhsStates = newRhsStates;
        }
        return new Pair(prevRhsStates, prevState2Vars);
    }

    public Iterable<Rule> getConstantBottomUp(int label) {
        Set<Rule> ret = new HashSet<>();//set best here? better list?
        Tree<HomomorphismSymbol> term = hom.get(label);
        if (term == null) {//hom might not map the label
            return ret;
        } else {
            IntIterator rhsResIt = decompAuto.runRaw(hom.get(label).map(symbol -> symbol.getValue())).iterator();
            while (rhsResIt.hasNext()) {
                int rhsStateID = rhsResIt.nextInt();
                addState(rhsStateID);

                //make rule
                Rule resRule = seenRulesAuto.createRule(rhsStateID, label, new int[0], 1);
                seenRulesAuto.addRule(resRule);
                ret.add(resRule);
            }
            return ret;
        }
    }
    
    public ConcreteTreeAutomaton<State> seenRulesAsAutomaton() {
        return seenRulesAuto;
    }
    
    @Override
    public String toString() {
        return seenRulesAsAutomaton().toString();
    }
    
    
    public State getStateForId(int id) {
        return seenRulesAuto.getStateForId(id);
    }
    
    public IntSet getFinalStates() {
        return seenRulesAuto.getFinalStates();
    }
    
}
