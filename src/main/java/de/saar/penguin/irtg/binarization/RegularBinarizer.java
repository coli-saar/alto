/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.binarization;

import de.saar.basic.StringOrVariable;
import de.saar.penguin.irtg.algebra.Algebra;
import de.saar.penguin.irtg.automata.ConcreteTreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class RegularBinarizer<E> {
    protected Algebra inputAlgebra, outputAlgebra;
    private int nextGensym;

    public RegularBinarizer(Algebra inputAlgebra, Algebra outputAlgebra) {
        this.inputAlgebra = inputAlgebra;
        this.outputAlgebra = outputAlgebra;
        nextGensym = 1;
    }
    
    public abstract TreeAutomaton<E> binarize(String symbol);
    
    public TreeAutomaton<String> binarize(Tree<String> term) {
        return null;
    }
    
    public TreeAutomaton<String> binarizeWithVariables(Tree<StringOrVariable> term) {
        if( term.getLabel().isVariable() ) {
            
        } else {
            TreeAutomaton<String> ret = rename(binarize(term.getLabel().getValue()));
            
//            for( Tree<StringOrVariable> )
        }
        
        
        
        return null;
    }

    public Algebra getInputAlgebra() {
        return inputAlgebra;
    }

    public Algebra getOutputAlgebra() {
        return outputAlgebra;
    }
    
    private void renameStates(TreeAutomaton<String> automaton, Set<String> statesToRename, String newState) {
        
    }
    
    private Set<String> copyWithRenaming(TreeAutomaton<E> automaton, ConcreteTreeAutomaton<String> intoAutomaton) {
        Map<E,String> stateNameMap = new HashMap<E, String>();
        
        for( Rule<E> rule : automaton.getRuleSet() ) {
            String[] newChildren = new String[rule.getChildren().length];
            for( int i = 0; i < rule.getChildren().length; i++ ) {
                newChildren[i] = getOrGenState(rule.getChildren()[i], stateNameMap);
            }
            
            Rule<String> newRule = new Rule<String>(getOrGenState(rule.getParent(), stateNameMap), rule.getLabel(), newChildren);
            intoAutomaton.addRule(newRule);
        }
        
        Set<String> ret = new HashSet<String>();
        
        for( E state : automaton.getFinalStates() ) {
            ret.add(getOrGenState(state, stateNameMap));
        }
        
        return ret;
    }
    
    private TreeAutomaton<String> rename(TreeAutomaton<E> automaton) {
        Map<E,String> stateNameMap = new HashMap<E, String>();
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        
        for( Rule<E> rule : automaton.getRuleSet() ) {
            String[] newChildren = new String[rule.getChildren().length];
            for( int i = 0; i < rule.getChildren().length; i++ ) {
                newChildren[i] = getOrGenState(rule.getChildren()[i], stateNameMap);
            }
            
            Rule<String> newRule = new Rule<String>(getOrGenState(rule.getParent(), stateNameMap), rule.getLabel(), newChildren);
            ret.addRule(newRule);
        }
        
        for( E state : automaton.getFinalStates() ) {
            ret.addFinalState(getOrGenState(state, stateNameMap));
        }
        
        return ret;
    }
    
    private String getOrGenState(E state, Map<E,String> stateMap) {
        if( stateMap.containsKey(state)) {
            return stateMap.get(state);
        } else {
            String ret = gensym();
            stateMap.put(state, ret);
            return ret;
        }
    }
    
    private String gensym() {
        return "q" + (nextGensym++);
    }
}


