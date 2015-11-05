/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.rule_finding.Variables;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author christoph_teichmann
 * @param <Type1>
 */
public class JustXEveryWhere<Type1> implements VariableIntroduction<Type1, Type1> {

    @Override
    public AlignedTrees<Type1> apply(AlignedTrees<Type1> t) {
        ConcreteTreeAutomaton<Type1> cta =
                    new ConcreteTreeAutomaton<>(t.getTrees().getSignature());
        SpecifiedAligner<Type1> spa = new SpecifiedAligner<>(cta);
        
        TreeAutomaton<Type1> tBase = t.getTrees();
        StateAlignmentMarking<Type1> alBase = t.getAlignments();
        
        IntIterator iit = tBase.getReachableStates().iterator();
        List<Type1> children = new ArrayList<>();
        while(iit.hasNext()){
            int stat = iit.nextInt();
            Type1 state = tBase.getStateForId(stat);
            int s = cta.addState(state);
            if(tBase.getFinalStates().contains(stat)){
                cta.addFinalState(s);
            }
            
            spa.put(state, alBase.getAlignmentMarkers(state));
            children.clear();
            children.add(state);
            
            cta.addRule(cta.createRule(state, Variables.makeVariable(""), children));
            
            Iterable<Rule> rules = tBase.getRulesTopDown(stat);
            for(Rule r : rules){
                children.clear();
                for(int child : r.getChildren()){
                    children.add(tBase.getStateForId(child));
                }
                
                String label = tBase.getSignature().resolveSymbolId(r.getLabel());
                cta.addRule(cta.createRule(state, label, children));
            }
            
        }
        
        
        return new AlignedTrees<>(cta,spa);
    }
    
}
