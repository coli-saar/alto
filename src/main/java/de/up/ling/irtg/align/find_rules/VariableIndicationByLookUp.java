/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;

/**
 *
 * @author christoph_teichmann
 */
public class VariableIndicationByLookUp implements VariableIndication {

    /**
     * 
     */
    private final HomomorphismManager hom;

    /**
     * 
     * @param hom 
     */
    public VariableIndicationByLookUp(HomomorphismManager hom) {
        this.hom = hom;
    }
    
    @Override
    public boolean isVariable(int label) {
        return hom.isVariable(label);
    }    

    @Override
    public boolean isIgnorableVariable(int label) {
        return false;//this.isVariable(label) && label != hom.getDefaultVariable();
    }
}