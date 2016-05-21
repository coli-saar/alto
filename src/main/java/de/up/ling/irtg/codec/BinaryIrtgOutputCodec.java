/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An output codec for IRTGs in a binary file format.
 * For large grammars, the binary format can be much
 * more compact than a text-based format, and can be
 * read by the {@link BinaryIrtgInputCodec} 
 * much faster than a text-based codec could.
 * <p>
 * To convert between binary and human-readable representations,
 * see {@link GrammarConverter}.
 * 
 * @author koller
 */

@CodecMetadata(name = "irtg-bin", description = "IRTG grammar (binary format)", extension = "irtb", type = InterpretedTreeAutomaton.class)
public class BinaryIrtgOutputCodec extends OutputCodec<InterpretedTreeAutomaton> {

    private TableOfContents toc = null;

    @Override
    public void write(InterpretedTreeAutomaton irtg, OutputStream ostream) throws IOException, UnsupportedOperationException {
        ObjectOutputStream oos = new ObjectOutputStream(ostream);
        List<String> interpNamesInOrder = new ArrayList<String>(irtg.getInterpretations().keySet());

        // write dummy TOC to reserve space
        toc = new TableOfContents();
        long tocLength = toc.write(oos);
        toc.recordStartPosition(Blocks.INTERPRETATIONS, tocLength);

        // write interpretations
        long interpLength = writeInterpretations(irtg, interpNamesInOrder, oos);
        toc.recordNewBlock(Blocks.SIGNATURES, interpLength);

        // write signatures
        long sigLength = 0;
        sigLength += writeInterner(irtg.getAutomaton().getStateInterner(), oos);
        sigLength += writeSignature(irtg.getAutomaton().getSignature(), oos);
        for (String intrp : interpNamesInOrder) {
            sigLength += writeSignature(irtg.getInterpretation(intrp).getAlgebra().getSignature(), oos);
        }
        toc.recordNewBlock(Blocks.RULES, sigLength);

        // write rules
        long rulesLength = writeRules(irtg, interpNamesInOrder, oos);

        // write TOC with actual values
        // TODO - this doesn't work yet
//        System.err.println("rewrite toc " + toc);
        oos.reset();
        toc.write(oos);
    }

    private long writeRules(InterpretedTreeAutomaton irtg, List<String> interpretationsInOrder, ObjectOutputStream oos) throws IOException {
        long bytes = 0;

        // write final states
        oos.writeInt(irtg.getAutomaton().getFinalStates().size());
        bytes += 4;

        for (int q : irtg.getAutomaton().getFinalStates()) {
            oos.writeInt(q);
            bytes += 4;
        }

        // iterate over rules
        oos.writeLong(irtg.getAutomaton().getNumberOfRules());
        bytes += 8;

        for (Rule r : irtg.getAutomaton().getRuleSet()) {
            // write automaton rule
            oos.writeInt(r.getParent());
            oos.writeInt(r.getLabel());

            for (int i = 0; i < r.getChildren().length; i++) {
                oos.writeInt(r.getChildren()[i]);
            }

            oos.writeDouble(r.getWeight());

            bytes += 4 * (2 + r.getChildren().length) + 8;

            // write homomorphic images
            for (String interp : interpretationsInOrder) {
                bytes += writeTree(irtg.getInterpretation(interp).getHomomorphism().get(r.getLabel()), oos);
            }
        }

        return bytes;
    }

    private long writeTree(Tree<HomomorphismSymbol> tree, ObjectOutputStream oos) {
        MutableInteger bytes = new MutableInteger(0);

        tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Void>() {
            @Override
            public Void visit(Tree<HomomorphismSymbol> node, Void data) {
                HomomorphismSymbol sym = node.getLabel();

                try {
                    if (sym.isVariable()) {
                        oos.writeInt(-sym.getValue());
                    } else {
                        oos.writeInt(sym.getValue());
                    }

                    bytes.setValue(bytes.getValue() + 4);
                } catch (IOException e) {
                    System.err.println("IO exception in writeTree: " + e.getMessage());
                }

                return null;
            }
        });

        return bytes.getValue();
    }

    private long writeInterner(Interner sig, ObjectOutputStream oos) throws IOException {
        long bytes = 0;

        oos.writeInt(sig.getKnownIds().size());
        bytes += 4;

        for (int id : sig.getKnownIds()) {
            oos.writeInt(id);
            bytes += 4;
            bytes += writeString(sig.resolveId(id).toString(), oos);
        }

        return bytes;
    }

    private long writeSignature(Signature sig, ObjectOutputStream oos) throws IOException {
        long bytes = 0;

        oos.writeInt(sig.getMaxSymbolId());
        bytes += 4;

        for (int id = 1; id <= sig.getMaxSymbolId(); id++) {
            oos.writeInt(sig.getArity(id));
            bytes += 4;
            bytes += writeString(sig.resolveSymbolId(id), oos);
        }

        return bytes;
    }

    private long writeInterpretations(InterpretedTreeAutomaton irtg, List<String> interpNamesInOrder, ObjectOutputStream oos) throws IOException {
        long bytes = 0;

        oos.writeInt(irtg.getInterpretations().size());
        bytes += 4;

        for (String interpName : interpNamesInOrder) {
            bytes += writeString(interpName, oos);
            bytes += writeString(irtg.getInterpretation(interpName).getAlgebra().getClass().getName(), oos);
        }

        return bytes;
    }

    private long writeString(String s, ObjectOutputStream oos) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream obaos = new ObjectOutputStream(baos);

        oos.writeUTF(s);

        obaos.writeUTF(s);
        obaos.close();
        return baos.toByteArray().length;
    }

    static enum Blocks {
        TOC, INTERPRETATIONS, SIGNATURES, RULES
    }

    static class TableOfContents {

        private Map<Blocks, Long> startPositions;
        private long previousBlockStart = 0L;

        public TableOfContents() {
            startPositions = new HashMap<>();

            for (Blocks b : Blocks.values()) {
                startPositions.put(b, 0L);
            }
        }

        public void recordNewBlock(Blocks block, long lengthOfPreviousBlock) {
            previousBlockStart += lengthOfPreviousBlock;
            recordStartPosition(block, previousBlockStart);
        }

        public void recordStartPosition(Blocks block, long position) {
            startPositions.put(block, position);
        }

        public Map<Blocks, Long> getStartPositions() {
            return startPositions;
        }

        /**
         * Returns the number of bytes written to the stream.
         *
         * @param os
         * @return
         * @throws IOException
         */
        public long write(ObjectOutputStream os) throws IOException {
            long bytesWritten = 0;

            for (Blocks b : Blocks.values()) {
                os.writeLong(startPositions.get(b));
                bytesWritten += 8; // long = 8 bytes
            }

            return bytesWritten;
        }

        public void read(ObjectInputStream is) throws IOException {
            for (Blocks b : Blocks.values()) {
                startPositions.put(b, is.readLong());
            }
        }

        @Override
        public String toString() {
            return startPositions.toString();
        }
    }

}
