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
public abstract class Lazy<E> {
    private E value;
    private boolean dirty;

    public Lazy() {
        value = null;
        dirty = true;
    }
    
    abstract protected E evaluate();
    
    public E getValue() {
        if( dirty ) {
            value = evaluate();
            dirty = false;
        }
        
        return value;
    }
    
    public void setDirty() {
        dirty = true;
    }    
}
