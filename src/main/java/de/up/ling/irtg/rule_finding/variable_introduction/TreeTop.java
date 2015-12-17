/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public class TreeTop<Type> implements VariableIntroduction<Type, Type> {

    @Override
    public AlignedTrees<Type> apply(AlignedTrees<Type> t) {
        TreeAutomaton<Type> ta = t.getTrees();
        Int2ObjectMap<String> map = new Int2ObjectOpenHashMap<>();
        
        ta.foreachStateInBottomUpOrder((int state, Iterable<Rule> rules) -> {
            Iterator<Rule> ru = rules.iterator();
            if(ru.hasNext()) {
                Rule r = ru.next();
                if(r.getArity() == 0) {
                    map.put(state, ta.getSignature().resolveSymbolId(r.getLabel())+"||term");
                }else {
                    String label = ta.getSignature().resolveSymbolId(r.getLabel());
                    if(r.getArity() == 2) {
                        switch(label) {
                            case MinimalTreeAlgebra.LEFT_INTO_RIGHT:
                                String child = map.get(r.getChildren()[1]);
                                map.put(state, child.split("\\|\\|")[0]+"||treeType");
                                
                                break;
                            case MinimalTreeAlgebra.RIGHT_INTO_LEFT:
                                child = map.get(r.getChildren()[0]);
                                map.put(state, child.split("\\|\\|")[0]+"||treeType");
                                
                                break;
                            default:
                                map.put(state, "");
                        }
                        
                    }else {
                        map.put(state, "");
                    }
                }
            }else {
              map.put(state, "");
            }
        });
        
        
        ConcreteTreeAutomaton<Type> cta = new ConcreteTreeAutomaton<>(ta.getSignature());
        SpecifiedAligner<Type> spec = new SpecifiedAligner<>(cta);
        
        ta.foreachStateInBottomUpOrder((int state, Iterable<Rule> rules) -> {
            Type sta = ta.getStateForId(state);
            int parent = cta.addState(sta);
            if(ta.getFinalStates().contains(state)) {
                cta.addFinalState(parent);
            }
            
            int label = cta.getSignature().addSymbol(Variables.makeVariable(map.get(state)), 1);
            
            cta.addRule(cta.createRule(parent, label, new int[] {parent}, 1.0));
            
            spec.put(sta, t.getAlignments().getAlignmentMarkers(sta));
            
            for(Rule rule : rules) {
                int[] children = new int[rule.getArity()];
                int pos = 0;
                for(int child : rule.getChildren()) {
                    children[pos++] = cta.addState(ta.getStateForId(child));
                }
                
                cta.addRule(cta.createRule(parent, rule.getLabel(), children, rule.getWeight()));
            }
        });
        
        return new AlignedTrees<>(cta,spec);
    }
}
