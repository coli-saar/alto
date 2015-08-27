/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.OutputCodec;
import java.io.IOException;
import java.io.Writer;

/**
 * Comment lines are prepended with the <code>commentPrefix</code>. If
 * commentPrefix is null, then comments are not printed at all.
 *
 * @author koller
 */
public class CorpusWriter extends AbstractCorpusWriter {

    private boolean printSeparatorLines;
    private final Writer writer;
//    private final InterpretationPrintingPolicy printingPolicy;
    private boolean isHeaderWritten;
    private String comment;
    public static final String NULL = "_null_";
    private String commentPrefix;
    private InterpretedTreeAutomaton irtg;

    public CorpusWriter(InterpretedTreeAutomaton irtg, String comment, String commentPrefix, Writer writer) throws IOException {
        this(irtg, comment, commentPrefix, InterpretationPrintingPolicy.fromIrtg(irtg), writer);
    }

    /**
     *
     *
     * @param comment
     * @param commentPrefix - string with which each line of comments is
     * prefixed; set to null to suppress comments in output
     * @param printingPolicy
     * @param writer
     * @throws IOException
     */
    public CorpusWriter(InterpretedTreeAutomaton irtg, String comment, String commentPrefix, InterpretationPrintingPolicy printingPolicy, Writer writer) throws IOException {
        super(printingPolicy, false);

        this.writer = writer;
        this.comment = comment;
        this.commentPrefix = commentPrefix;
        this.printingPolicy = printingPolicy;
        this.irtg = irtg;

        printSeparatorLines = true;

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
            writer.write(makeHeader(comment));
            
            if( printSeparatorLines ) {
                writer.write("\n");
            }
            
            isHeaderWritten = true;
        }

        if (commentPrefix != null) {
            if (inst.getComments() != null && commentPrefix != null) {
                for (String key : inst.getComments().keySet()) {
                    writer.write(commentPrefix + key + ": " + inst.getComments().get(key) + "\n");
                }
            }
        }

        withDerivationTree(inst, irtg.getAutomaton().getSignature(), repr -> writer.write(repr + "\n"));
        forEachInterpretation(inst, (key, repr) -> writer.write(repr + "\n"));

        if (printSeparatorLines) {
            writer.write("\n");
        }

        writer.flush();
    }

    private String makeHeader(String comment) {
        StringBuilder buf = new StringBuilder();

        if (commentPrefix != null) {
            buf.append(commentPrefix + "IRTG " + (annotated ? "" : "un") + "annotated corpus file, v" + Corpus.CORPUS_VERSION + "\n");
            buf.append(commentPrefix + "\n");

            if (comment != null) {
                for (String line : comment.split("\n")) {
                    buf.append(commentPrefix + line + "\n");
                }
                buf.append(commentPrefix + "\n");
            }

            for (Pair<String, OutputCodec> interp : printingPolicy.get()) {
                buf.append(commentPrefix + "interpretation " + interp.getLeft() + ": " + irtg.getInterpretation(interp.getLeft()).getAlgebra().getClass() + "\n");
            }
        }

        return buf.toString();
    }

    public void setPrintSeparatorLines(boolean printSeparatorLines) {
        this.printSeparatorLines = printSeparatorLines;
    }
}
