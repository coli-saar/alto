/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.saar.basic.Pair;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.io.IOException;
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

    public interface ExConsumer<E> {
        void accept(E x) throws IOException;
    }

    public interface ExBiConsumer<E, F> {
        void accept(E x, F y) throws IOException;
    }

    protected void withDerivationTree(Instance inst, Signature derivationTreeSignature, ExConsumer<String> fn) throws IOException {
        boolean isn = inst.isNull();

        if (annotated && printingPolicy.getAlgebraForDerivationTree() != null) {
            if (isn) {
                fn.accept(NULL);
            } else {
                Tree<String> t = derivationTreeSignature.resolve(inst.getDerivationTree());
                fn.accept(printingPolicy.getAlgebraForDerivationTree().representAsString(t));
            }
        }
    }

    protected void forEachInterpretation(Instance inst, ExBiConsumer<String, String> fn) throws IOException {
        boolean isn = inst.isNull();

        for (Pair<String, OutputCodec> interp : printingPolicy.get()) {
            // skip interpretations that are defined in the grammar, but not in the corpus
            // (more precisely, in this instance)
            if( inst.getInputObjects().containsKey(interp.getLeft())) {
                Object o = inst.getInputObjects().get(interp.getLeft());
                fn.accept(interp.getLeft(), isn ? NULL : interp.getRight().asString(o));
            }
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
    
    public void writeCorpus(Corpus corpus) throws IOException {
        int count = 0;
        
        for( Instance inst : corpus ) {
            ++count;
            
            try {
                writeInstance(inst);
            } catch(NullPointerException exception) {
                throw new NullPointerException("Instance "+inst+" at count "+count+" caused error:"+System.getProperty("line.separator")+exception.getMessage());
            }
        }
    }

}
