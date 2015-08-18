/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.corpus;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import static de.up.ling.irtg.corpus.CorpusWriter.NULL;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public abstract class AbstractCorpusWriter implements Consumer<Instance> {
    protected boolean annotated;
    protected InterpretationPrintingPolicy printingPolicy;
    public static final String NULL = "_null_";
    
    public abstract void writeInstance(Instance inst) throws IOException;
    public abstract void close() throws IOException;

    public AbstractCorpusWriter(InterpretationPrintingPolicy printingPolicy, boolean annotated) {
        this.annotated = annotated;
        this.printingPolicy = printingPolicy;
    }
    
    public static interface ExConsumer<E> {
        public void accept(E x) throws IOException;
    }
    
    public static interface ExBiConsumer<E,F> {
        public void accept(E x, F y) throws IOException;
    }
    
    protected void withDerivationTree(Instance inst, ExConsumer<String> fn) throws IOException {
        boolean isn = inst.isNull();
        
        if (annotated && printingPolicy.getAlgebraForDerivationTree() != null) {
            fn.accept(isn ? NULL : printingPolicy.getAlgebraForDerivationTree().representAsString(inst.getDerivationTree()));
        }
    }
    
    protected void forEachInterpretation(Instance inst, ExBiConsumer<String,String> fn) throws IOException {
        boolean isn = inst.isNull();
        
        for( Pair<String,Algebra> interp : printingPolicy.get() ) {
            fn.accept(interp.getLeft(), isn ? NULL : interp.getRight().representAsString(inst.getInputObjects().get(interp.getLeft())));
        }
    }

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
