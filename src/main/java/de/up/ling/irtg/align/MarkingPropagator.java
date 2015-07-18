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
 *
 * @author christoph
 */
public class MarkingPropagator {  
    
    /**
     * 
     */
    private final String setPrefix;
    
    /**
     * 
     * @param setPrefixMarker 
     */
    public MarkingPropagator(String setPrefixMarker)
    {
        this.setPrefix = setPrefixMarker;
    }
    
    /**
     * 
     * @param input
     * @param rlm
     * @param num
     * @return 
     */
    public TreeAutomaton introduce(TreeAutomaton input, RuleMarker rlm, int num){
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton(input.getSignature());
        
        Semiring<IntSet> sr = new VariablePropagator();
        
        Int2ObjectMap<IntSet> mapping = input.evaluateInSemiring2(sr, rlm.ruleMarkings(num));
        
        Visitor vis = new Visitor(setPrefix, mapping, rlm, cta, input);
        input.foreachStateInBottomUpOrder(vis);
        
        return cta;
    }
    
    /**
     * 
     */
    private class Visitor implements TreeAutomaton.BottomUpStateVisitor
    {
        
        /**
         * 
         */
        private final String prefix;
        
        /**
         * 
         */
        private final Int2ObjectMap<IntSet> vars;
        
        /**
         * 
         */
        private final ConcreteTreeAutomaton goal;
        
        /**
         * 
         */
        private final TreeAutomaton original;
        
        /**
         * 
         */
        private final RuleMarker rlm;
        

        /**
         * 
         * @param prefix
         * @param vars
         * @param goal 
         */
        public Visitor(String prefix, Int2ObjectMap<IntSet> vars, RuleMarker rlm,
                                        ConcreteTreeAutomaton goal, TreeAutomaton original) {
            this.prefix = prefix;
            this.vars = vars;
            this.goal = goal;
            this.original = original;
            this.rlm = rlm;
        }
        
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            
            Object st = this.original.getStateForId(state);
            String loopLabel = rlm.makeCode(this.vars.get(state),this.original,state);
            
            this.goal.addRule(this.goal.createRule(st, loopLabel, new Object[] {st}));
            
            if(original.getFinalStates().contains(state))
            {
                this.goal.addFinalState(state);
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
         * 
         * @param children
         * @return 
         */
        private Object[] makeCopy(int[] children) {
            Object[] obs = new Object[] {children.length};
            
            for (int i = 0; i < children.length; i++) {
                obs[i] = this.original.getStateForId(children[i]);
            }
            
            return obs;
        }
        
    }
    
    /**
     * 
     */
    private static class VariablePropagator implements Semiring<IntSet> {

        /**
         * 
         */
        private final static IntSet ZERO = new IntAVLTreeSet();
        
        @Override
        public IntSet add(IntSet x, IntSet y) {
            if(!x.equals(y)){
                throw new IllegalStateException("Variables dominated by states are not unique");
            }
            
            return x;
        }

        @Override
        public IntSet multiply(IntSet x, IntSet y) {
           IntIterator ii = x.iterator();
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