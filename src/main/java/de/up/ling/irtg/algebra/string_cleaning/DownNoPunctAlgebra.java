/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.string_cleaning;

import de.up.ling.irtg.algebra.StringAlgebra;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class DownNoPunctAlgebra extends StringAlgebra {
    /**
     * 
     */
    private static final Pattern PUNCT = Pattern.compile("\\p{Punct}");
    
    /**
     * 
     */
    private static final Function<String,String> FUNCTION = (String input) -> {
        String result = input.toLowerCase();
        
        Matcher mat = PUNCT.matcher(result);
        
        return mat.replaceAll("").trim();
    };

    @Override
    public List<String> parseString(String representation) {
        representation = FUNCTION.apply(representation);
        return super.parseString(representation);
    }
}
