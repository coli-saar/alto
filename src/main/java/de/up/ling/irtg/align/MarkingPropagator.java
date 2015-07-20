/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.Semiring;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * This class takes alignments for single rules and propagates them through the complete automaton.
 * 
 * As a result we get a single automaton that is clearly marked for the possible positions of
 * frontier nodes.
 * 
 * @author christoph
 */
public class MarkingPropagator {
        
    /**
     * This method takes an automaton and a rule marker and creates a new automaton
     * that marks alignment set in special labels but is otherwise like the input automaton.
     * 
     * Note that alignment sets must be unique for every state or this will result in an exception.
     * Alignment markers may also occur only once.
     * 
     * @param input the automaton that we read
     * @param rlm a rule marker that can work with the given automaton
     * @param num which of the two automata the marker is aware of is used here.
     * @return 
     */
    public TreeAutomaton introduce(TreeAutomaton input, RuleMarker rlm, int num){
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton(input.getSignature());
        
        // basically everything is done by the semiring framework
        Semiring<IntSet> sr = new VariablePropagator();
        
        Int2ObjectMap<IntSet> mapping = input.evaluateInSemiring2(sr, rlm.ruleMarkings(num));
        
        // we only need to compute the new automaton
        Visitor vis = new Visitor(mapping, rlm, cta, input);
        input.foreachStateInBottomUpOrder(vis);
        
        return cta;
    }
    
    /**
     * A simple visitor used to construct the new automaton by copying the rules of the old and introducing some
     * extra labels.
     */
    private class Visitor implements TreeAutomaton.BottomUpStateVisitor
    {
        
        /**
         * The variable sets we are aware of for the states.
         */
        private final Int2ObjectMap<IntSet> vars;
        
        /**
         * the automaton we are constructing.
         */
        private final ConcreteTreeAutomaton goal;
        
        /**
         * the original with which we started.
         */
        private final TreeAutomaton original;
        
        /**
         * the rule marker for encoding the variable sets as labels.
         */
        private final RuleMarker rlm;
        

        /**
         * Construct a new instance.
         * 
         */
        public Visitor(Int2ObjectMap<IntSet> vars, RuleMarker rlm,
                                        ConcreteTreeAutomaton goal, TreeAutomaton original) {
            this.vars = vars;
            this.goal = goal;
            this.original = original;
            this.rlm = rlm;
        }
        
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            
            Object st = this.original.getStateForId(state);
            String loopLabel = rlm.makeCode(this.vars.get(state),this.original,state);
            // here we add a loop for the alignments
            this.goal.addRule(this.goal.createRule(st, loopLabel, new Object[] {st}));
            
            if(original.getFinalStates().contains(state))
            {
                this.goal.addFinalState(this.goal.getIdForState(st));
            }
            
            for(Rule r : rulesTopDown)
            {
                Object[] arr = makeCopy(r.getChildren());
                String label = r.getLabel(original);
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
    
    /**
     * This semiring is used to keep track of the alignments.
     */
    private static class VariablePropagator implements Semiring<IntSet> {

        /**
         * The empty set used as a starting point.
         */
        private final static IntSet ZERO = new IntAVLTreeSet();
        
        @Override
        public IntSet add(IntSet x, IntSet y) {
            // at the start we just take one of the inputs.
            if(ZERO == x){
                return y;
            }
            if(ZERO == y){
                return x;
            }
            // if the sets do not match, then we have a violation of our assumptions.
            if(!x.equals(y)){    
                throw new IllegalStateException("Variables dominated by states are not unique");
            }
            
            return x;
        }

        @Override
        public IntSet multiply(IntSet x, IntSet y) {
           IntIterator ii = x.iterator();
           
           // We do not want the same alignment twice in one automaton.
           while(ii.hasNext()){
                if(y.contains(ii.nextInt())){
                    throw new IllegalStateException("Adding a variable twice is against the rules for alignment"
                        + "markers; attempted for: "+x+" "+y);
                }
            }
           
            IntSet set = new IntAVLTreeSet(x);
            set.addAll(y);
           
           return set;
        }

        @Override
        public IntSet zero() {
            return ZERO;
        }
    };
}