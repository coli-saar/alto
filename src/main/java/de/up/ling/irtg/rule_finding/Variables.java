/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

/**
 *
 * @author christoph_teichmann
 */
public class Variables {
    /**
     * 
     */
    public static final String VARIABLE_PREFIX = "__X__";
    
    /**
     * 
     * @param input
     * @return 
     */
    public static boolean isVariable(String input) {
        return input.startsWith(VARIABLE_PREFIX);
    }
    
    /**
     * 
     * @param input
     * @return 
     */
    public static String getInformation(String input) {
        return input.substring(VARIABLE_PREFIX.length()+1, input.length()-1);
    }
    
    /**
     * 
     * @param input
     * @return 
     */
    public static String createVariable(String input) {
        return VARIABLE_PREFIX+"{"+input+"}";
    }
}
