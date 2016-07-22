/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author groschwitz
 */
public class TAAOperationWrapperVariable extends TAAOperationWrapper {

    private String varName;
    
    public TAAOperationWrapperVariable () {
        super(0, null, null);
        varName = "";
    }
    
    public TAAOperationWrapperVariable (String varName) {
        super(0, null, null);
        this.varName = varName;
    }
    
    public void setVarName(String newVarName) {
        varName = newVarName;
    }
    
    @Override
    protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "Variable: ?"+varName;
    }

    @Override
    public String getCode() {
        return "?"+varName;
    }
    
    
}
