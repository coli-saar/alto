/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extract_explicit;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public interface RuleDriver<Type> {
    /**
     * 
     * @return 
     */
    public TreeAutomaton<Type> getAutomaton();
    
    /**
     * 
     * @return 
     */
    public AlignmentInformation getAlignmentInformation();
}
