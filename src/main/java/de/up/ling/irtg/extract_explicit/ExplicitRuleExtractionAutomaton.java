/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extract_explicit;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.IntTupleIterator;
import de.up.ling.irtg.util.Tuple;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public class ExplicitRuleExtractionAutomaton<Type> extends TreeAutomaton<Tuple<Type>> {
    /**
     * 
     */
    private final TreeGroupInterner ruleNames;
    
    /**
     * 
     */
    private final RuleDriver<Type> mainInput;
    
    /**
     * 
     */
    private final RuleMatching<Type>[] paired;
    
    /**
     * 
     */
    private IntSet topDownDone = new IntOpenHashSet();
    
    /**
     * 
     * @param ruleNames
     * @param baseRules
     * @param translations 
     */
    public ExplicitRuleExtractionAutomaton(TreeGroupInterner ruleNames, RuleDriver<Type> baseRules,
                                            RuleMatching<Type>... translations) {
        super(ruleNames.getSignature());
        
        this.ruleNames = ruleNames;
        this.mainInput = baseRules;
        
        if(translations == null) {
            this.paired = new RuleMatching[0];
        } else {
            this.paired = translations;
        }
        
        List<IntIterable> finals = new ArrayList<>();
        finals.add(mainInput.getAutomaton().getFinalStates());
        
        for(RuleMatching<Type> rm : translations) {
            finals.add(rm.getAutomaton().getFinalStates());
        }
        
        List<Type> tup = new ArrayList<>();
        List<AlignmentInformation> li = new ArrayList<>();
        IntTupleIterator iti = new IntTupleIterator(finals);
        while(iti.hasNext()) {
            int[] states = iti.next();
            
            tup.clear();
            li.clear();
            tup.add(this.mainInput.getAutomaton().getStateForId(states[0]));
            li.add(this.mainInput.getAlignmentInformation());
            for(int i=1;i<states.length;++i) {
                tup.add(this.paired[i-1].getAutomaton().getStateForId(states[i]));
                li.add(this.paired[i-1].getAlignmentInformation());
            }
            
            if(!checkMatching(li)) {
                continue;
            }
            
            Tuple<Type> t = new Tuple(finals);
            int state = this.addState(t);
            
            this.addFinalState(state);
        }
    }
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        if(!topDownDone.contains(parentState)) {
            this.getRulesTopDown(parentState);
        }
        
        if(!this.useCachedRuleTopDown(labelId, parentState)) {
            return new ArrayList<>();
        } else {
            return this.getRulesTopDownFromExplicit(labelId, parentState);
        }
    }

    @Override
    public boolean isBottomUpDeterministic() {
        boolean b  = true;
        
        b &= this.mainInput.getAutomaton().isBottomUpDeterministic();
        for(RuleMatching<Type> rm : this.paired) {
            b &= rm.getAutomaton().isBottomUpDeterministic();
        }
        
        return b;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return false;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return true;
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int parentState) {
        this.topDownDone.add(parentState);
        
        //TODO
        Tuple<Type> tuple = this.getStateForId(parentState);
        
        List<Iterable<Tree<HomomorphismSymbol>>> images = new ArrayList<>(); 
        
        
        
        
        return super.getRulesTopDown(parentState); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param li
     * @return 
     */
    private boolean checkMatching(List<AlignmentInformation> alignmentInformation) {
        for(int i=0;i<alignmentInformation.size();++i) {
            AlignmentInformation alig = alignmentInformation.get(i);
            
            for(int j=0;j<alignmentInformation.size();++j) {
                AlignmentInformation other = alignmentInformation.get(j);
                
                if(!alig.matches(other)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
