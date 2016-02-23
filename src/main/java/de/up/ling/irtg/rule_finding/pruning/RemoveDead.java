/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.AndOrSemiring;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveDead {
    
    /**
     * 
     * @param <State>
     * @param toReduce
     * @return 
     */
    public static <State> TreeAutomaton<State> reduce(TreeAutomaton<State> toReduce){
        ConcreteTreeAutomaton<State> cta = new ConcreteTreeAutomaton<>(toReduce.getSignature());
        
        IntSet terminating = findTerminating(toReduce);
        
        makeFinalState(toReduce, terminating, cta);
        
        Iterable<Rule> it = toReduce.getAllRulesTopDown();
        List<State> l = new ObjectArrayList<>();
        outer : for(Rule r : it){
            if(!terminating.contains(r.getParent())){
                continue;
            }
            
            for(int i : r.getChildren()){
                if(!terminating.contains(i)){
                    continue outer;
                }
            }
            
            State parent = toReduce.getStateForId(r.getParent());
            String label = toReduce.getSignature().resolveSymbolId(r.getLabel());
            l.clear();
            for(int i : r.getChildren()){
                l.add(toReduce.getStateForId(i));
            }
            
            cta.addRule(cta.createRule(parent, label, l, r.getWeight()));
        }
       
        return cta;
    }

    /**
     * 
     * @param toReduce
     * @return 
     */
    private static IntSet findTerminating(TreeAutomaton toReduce) {
        IntSet terminating = new IntOpenHashSet();
        
        IntSet seen = new IntOpenHashSet();
        
        ArrayList<Rule> todo = new ArrayList<>();
        IntIterator fin = toReduce.getFinalStates().iterator();
        Consumer<Rule> appendFirst = (Rule r) -> {
            todo.add(r);
        };
        
        while(fin.hasNext()) {
            int state = fin.nextInt();
            seen.add(state);
            
            toReduce.foreachRuleTopDown(state, appendFirst);
        }
        visitAll(todo, terminating, seen, toReduce, appendFirst);
        
        return terminating;
    }

    /**
     * 
     * @param todo
     * @param terminating
     * @param seen
     * @param toReduce
     * @param appendFirst 
     */
    private static void visitAll(ArrayList<Rule> todo, IntSet terminating, IntSet seen,
            TreeAutomaton toReduce, Consumer<Rule> appendFirst) {
        outerLoop : while(!todo.isEmpty()) {
            Rule r = todo.remove(todo.size()-1);
            
            if(terminating.contains(r.getParent())) {
                continue;
            }
            
            boolean term = true;
            for(int i=0;i<r.getArity();++i) {
                int child = r.getChildren()[i];
                
                if(seen.contains(child)) {
                    term &= terminating.contains(child);
                } else {
                    seen.add(child);
                    todo.add(r);
                    
                    toReduce.foreachRuleTopDown(child, appendFirst);
                    
                    continue outerLoop;
                }
            }
            
            if(term) {
                terminating.add(r.getParent());
            }
        }
    }

    /**
     * 
     * @param <State>
     * @param toReduce
     * @param idm
     * @param cta 
     */
    private static <State> void makeFinalState(TreeAutomaton<State> toReduce, IntSet idm,
                                                        ConcreteTreeAutomaton<State> cta) {
        IntIterator iit = toReduce.getFinalStates().iterator();
        while(iit.hasNext()){
            int state = iit.nextInt();
            
            if(idm.contains(state)){
                cta.addFinalState(cta.addState(toReduce.getStateForId(state)));
            }
        }
    }
}