/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

/**
 *
 * @author Jonas
 */
public class VariableNotDefinedException extends Exception {
    
    public VariableNotDefinedException(String variableName) {
        super("Variable " +variableName + " is undefined in this program and was not found in the variable remapper");
    }
    
}
