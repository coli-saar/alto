/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.hom.Homomorphism;

/**
 *
 * @author christoph_teichmann
 */
public class VariableIndicationByLookUp implements VariableIndication {

    /**
     * 
     */
    private final Homomorphism hom;

    /**
     * 
     * @param hom 
     */
    public VariableIndicationByLookUp(Homomorphism hom) {
        this.hom = hom;
    }
    
    @Override
    public boolean isVariable(int label) {
        return HomomorphismManager.VARIABLE_PATTERN.test(hom.getTargetSignature()
                .resolveSymbolId(hom.get(label).getLabel().getValue()));
    }    
}