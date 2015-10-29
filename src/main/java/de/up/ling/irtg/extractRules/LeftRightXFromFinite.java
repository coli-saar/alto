/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules;

import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.align.alignment_marking.SpecifiedAligner;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.irtg.signature.Signature;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author christoph_teichmann
 */
public class LeftRightXFromFinite<Type1> implements 
                   Function<Pair<TreeAutomaton<Type1>,StateAlignmentMarking<Type1>>,
                            Pair<TreeAutomaton<Pair<Type1,String>>,StateAlignmentMarking<Pair<Type1,String>>>> {

    
    @Override
    public Pair<TreeAutomaton<Pair<Type1, String>>, StateAlignmentMarking<Pair<Type1, String>>> apply(Pair<TreeAutomaton<Type1>, StateAlignmentMarking<Type1>> t) {
        ConcreteTreeAutomaton<Pair<Type1,String>> cta = new ConcreteTreeAutomaton<>(t.getKey().getSignature());
        SpecifiedAligner<Pair<Type1,String>> spa = new SpecifiedAligner<>(cta);
        
        TreeAutomaton<Type1> tBase = t.getKey();
        StateAlignmentMarking<Type1> alBase = t.getValue();
        
        RuleEvaluator<Set<Pair<String,String>>> re = new Evaluator(tBase.getSignature());
        Semiring<Set<Pair<String,String>>> sem = new LeftRightSemiRing();
        
        Map<Integer,Set<Pair<String,String>>> map = tBase.evaluateInSemiring(sem, re);
        
        
        
        //TODO
        return new Pair<>(cta,spa);
    }
    
    
    
    
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
            Set<Pair<String,String>> s = new TreeSet<>();
            
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
            Set<Pair<String,String>> ret = new TreeSet<>(x);
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
                Set<Pair<String,String>> pairs = new TreeSet<>();
                
                x.stream().forEach((p1) -> {
                    y.stream().forEach((p2) -> {
                        pairs.add(new Pair<>(p1.getKey(),p2.getValue()));
                    });
                });
                
                return pairs;
            }
        }

        @Override
        public Set<Pair<String, String>> zero() {
            return new TreeSet<>();
        }
    }
}
