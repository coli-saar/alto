/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class CorpusWriter implements Consumer<Instance> {

    private final Writer writer;
    private boolean isAnnotated;
    private final InterpretedTreeAutomaton irtg;
    private final List<String> interpretationsInOrder;
    private boolean isHeaderWritten;
    private String comment;
    public static final String NULL = "_null_";

    public CorpusWriter(InterpretedTreeAutomaton irtg, boolean isAnnotated, String comment, Writer writer) throws IOException {
        this.writer = writer;
        this.isAnnotated = isAnnotated;
        this.irtg = irtg;
        this.interpretationsInOrder = new ArrayList<>(irtg.getInterpretations().keySet()); // fix some order of interpretations
        this.comment = comment;

        isHeaderWritten = false;
    }

    public void writeInstance(Instance inst) throws IOException {
        boolean isn = inst.isNull();
        
        if (!isHeaderWritten) {
            writer.write(makeHeader(comment) + "\n");
            isHeaderWritten = true;
        }

        if (inst.getComment() != null) {
            writer.write("# " + inst.getComment() + "\n");
        }

        for (String interp : interpretationsInOrder) {
            String repr = isn ? NULL : irtg.getInterpretation(interp).getAlgebra().representAsString(inst.getInputObjects().get(interp));
            writer.write(repr + "\n");
        }

        if (isAnnotated) {
            writer.write((isn ? NULL : irtg.getAutomaton().getSignature().resolve(inst.getDerivationTree())) + "\n");
        }

        writer.write("\n");
    }

    private String makeHeader(String comment) {
        StringBuilder buf = new StringBuilder();

        buf.append("# IRTG " + (isAnnotated ? "" : "un") + "annotated corpus file, v" + Corpus.CORPUS_VERSION + "\n");
        buf.append("# \n");

        if (comment != null) {
            for (String line : comment.split("\n")) {
                buf.append("# " + line + "\n");
            }
            buf.append("# \n");
        }

        for (String interp : interpretationsInOrder) {
            buf.append("# interpretation " + interp + ": " + irtg.getInterpretations().get(interp).getAlgebra().getClass() + "\n");
        }

        return buf.toString();
    }

    @Override
    public void accept(Instance t) {
        try {
            writeInstance(t);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setIsAnnotated(boolean isAnnotated) {
        this.isAnnotated = isAnnotated;
    }
    
    
}
