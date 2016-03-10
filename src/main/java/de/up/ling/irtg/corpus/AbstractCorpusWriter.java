/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.saar.basic.Pair;
import de.up.ling.irtg.codec.OutputCodec;
import static de.up.ling.irtg.corpus.CorpusWriter.NULL;
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

    public static interface ExConsumer<E> {
        public void accept(E x) throws IOException;
    }

    public static interface ExBiConsumer<E, F> {
        public void accept(E x, F y) throws IOException;
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
            fn.accept(interp.getLeft(), isn ? NULL : interp.getRight().asString(inst.getInputObjects().get(interp.getLeft())));
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
