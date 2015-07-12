/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.corpus;

import java.io.IOException;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public abstract class AbstractCorpusWriter implements Consumer<Instance> {
    protected boolean annotated = false;
    
    public abstract void writeInstance(Instance inst) throws IOException;
    public abstract void close() throws IOException;

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    @Override
    public void accept(Instance t) {
        try {
            writeInstance(t);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
