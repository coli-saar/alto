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
    private final String setPrefix;
    
    /**
     * 
     * @param setPrefixMarker 
     */
    public IntroduceX(String setPrefixMarker)
    {
        this.setPrefix = setPrefixMarker;
    }
    
    /**
     * 
     * @param input
     * @param rlm
     * @return 
     */
    public TreeAutomaton introduce(TreeAutomaton input, RuleMarker rlm){
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton(input.getSignature());
        
        Semiring<IntSet[]> sr = new VariablePropagator(rlm.width());
        
        Int2ObjectMap<IntSet[]> mapping = input.evaluateInSemiring2(sr, rlm);
        
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
        private final Int2ObjectMap<IntSet[]> vars;
        
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
        public Visitor(String prefix, Int2ObjectMap<IntSet[]> vars, RuleMarker rlm,
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
    private class VariablePropagator implements Semiring<IntSet[]> {
        
        /**
         * 
         */
        private final int width;
        
        VariablePropagator(int width)
        {
            this.width = width;
        }

        @Override
        public IntSet[] add(IntSet[] x, IntSet[] y) {
            
            if(x.length != y.length)
            {
                throw new IllegalStateException("non-equal alignment groups");
            }
            
            for(int i = 0;i<x.length;++i)
            {
                IntSet a = x[i];
                IntSet b = x[i];
                
                if(!a.equals(b))
                {
                    throw new IllegalStateException("Variables dominated by states are not unique");
                }
            }
            
            return x;
        }

        @Override
        public IntSet[] multiply(IntSet[] x, IntSet[] y) {
            
           if(x.length != y.length)
           {
               throw new IllegalStateException("non-equal alignment groups");
           }
            
           IntSet[] ret = new IntSet[x.length];
            
            for(int i=0;i<x.length;++i)
            {
                IntSet a = x[i];
                IntSet b = x[i];
                
                IntIterator ii = a.iterator();
                while(ii.hasNext())
                {
                    if(b.contains(ii.nextInt()))
                    {
                        throw new IllegalStateException("Adding a variable twice is against the rules for alignment"
                           + "markers; attempted for: "+a+" "+b);
                    }
                }
           
                IntSet set = new IntAVLTreeSet(a);
                set.addAll(b);
                
                ret[i] = set;
            }
           
           return ret;
        }

        @Override
        public IntSet[] zero() {
            IntSet[] ret = new IntSet[width];
            
            for(int i=0;i<ret.length;++i)
            {
                ret[i] = new IntAVLTreeSet();
            }
            
            return ret;
        }
    };
}