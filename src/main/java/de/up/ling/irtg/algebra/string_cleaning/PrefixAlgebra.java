/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.string_cleaning;

import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class PrefixAlgebra extends DownNoPunctAlgebra {
    
    /**
     * 
     */
    private final Function<String,String> trimmer;
    
    /**
     * 
     * @param maxSize 
     */
    public PrefixAlgebra(final int maxSize) {
        trimmer = (String input) -> {
            StringBuilder sb = new StringBuilder();
            
            int length = input.length();
            int count = 0;
            for(int pos=0;pos<length;) {
                int codePoint = input.codePointAt(pos);
                
                if(Character.isWhitespace(codePoint)){
                    count = 0;
                    sb.append(Character.toChars(codePoint));
                }else {
                    if(count < maxSize) {
                        sb.append(Character.toChars(codePoint));
                    }
                    
                    ++count;
                }
                
                pos += Character.charCount(codePoint);
            }
            
            return sb.toString();
        };
    }
    
    /**
     * 
     */
    public PrefixAlgebra() {
        this(4);
    }

    @Override
    public List<String> parseString(String representation) {
        return super.parseString(trimmer.apply(representation));
    }   
}
