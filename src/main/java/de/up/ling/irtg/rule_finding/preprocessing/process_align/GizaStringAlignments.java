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
public class GizaStringAlignments implements BinaryOperator<String> {
    /**
     * 
     */
    private final boolean useLeft;

    /**
     * 
     * @param useLeft 
     */
    public GizaStringAlignments(boolean useLeft) {
        this.useLeft = useLeft;
    }
    
    @Override
    public String apply(String input, String alignments) {
        String[] parts = alignments.split("\\s+");
        
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<parts.length;++i){
            if(i != 0){
                sb.append(' ');
            }
            String[] p = parts[i].split("-");
            int pos = Integer.parseInt(p[this.useLeft ? 0 : 1].trim());
            
            sb.append(Integer.toString(pos)).append(':').append(Integer.toString(pos+1))
                    .append(':').append(Integer.toString(i+1));
        }
        
        return sb.toString().trim();
    }
}
