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

/**
 *
 * @author koller
 */
public class CorpusWriter extends AbstractCorpusWriter {

    private final Writer writer;
    private final InterpretedTreeAutomaton irtg;
    private final List<String> interpretationsInOrder;
    private boolean isHeaderWritten;
    private String comment;
    public static final String NULL = "_null_";
    private String commentPrefix;

    public CorpusWriter(InterpretedTreeAutomaton irtg, String comment, String commentPrefix, Writer writer) throws IOException {
        this.writer = writer;
        this.irtg = irtg;
        this.interpretationsInOrder = new ArrayList<>(irtg.getInterpretations().keySet()); // fix some order of interpretations
        this.comment = comment;
        this.commentPrefix = commentPrefix;

        isHeaderWritten = false;
    }
    
    public CorpusWriter(InterpretedTreeAutomaton irtg, String comment, Writer writer) throws IOException {
        this(irtg, comment, "# ", writer);
    }
    
    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

    @Override
    public void writeInstance(Instance inst) throws IOException {
        boolean isn = inst.isNull();
        
        if (!isHeaderWritten) {
            writer.write(makeHeader(comment) + "\n");
            isHeaderWritten = true;
        }

        if (inst.getComments() != null) {
            for( String key : inst.getComments().keySet() ) {
                writer.write(commentPrefix + key + ": " + inst.getComments().get(key) + "\n");
            }
        }

        for (String interp : interpretationsInOrder) {
            String repr = isn ? NULL : irtg.getInterpretation(interp).getAlgebra().representAsString(inst.getInputObjects().get(interp));
            writer.write(repr + "\n");
        }

        if (annotated) {
            writer.write((isn ? NULL : irtg.getAutomaton().getSignature().resolve(inst.getDerivationTree())) + "\n");
        }

        writer.write("\n");
        writer.flush();
    }

    private String makeHeader(String comment) {
        StringBuilder buf = new StringBuilder();

        buf.append(commentPrefix + "IRTG " + (annotated ? "" : "un") + "annotated corpus file, v" + Corpus.CORPUS_VERSION + "\n");
        buf.append(commentPrefix + "\n");

        if (comment != null) {
            for (String line : comment.split("\n")) {
                buf.append(commentPrefix + line + "\n");
            }
            buf.append(commentPrefix + "\n");
        }

        for (String interp : interpretationsInOrder) {
            buf.append(commentPrefix + "interpretation " + interp + ": " + irtg.getInterpretations().get(interp).getAlgebra().getClass() + "\n");
        }

        return buf.toString();
    }
}
