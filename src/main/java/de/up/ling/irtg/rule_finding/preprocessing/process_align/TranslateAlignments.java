/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.process_align;

import java.util.function.BinaryOperator;

/**
 *
 * @author christoph_teichmann
 */
public class TranslateAlignments {
    
    /**
     * 
     */
    private final BinaryOperator<String> converterOne;
    
    /**
     * 
     */
    private final BinaryOperator<String> converterTwo;

    /**
     * 
     * @param converterOne
     * @param converterTwo 
     */
    public TranslateAlignments(BinaryOperator<String> converterOne, BinaryOperator<String> converterTwo) {
        this.converterOne = converterOne;
        this.converterTwo = converterTwo;
    }
    
    /**
     * 
     * @param lineOne
     * @param lineTwo
     * @param alignments
     * @return 
     */
    public String transform(String lineOne, String lineTwo, String alignments){
        StringBuilder sb = new StringBuilder();
        sb.append(lineOne);
        sb.append('\n');
        sb.append(lineTwo);
        sb.append('\n');
        
        sb.append(this.converterOne.apply(lineOne, alignments)).append("\n");
        sb.append(this.converterTwo.apply(lineTwo, alignments));
        
        return sb.toString();
    }
}
