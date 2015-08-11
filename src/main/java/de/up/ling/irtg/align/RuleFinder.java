/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Interner;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFinder {
 
    /**
     * 
     * @param left
     * @param right
     * @param hm
     * @return 
     */
    public TreeAutomaton getRules(TreeAutomaton left, TreeAutomaton right, HomomorphismManager hm){
        TreeAutomaton combined = hm.getCondensedRestriction().intersect(
                left.inverseHomomorphism(hm.getHomomorphism1())).intersect(right.inverseHomomorphism(hm.getHomomorphism2()));
        
        return combined;
    }
    
    /**
     * 
     * @param ruleTrees
     * @param restriction
     * @return 
     */
    public TreeAutomaton generalize(TreeAutomaton ruleTrees, TreeAutomaton restriction){
        ConcreteTreeAutomaton<Integer> cta = new ConcreteTreeAutomaton<>();
        VisitorStoringVariable vsv = new VisitorStoringVariable(cta);
        
        vsv.setOriginal(ruleTrees);
        vsv.setToken(1);
        vsv.setNegativeWeight(false);
        ruleTrees.foreachStateInBottomUpOrder(vsv);
        
        vsv.setOriginal(restriction);
        vsv.setToken(2);
        vsv.setNegativeWeight(true);
        restriction.foreachStateInBottomUpOrder(vsv);
        
        for(Integer i : vsv.getWithX()){
            for(Integer j : vsv.getWithX()){
                Rule r;
                cta.addRule(r = cta.createRule( i, HomomorphismManager.VARIABLE_PREFIX, new Integer[] {j}));
                r.setWeight(0.5);
            }
        }
        
        return cta;
    }
    
    /**
     * 
     * @param ta
     * @return 
     */
    public TreeAutomaton normalize(TreeAutomaton ta){
        ConcreteTreeAutomaton ret = new ConcreteTreeAutomaton(ta.getSignature());
        
        Visitor vis = new Visitor(ret, ta);
        ta.foreachStateInBottomUpOrder(vis);
        
        return ret;
    }
    
    /**
     * 
     * @param tas
     * @return 
     */
    public List<TreeAutomaton> normalizeBulk(Collection<TreeAutomaton> tas){
        List<TreeAutomaton> ret = new ArrayList<>();
        
        for(TreeAutomaton ta : tas){
            ret.add(this.normalize(ta));
        }
        
        return ret;
    }
    
    /**
     * 
     * @param ruleTree
     * @param hm
     * @param lAlg
     * @param rAlg
     * @return 
     */
    public InterpretedTreeAutomaton getInterpretation(TreeAutomaton ruleTree, HomomorphismManager hm,
            Algebra lAlg, Algebra rAlg){
        //TODO
        return null;
    }
    
    /**
     * 
     * @param left
     * @param right
     * @param hm
     * @param lAlg
     * @param rAlg
     * @return 
     */
    public InterpretedTreeAutomaton getInterpretation(TreeAutomaton left, TreeAutomaton right, HomomorphismManager hm,
                                                        Algebra lAlg, Algebra rAlg){
        return this.getInterpretation(this.getRules(left, right, hm), hm, lAlg, rAlg);
    }
    
    /**
     * 
     */
    private class VisitorStoringVariable implements TreeAutomaton.BottomUpStateVisitor
    {        
        /**
         * the automaton we are constructing.
         */
        private final ConcreteTreeAutomaton<Integer> goal;
        
        /**
         * the original with which we started.
         */
        private TreeAutomaton original = null;
        
        /**
         * 
         */
        private Object token = null;
        
        /**
         * 
         */
        private final Interner<Pair<Object,Object>> inter = new Interner();
        
        /**
         * 
         */
        private final Set<Integer> withX = new ObjectOpenHashSet<>();
        
        /**
         * 
         */
        private final Set<Integer> fromX = new ObjectOpenHashSet<>();
        
        /**
         * 
         */
        private boolean negativeWeight = false;
        
        /**
         * Construct a new instance.
         * 
         */
        public VisitorStoringVariable(ConcreteTreeAutomaton<Integer> goal) {
            this.goal = goal;
        }

        /**
         * 
         * @param original 
         */
        public void setOriginal(TreeAutomaton original) {
            this.original = original;
        }

        /**
         * 
         * @param token 
         */
        public void setToken(Object token) {
            this.token = token;
        }

        /**
         * 
         * @param negativeWeight 
         */
        public void setNegativeWeight(boolean negativeWeight) {
            this.negativeWeight = negativeWeight;
        }
        
        /**
         * 
         * @return 
         */
        public Set<Integer> getWithX() {
            return withX;
        }

        /**
         * 
         * @return 
         */
        public Set<Integer> getFromX() {
            return fromX;
        }
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            Integer st = this.makeState(state);
            
            if(original.getFinalStates().contains(state))
            {
                this.fromX.add(st);
                this.goal.addFinalState(this.goal.getIdForState(st));
            }
            
            for(Rule r : rulesTopDown)
            {
                Integer[] arr = makeCopy(r.getChildren());
                String label = r.getLabel(original);
                if(HomomorphismManager.VARIABLE_PATTERN.matcher(label).matches()){
                    this.withX.add(st);
                    this.fromX.add(arr[0]);
                    continue;
                }
                
                double weight = this.negativeWeight ? -1 : 1;
                
                this.goal.addRule(goal.createRule(st, label, arr, weight));
            }
        }

        /**
         * Creates a copy of a rules child states
         * 
         * @param children
         * @return 
         */
        private Integer[] makeCopy(int[] children) {
            Integer[] obs = new Integer[children.length];
            
            for (int i = 0; i < children.length; i++) {
                int state = children[i];
                Integer val = makeState(state);
                
                obs[i] = val;
            }
            
            return obs;
        }

        /**
         * 
         * @param state
         * @return 
         */
        int makeState(int state) {
            return this.inter.addObject(new Pair(this.token, this.original.getStateForId(state)));
        }
        
    }
    
    /**
     * 
     */
    private class Visitor implements TreeAutomaton.BottomUpStateVisitor
    {        
        /**
         * the automaton we are constructing.
         */
        private final ConcreteTreeAutomaton goal;
        
        /**
         * the original with which we started.
         */
        private final TreeAutomaton original;
        

        /**
         * Construct a new instance.
         * 
         */
        public Visitor(ConcreteTreeAutomaton goal, TreeAutomaton original) {
            this.goal = goal;
            this.original = original;
        }
        
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            Object st = this.original.getStateForId(state);
            
            if(original.getFinalStates().contains(state))
            {
                this.goal.addFinalState(this.goal.getIdForState(st));
            }
            
            for(Rule r : rulesTopDown)
            {
                Object[] arr = makeCopy(r.getChildren());
                String label = r.getLabel(original);
                if(HomomorphismManager.VARIABLE_PATTERN.matcher(label).matches()){
                    label = HomomorphismManager.VARIABLE_PREFIX;
                }
                
                double weight = r.getWeight();
                
                this.goal.addRule(goal.createRule(st, label, arr, weight));
            }
        }

        /**
         * Creates a copy of a rules child states
         * 
         * @param children
         * @return 
         */
        private Object[] makeCopy(int[] children) {
            Object[] obs = new Object[children.length];
            
            for (int i = 0; i < children.length; i++) {
                obs[i] = this.original.getStateForId(children[i]);
            }
            
            return obs;
        }
        
    }
}