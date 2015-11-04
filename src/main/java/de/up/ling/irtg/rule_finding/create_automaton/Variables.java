/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import java.util.function.Predicate;

/**
 *
 * @author christoph_teichmann
 */
public class Variables {
    
    /**
     * 
     */
    private static final String VARIABLE_PREFIX = "X";
    
    /**
     * 
     */
    public static final Predicate<String> IS_VARIABLE = 
            (String t) -> t.codePointAt(0) == VARIABLE_PREFIX.codePointAt(0);
    
    /**
     * 
     * @param additionalInformation
     * @return 
     */
    public static String makeVariable(String additionalInformation){
        return VARIABLE_PREFIX+additionalInformation;
    }
    
    /**
     * 
     * @param variable
     * @return 
     */
    public static String getInformation(String variable){
        return variable.substring(2);
    }
}
