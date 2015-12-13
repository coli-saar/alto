/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author christoph
 */
public class BerkleyToAltoTree {
    /**
     * 
     */
    private final static Pattern NON_TERMINAL = Pattern.compile("\\((\\S*)");
    
    /**
     * 
     */
    private final static Pattern TERMINAL = Pattern.compile("\\)");
    
    /**
     * 
     */
    private final static Pattern SEQ_WHITE_SPACE = Pattern.compile("\\s+");
    
    /**
     * 
     */
    private final static Pattern QUOTES = Pattern.compile("'");
    
    
    private final static Pattern QUOTE_ALL = Pattern.compile("[^\\(\\)\\s]+");
    
    /**
     * 
     * @param in
     * @return 
     */
    public static String convert(final String in) {
        Matcher mat = NON_TERMINAL.matcher(in);
        String result = mat.replaceAll(" $1( ");
        
        mat = TERMINAL.matcher(result);
        result = mat.replaceAll(" )");
        
        mat = SEQ_WHITE_SPACE.matcher(result);
        result = mat.replaceAll(" ");
        
        mat = QUOTES.matcher(result);
        result = mat.replaceAll("__QUOTE__");
        
        mat = QUOTE_ALL.matcher(result);
        result = mat.replaceAll("'$0'");
        
        return result.trim();
    }    
}
