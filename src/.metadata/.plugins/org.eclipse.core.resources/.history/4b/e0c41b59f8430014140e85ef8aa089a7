/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class Lazy<E> implements Serializable {
    private transient E value;
    private boolean dirty;
    private final Supplier<E> supplier;

    public Lazy(Supplier<E> supplier) {
        value = null;
        dirty = true;
        this.supplier = supplier;
    }
    
//    abstract protected E evaluate();
    
    public E getValue() {
        if( dirty ) {
            value = supplier.get();
            dirty = false;
        }
        
        return value;
    }
    
    public void setDirty() {
        dirty = true;
    }
    
    // for deserialization
    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         dirty = true;
    }
}

