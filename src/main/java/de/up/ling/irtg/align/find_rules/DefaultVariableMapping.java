/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import com.google.common.base.Function;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author christoph_teichmann
 */
public class DefaultVariableMapping implements Function<Rule,Integer> {
    /**
     * 
     */
    private final HomomorphismManager homa;

    /**
     * 
     * @param homa 
     */
    public DefaultVariableMapping(HomomorphismManager homa) {
        this.homa = homa;
    }
       
    @Override
    public Integer apply(Rule input) {
        int label = input.getLabel();
        
        if(homa.isVariable(label)){
            return homa.getDefaultVariable();
        }else{
            return label;
        }
    }
}