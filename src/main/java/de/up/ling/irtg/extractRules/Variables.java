/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class Variables {
    
    /**
     * 
     */
    private static final String variablePrefix = "X";
    
    /**
     * 
     */
    public static final Predicate<String> isVariable = Pattern.compile("^X.*").asPredicate();
    
    /**
     * 
     * @param additionalInformation
     * @return 
     */
    public String makeVariable(Object... additionalInformation){
        StringBuffer sb = new StringBuffer(variablePrefix);
        
        for(Object o : additionalInformation){
            sb.append("_").append(o.toString());
        }
        
        return sb.toString();
    }
    
    /**
     * 
     * @param original
     * @param additionalInformation
     * @return 
     */
    public String makeVariable(String original, Object... additionalInformation){
        StringBuffer sb;
        
        if(isVariable.test(original)){
            sb = new StringBuffer(original);
        }else{
            sb = new StringBuffer(variablePrefix).append("_").append(original);
        }
        
        for(Object o : additionalInformation){
            sb.append("_").append(o.toString());
        }
        
        return sb.toString();
    }
}
