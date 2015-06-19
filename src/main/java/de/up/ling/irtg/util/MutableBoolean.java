/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

/**
 *
 * @author koller
 */
public class MutableBoolean {
    private boolean val = false;
    
    public MutableBoolean(boolean b) {
        val = b;
    }

    public void setValue(boolean b) {
        val = b;
    }

    public boolean booleanValue() {
        return val;
    }
    
}
