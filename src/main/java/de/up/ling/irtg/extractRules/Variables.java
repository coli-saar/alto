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
    public static String makeVariable(Object... additionalInformation){
        StringBuilder sb = new StringBuilder(VARIABLE_PREFIX);
        
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
    public static String makeVariable(String original, Object... additionalInformation){
        StringBuffer sb;
        
        if(IS_VARIABLE.test(original)){
            sb = new StringBuffer(original);
        }else{
            sb = new StringBuffer(VARIABLE_PREFIX).append("_").append(original);
        }
        
        for(Object o : additionalInformation){
            sb.append("_").append(o.toString());
        }
        
        return sb.toString();
    }
}
