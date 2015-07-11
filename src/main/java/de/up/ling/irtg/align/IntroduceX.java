/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.Semiring;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author christoph
 */
public class IntroduceX {  
    
    /**
     * 
     */
    private final static Semiring<IntSet> VAR_PROPAGATOR = 
                                                new Semiring<IntSet>() {

        @Override
        public IntSet add(IntSet x, IntSet y) {
            if(x.equals(y))
            {
                return x;
            }
            
            throw new IllegalStateException("Variables dominated by states are not unique");
        }

        @Override
        public IntSet multiply(IntSet x, IntSet y) { 
           IntIterator ii = x.iterator();
           while(ii.hasNext())
           {
               if(y.contains(ii.nextInt()))
               {
                   throw new IllegalStateException("Adding a variable twice is against the rules for alignment"
                           + "markers");
               }
           }
           
           IntSet set = new IntAVLTreeSet(x);
           set.addAll(y);
           return set;
        }

        @Override
        public IntSet zero() {
            return new IntAVLTreeSet();
        }
    };
    
    /**
     * 
     * @param input
     * @param marks
     * @param setPrefix
     * @return 
     */
    public static Pair<TreeAutomaton,Set<String>> introduce(TreeAutomaton input, RuleMarker marks,
                                                             String setPrefix){
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton(input.getSignature());
        Int2ObjectMap<IntSet> mapping = input.evaluateInSemiring2(VAR_PROPAGATOR, marks);
        Set<String> specialSymbols = new TreeSet<>();
        
        Visitor vis = new Visitor(setPrefix, mapping, specialSymbols, cta, input);
        input.foreachStateInBottomUpOrder(vis);
        
        return new Pair<>(cta,specialSymbols);
    }
    
    /**
     * 
     */
    private static class Visitor implements TreeAutomaton.BottomUpStateVisitor
    {
        /**
         * 
         */
        private final Set<String> specialSymbols;
        
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
         * @param prefix
         * @param vars
         * @param goal 
         */
        public Visitor(String prefix, Int2ObjectMap<IntSet> vars, Set<String> special,
                                        ConcreteTreeAutomaton goal, TreeAutomaton original) {
            this.prefix = prefix;
            this.vars = vars;
            this.goal = goal;
            this.original = original;
            this.specialSymbols = special;
        }
        
        
        

        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            
            Object st = this.original.getStateForId(state);
            String loopLabel = this.prefix+this.vars.get(state);
            this.specialSymbols.add(loopLabel);
            
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
}