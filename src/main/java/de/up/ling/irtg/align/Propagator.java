/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.align.alignment_marking.SpecifiedAligner;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.Semiring;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author christoph_teichmann
 */
public class Propagator {
    
    /**
     * 
     */
    private final VariablePropagator vp = new VariablePropagator();
 
    /**
     * 
     */
    private final Pruner pruner;
    
    /**
     * 
     */
    public Propagator(){
        this(Pruner.DEFAULT_PRUNER);
    }
    
    /**
     * 
     * @param pruner 
     */
    public Propagator(Pruner pruner){
        this.pruner = pruner;
    }
    
    
    /**
     * 
     * @param input
     * @param alignments
     * @return 
     */
    public Int2ObjectMap<IntSet> propagate(TreeAutomaton input, StateAlignmentMarking alignments){
        Int2ObjectMap<IntSet> map = input.evaluateInSemiring(vp, alignments);
        return map;
    }
    
    /**
     * 
     * @param <State>
     * @param input
     * @param alignments
     * @return 
     */
    public <State> TreeAutomaton convert(TreeAutomaton<State> input, StateAlignmentMarking<State> alignments){
        Int2ObjectMap markings = this.propagate(input, alignments);
        final ConcreteTreeAutomaton<State> output = new ConcreteTreeAutomaton<>(input.getSignature());
        final SpecifiedAligner map = new SpecifiedAligner(output);
        
        input = this.pruner.prePrune(input, alignments);
        
        Visitor visit = new Visitor(markings, output, input, map, alignments);
        input.foreachStateInBottomUpOrder(visit);
        
        return this.pruner.postPrune(output, map);
    }
    
    /**
     * 
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
           
           // We do not want the same alignment marker twice in a derivation.
           while(ii.hasNext()){
                if(y.contains(ii.nextInt())){
                    throw new IllegalStateException("Adding a variable twice along one derivation path is"
                            + " against the rules for alignment markers; attempted for: "+x+" "+y);
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
         * 
         */
        private final SpecifiedAligner local;
        
        /**
         * 
         */
        private final StateAlignmentMarking mark;

        /**
         * 
         * @param vars
         * @param goal
         * @param original
         * @param local
         * @param mark 
         */
        public Visitor(Int2ObjectMap<IntSet> vars, ConcreteTreeAutomaton goal,
                TreeAutomaton original, SpecifiedAligner local, StateAlignmentMarking mark) {
            this.vars = vars;
            this.goal = goal;
            this.original = original;
            this.local = local;
            this.mark = mark;
        }

        
        
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            Object st = this.original.getStateForId(state);
            String loopLabel = HomomorphismManager.VARIABLE_PREFIX+"_"+this.vars.get(state).toString();
            // here we add a loop for the alignments
            this.goal.addRule(this.goal.createRule(st, loopLabel, new Object[] {st}));
            this.local.put(this.goal.getIdForState(st), this.mark.getAlignmentMarkers(st));
            
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
}