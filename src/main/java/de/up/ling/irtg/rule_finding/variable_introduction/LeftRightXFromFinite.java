/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.saar.basic.Pair;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.TupleIterator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author christoph_teichmann
 * @param <Type1>
 */
public class LeftRightXFromFinite<Type1> 
        implements VariableIntroduction<Type1,Pair<Type1, Pair<String, String>>>{

    /**
     * 
     * @param cta 
     */
    private void addVariables(ConcreteTreeAutomaton<Pair<Type1, Pair<String, String>>> cta) {
        IntIterator iit = cta.getReachableStates().iterator();
        while(iit.hasNext()){
            int state = iit.nextInt();
            Pair<Type1,Pair<String,String>> pair = cta.getStateForId(state);
            
            String var = Variables.makeVariable(pair.getRight().getLeft()+"_"+pair.right.getRight());
            int code = cta.getSignature().addSymbol(var, 1);
            
            Rule r = cta.createRule(state, code, new int[] {state}, 1.0);
            cta.addRule(r);
        }
    }

    /**
     * 
     * @param r
     * @param map
     * @param holder
     * @param parent
     * @param cta
     * @param tBase
     * @param lCode
     * @param spa
     * @param align 
     */
    private void handleNonUnary(Rule r, Map<Integer, Set<Pair<String, String>>> map,
            List<Set<Pair<String, String>>> holder, Type1 parent,
            ConcreteTreeAutomaton<Pair<Type1, Pair<String, String>>> cta,
            TreeAutomaton<Type1> tBase, int lCode,
        SpecifiedAligner<Pair<Type1, Pair<String, String>>> spa, IntSet align) {
        holder.clear();
        
        for(int k : r.getChildren()){
            Set<Pair<String,String>> sides = map.get(k);
            
            holder.add(sides);
        }
        
        Iterable<Pair<String,String>>[] ar = holder.toArray(new Set[holder.size()]);
        TupleIterator<Pair<String,String>> tups = new TupleIterator<>(ar);
        
        while(tups.hasNext()){
            Pair<String,String>[] arr = tups.next();
            
            Pair<String,String> parentPair =
                    new Pair<>(arr[0].getLeft(),arr[arr.length-1].getRight());
            int parState = this.makeState(parent, parentPair, cta, tBase, spa, align);
            
            int[] children = new int[r.getArity()];
            for(int i=0;i<r.getChildren().length;++i){
                Pair<String,String> childPair = arr[i];
                
                Type1 stat = tBase.getStateForId(r.getChildren()[i]);
                int code = this.makeState(stat, childPair, cta, tBase, spa, null);
                
                children[i] = code;
            }
            
            Rule nr = cta.createRule(parState, lCode, children, r.getWeight());
            cta.addRule(nr);
        }
    }

    /**
     * 
     * @param label
     * @param parent
     * @param cta
     * @param lCode
     * @param r
     * @param basis 
     */
    private void handleUnaryRule(String label, Type1 parent,
            ConcreteTreeAutomaton<Pair<Type1, Pair<String, String>>> cta,
            int lCode, Rule r, TreeAutomaton<Type1> basis,
            SpecifiedAligner<Pair<Type1, Pair<String, String>>> spa,
            IntSet align) {
        Pair<String,String> lab = new Pair<>(label,label);
        
        int par = makeState(parent, lab, cta, basis, spa, align);
        Rule nr = cta.createRule(par, lCode, new int[] {}, r.getWeight());
        cta.addRule(nr);
    }

    /**
     * 
     * @param parent
     * @param boundaries
     * @param cta
     * @param basis
     * @param parentCode
     * @return 
     */
    private int makeState(Type1 parent, Pair<String, String> boundaries,
            ConcreteTreeAutomaton<Pair<Type1, Pair<String, String>>> cta,
            TreeAutomaton<Type1> basis,
            SpecifiedAligner<Pair<Type1, Pair<String, String>>> spa, IntSet al) {
        Pair<Type1,Pair<String,String>> state = new Pair<>(parent,boundaries);
        
        int par = cta.addState(state);
        
        if(al != null){
            spa.put(state, al);
        }
        
        int parentCode = basis.getIdForState(parent);
        
        if(basis.getFinalStates().contains(parentCode)){
            cta.addFinalState(par);
        }
        
        return par;
    }

    @Override
    public AlignedTrees<Pair<Type1, Pair<String, String>>> apply(AlignedTrees<Type1> t) {
        ConcreteTreeAutomaton<Pair<Type1,Pair<String,String>>> cta =
                    new ConcreteTreeAutomaton<>(t.getTrees().getSignature());
        SpecifiedAligner<Pair<Type1, Pair<String, String>>> spa = new SpecifiedAligner<>(cta);
        
        TreeAutomaton<Type1> tBase = t.getTrees();
        StateAlignmentMarking<Type1> alBase = t.getAlignments();
        
        RuleEvaluator<Set<Pair<String,String>>> re = new Evaluator(tBase.getSignature());
        Semiring<Set<Pair<String,String>>> sem = new LeftRightSemiRing();
        
        Map<Integer,Set<Pair<String,String>>> map = tBase.evaluateInSemiring(sem, re);
        Iterable<Rule> it = tBase.getAllRulesTopDown();
        
        List<Set<Pair<String,String>>> holder = new ArrayList<>();
        for(Rule r : it){
            Type1 parent = tBase.getStateForId(r.getParent());
            
            IntSet al;
            if(alBase == null){
                al  = new IntOpenHashSet();
            }else{
                al = alBase.getAlignmentMarkers(parent);
            }
            
            String label = tBase.getSignature().resolveSymbolId(r.getLabel());
            int lCode = cta.getSignature().addSymbol(label, r.getArity());
            
            if(r.getArity() == 0){
                handleUnaryRule(label, parent, cta, lCode, r, tBase, spa, al);
                continue;
            }
            
            handleNonUnary(r, map, holder, parent, cta, tBase, lCode, spa, al);
        }
        
        
        addVariables(cta);
        
        
        return new AlignedTrees<>(cta,spa);
    }
    
    /**
     * 
     */
    private static class Evaluator implements RuleEvaluator<Set<Pair<String,String>>>{
        /**
         * 
         */
        private final Signature sig;

        /**
         * 
         * @param sig 
         */
        public Evaluator(Signature sig) {
            this.sig = sig;
        }
        
        @Override
        public Set<Pair<String, String>> evaluateRule(Rule rule) {
            Set<Pair<String,String>> s = new HashSet<>();
            
            int label = rule.getLabel();
            if(sig.getArity(label) == 0){
                String lab = sig.resolveSymbolId(label);
                
                s.add(new Pair<>(lab,lab));
                return s;
            }else{
                return s;
            }
        }
    }
    
    /**
     * 
     */
    private static class LeftRightSemiRing implements Semiring<Set<Pair<String,String>>>{

        @Override
        public Set<Pair<String, String>> add(Set<Pair<String, String>> x, Set<Pair<String, String>> y) {
            Set<Pair<String,String>> ret = new HashSet<>(x);
            ret.addAll(y);
            
            return ret;
        }

        @Override
        public Set<Pair<String, String>> multiply(Set<Pair<String, String>> x, Set<Pair<String, String>> y) {
            if(x.isEmpty()){
                return y;
            }else if(y.isEmpty()){
                return x;
            }else{
                Set<Pair<String,String>> pairs = new HashSet<>();
                
                x.stream().forEach((p1) -> {
                    y.stream().forEach((p2) -> {
                        pairs.add(new Pair<>(p1.getLeft(),p2.getRight()));
                    });
                });
                
                return pairs;
            }
        }

        @Override
        public Set<Pair<String, String>> zero() {
            return new HashSet<>();
        }
    }
}
