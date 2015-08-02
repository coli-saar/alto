/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class DebuggingWriter {
    private boolean enabled = false;
    
    public void withDebug(Runnable todo) {
        setEnabled(true);
        
        try {
            todo.run();
        } finally {
            setEnabled(false);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void D(int depth, Supplier<String> s) {
        if (enabled) {
            System.err.println(Util.repeat("  ", depth) + s.get());
        }
    }
}
