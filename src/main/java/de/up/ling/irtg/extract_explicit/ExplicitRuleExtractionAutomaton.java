/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extract_explicit;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.Tuple;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Iterator;

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
        
        IntIterator driverFinal = baseRules.finalIterator();
        //TODO
        
        
    }
    
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isBottomUpDeterministic() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * 
     */
    private class MathingFinalIterable implements Iterable<Type> {
        /**
         * 
         */
        private final RuleMatching<Type> rm;
        
        /**
         * 
         * @param rm 
         */
        public MathingFinalIterable(RuleMatching rm) {
            this.rm = rm;
        }
        
        @Override
        public Iterator<Type> iterator() {
            return new Iterator<Type>() {
                /**
                 * 
                 */
                private final IntIterator mat = rm.finalStates();
                
                
                
                @Override
                public boolean hasNext() {
                    return mat.hasNext();
                }

                @Override
                public Type next() {
                    return rm.getStateForID(mat.nextInt());
                }
            };
        }
        
    }
    
    /**
     * 
     */
    private class DriverFinalIterable implements Iterable<Type> {
        /**
         * 
         */
        private final RuleDriver<Type> driv;
        
        /**
         * 
         * @param driv 
         */
        public DriverFinalIterable(RuleDriver<Type> driv) {
            this.driv = driv;
        }
        
        @Override
        public Iterator<Type> iterator() {
            return new Iterator<Type>() {
                private final IntIterator di = driv.finalIterator();
                
                
                @Override
                public boolean hasNext() {
                    return this.di.hasNext();
                }

                @Override
                public Type next() {
                    return driv.getStateForID(this.di.nextInt());
                }
            };
        }
    }
}
