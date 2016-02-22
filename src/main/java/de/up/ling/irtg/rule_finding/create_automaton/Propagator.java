/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.Semiring;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
     * @param input
     * @param alignments
     * @return 
     */
    public Int2ObjectMap<IntSortedSet> propagate(TreeAutomaton input, StateAlignmentMarking alignments){
        Int2ObjectMap<IntSortedSet> map = input.evaluateInSemiring(vp, alignments);
        return map;
    }
    
    /**
     * 
     * @param markingSets
     * @return 
     */
    public static Set<String> turnToMarkers(Collection<IntSortedSet> markingSets) {
        Set<String> set = new HashSet<>();
        
        for(IntSortedSet iss : markingSets) {
            set.add(makeAlignmentString(iss));
        }
        
        return set;
    }
    
    
    /**
     * 
     * @param <State>
     * @param aut
     * @param markings
     * @param counterParts
     * @return 
     */
    public <State> TreeAutomaton<String> convert(TreeAutomaton<State> aut,
                                                Int2ObjectMap<IntSortedSet> markings,
                                                Set<String> counterParts){
        ConcreteTreeAutomaton<String> output = new ConcreteTreeAutomaton<>(aut.getSignature());
        
        Visitor visit = new Visitor(markings, output, aut, counterParts);
        
        aut.foreachStateInBottomUpOrder(visit);
        
        return output;
    }
    
    /**
     * 
     * @param variable
     * @return 
     */
    public static String getAlignments(String variable) {
        String all = makeParts(variable)[0].trim();
        
        return all;
    }

    /**
     * 
     * @param variable
     * @return 
     */
    private static String[] makeParts(String variable) {
        return Variables.getInformation(variable).split(" _@_ ");
    }
    
    /**
     * 
     * @param variable
     * @return 
     */
    public static String getStateDescription(String variable) {
        String[] q = makeParts(variable);
        if(q.length < 2) {
            return q[0].trim();
        }
        String all = q[1].trim();
        
        return all;
    }
    
    /**
     * 
     * @param alignments
     * @param state
     * @return 
     */
    public static String createVariableWithContent(String alignments, String state) {
        StringBuilder sc = new StringBuilder(alignments);
        sc.append(" _@_ ");
        sc.append(state);
        
        return Variables.createVariable(sc.toString());
    }

    /**
     * 
     * @param iit
     * @return 
     */
    private static String makeAlignmentString(IntIterable markers) {
        IntIterator iit = markers.iterator();
        
        StringBuilder sb = new StringBuilder();
        boolean first=true;
        while(iit.hasNext()) {
            if(first) {
                first = false;
            } else {
                sb.append(",");
            }
            
            sb.append(iit.nextInt());
        }
        
        return sb.toString();
    }
    
    
    /**
     * It is very important that we only use sorted sets here, so that equals
     * sets can be identified by their string representation being equal.
     */
    private static class VariablePropagator implements Semiring<IntSortedSet> {

        /**
         * The empty set used as a starting point.
         */
        private final static IntSortedSet ZERO = new IntAVLTreeSet();
        
        @Override
        public IntSortedSet add(IntSortedSet x, IntSortedSet y) {
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
        public IntSortedSet multiply(IntSortedSet x, IntSortedSet y) {
           IntIterator ii = x.iterator();
           
           // We do not want the same alignment marker twice in a derivation.
           while(ii.hasNext()){
                if(y.contains(ii.nextInt())){
                    throw new IllegalStateException("Adding a variable twice along one derivation path is"
                            + " against the rules for alignment markers; attempted for: "+x+" "+y);
                }
            }
           
            IntSortedSet set = new IntAVLTreeSet(x);
            set.addAll(y);
           
           return set;
        }

        @Override
        public IntSortedSet zero() {
            return ZERO;
        }
    };
    
    
    /**
     * A simple visitor used to construct the new automaton by copying the rules of the old
     * and introducing some extra labels.
     */
    private class Visitor implements TreeAutomaton.BottomUpStateVisitor
    {    
        /**
         * The variable sets we are aware of for the states.
         */
        private final Int2ObjectMap<IntSortedSet> vars;
        
        /**
         * the automaton we are constructing.
         */
        private final ConcreteTreeAutomaton<String> goal;
        
        /**
         * the original with which we started.
         */
        private final TreeAutomaton original;
        
        /**
         * 
         */
        private final Set<String> counterParts;

        /**
         * 
         * @param vars
         * @param goal
         * @param original
         * @param local
         * @param mark 
         */
        public Visitor(Int2ObjectMap<IntSortedSet> vars, ConcreteTreeAutomaton goal, TreeAutomaton original,
                Set<String> counterPart) {
            this.vars = vars;
            this.goal = goal;
            this.original = original;
            
            this.counterParts = counterPart;
        }

        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            String stateName = this.original.getStateForId(state).toString();
            int code = this.goal.addState(stateName);
            
            // here we add a loop for the alignments
            IntSortedSet propagatedAligments = this.vars.get(state);
            String lining = makeAlignmentString(propagatedAligments);
            if(this.counterParts.contains(lining)) {
                String varLabel = createVariableWithContent(lining, stateName);
                this.goal.addRule(this.goal.createRule(stateName, varLabel, new String[] {stateName}));
            }            
            
            if(original.getFinalStates().contains(state))
            {
                this.goal.addFinalState(code);
            }
            
            for(Rule r : rulesTopDown)
            {
                String[] arr = makeCopy(r.getChildren());
                String label = r.getLabel(original);
                
                double weight = r.getWeight();
                Rule newRule = goal.createRule(stateName, label, arr, weight);
                
                this.goal.addRule(newRule);
            }
        }

        /**
         * Creates a copy of a rules child states
         * 
         * @param children
         * @return 
         */
        private String[] makeCopy(int[] children) {
            String[] obs = new String[children.length];
            
            for (int i = 0; i < children.length; i++) {
                obs[i] = this.original.getStateForId(children[i]).toString();
            }
            
            return obs;
        }
    }
}