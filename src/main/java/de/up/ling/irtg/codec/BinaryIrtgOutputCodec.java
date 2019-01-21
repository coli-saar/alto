/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.io.NumberCodec;
import de.up.ling.irtg.io.StringCodec;
import de.up.ling.irtg.io.UtfStringCodec;
import de.up.ling.irtg.io.VariableLengthNumberCodec;
import de.up.ling.irtg.script.GrammarConverter;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An output codec for IRTGs in a binary file format. For large grammars, the
 * binary format can be much more compact than a text-based format, and can be
 * read by the {@link BinaryIrtgInputCodec} much faster than a text-based codec
 * could.
 * <p>
 * To convert between binary and human-readable representations, see
 * {@link GrammarConverter}.
 *
 * @author koller
 */
@CodecMetadata(name = "irtg-bin", description = "IRTG grammar (binary format)", extension = "irtb", type = InterpretedTreeAutomaton.class)
public class BinaryIrtgOutputCodec extends OutputCodec<InterpretedTreeAutomaton> {
    @Override
    public void write(InterpretedTreeAutomaton irtg, OutputStream ostream) throws IOException, UnsupportedOperationException {
        ObjectOutputStream oos = new ObjectOutputStream(ostream);
        NumberCodec nc = new VariableLengthNumberCodec(oos);
        StringCodec sc = new UtfStringCodec(oos);
        
        List<String> interpNamesInOrder = new ArrayList<>(irtg.getInterpretations().keySet());
        TableOfContents toc = new TableOfContents();

        Header header = new Header();
        long headerLength = header.write(oos);
        toc.recordStartPosition(Blocks.TOC, headerLength);

        // write dummy TOC to reserve space
        long tocLength = toc.write(oos);
        toc.recordStartPosition(Blocks.INTERPRETATIONS, tocLength);

        // write interpretations
        long interpLength = writeInterpretations(irtg, interpNamesInOrder, nc, sc);
        toc.recordNewBlock(Blocks.SIGNATURES, interpLength);

        // write signatures
        long sigLength = 0;
        sigLength += writeInterner(irtg.getAutomaton().getStateInterner(), nc, sc);
        sigLength += writeSignature(irtg.getAutomaton().getSignature(), nc, sc);
        for (String intrp : interpNamesInOrder) {
            long sigBytes = writeSignature(irtg.getInterpretation(intrp).getHomomorphism().getTargetSignature(), nc, sc);
	    sigLength += sigBytes;
//            sigLength += writeSignature(irtg.getInterpretation(intrp).getAlgebra().getSignature(), nc, sc);
        }
        toc.recordNewBlock(Blocks.RULES, sigLength);

        // write rules
        long rulesLength = writeRules(irtg, interpNamesInOrder, nc);
        
        // rewrite TOC with actual values
        // TODO - this doesn't work yet
        oos.reset();
        toc.write(oos);
        
        oos.flush();
    }

    private long writeRules(InterpretedTreeAutomaton irtg, List<String> interpretationsInOrder, NumberCodec nw) throws IOException {
        long bytes = 0;

        // write final states
        bytes += nw.writeInt(irtg.getAutomaton().getFinalStates().size());

        for (int q : irtg.getAutomaton().getFinalStates()) {
            bytes += nw.writeInt(q);
        }

        // iterate over rules
        bytes += nw.writeLong(irtg.getAutomaton().getNumberOfRules());

        for (Rule r : irtg.getAutomaton().getRuleSet()) {
            // write automaton rule
            bytes += nw.writeInt(r.getParent());
            bytes += nw.writeInt(r.getLabel());

            for (int i = 0; i < r.getChildren().length; i++) {
                bytes += nw.writeInt(r.getChildren()[i]);
            }

            bytes += nw.writeDouble(r.getWeight());

            // write homomorphic images
            for (String interp : interpretationsInOrder) {
                bytes += writeTree(irtg.getInterpretation(interp).getHomomorphism().get(r.getLabel()), nw);
            }
        }

        return bytes;
    }

    private long writeTree(Tree<HomomorphismSymbol> tree, NumberCodec  nw) {
        MutableInteger bytes = new MutableInteger(0);

        tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Void>() {
            @Override
            public Void visit(Tree<HomomorphismSymbol> node, Void data) {
                HomomorphismSymbol sym = node.getLabel();
                int bytesHere = 0;

                try {
                    if (sym.isVariable()) {
                        bytesHere = (int) nw.writeSignedInt(-sym.getValue());
                    } else {
                        bytesHere = (int) nw.writeSignedInt(sym.getValue());
                    }

                    bytes.setValue(bytes.getValue() + bytesHere);
                } catch (IOException e) {
                    System.err.println("IO exception in writeTree: " + e.getMessage());
                }

                return null;
            }
        });

        return bytes.getValue();
    }

    private long writeInterner(Interner sig, NumberCodec nc, StringCodec sc) throws IOException {
        long bytes = 0;

        bytes += nc.writeInt(sig.getKnownIds().size());

        for (int id : sig.getKnownIds()) {
            bytes += nc.writeInt(id);
            bytes += sc.writeString(sig.resolveId(id).toString());
        }

        return bytes;
    }

    protected long writeSignature(Signature sig, NumberCodec nc, StringCodec sc) throws IOException {
        long bytes = 0;

        bytes += nc.writeInt(sig.getMaxSymbolId());

        for (int id = 1; id <= sig.getMaxSymbolId(); id++) {
            bytes += nc.writeInt(sig.getArity(id));
            bytes += sc.writeString(sig.resolveSymbolId(id));
        }

        return bytes;
    }

    private long writeInterpretations(InterpretedTreeAutomaton irtg, List<String> interpNamesInOrder, NumberCodec nc, StringCodec sc) throws IOException {
        long bytes = 0;
        
        bytes += nc.writeInt(irtg.getInterpretations().size());

        for (String interpName : interpNamesInOrder) {
            bytes += sc.writeString(interpName);
            bytes += sc.writeString(irtg.getInterpretation(interpName).getAlgebra().getClass().getName());
        }

        return bytes;
    }
    
    enum Blocks {
        HEADER, TOC, INTERPRETATIONS, SIGNATURES, RULES
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

    static class Header {

        private int version;
        private int reserved1;
        private int reserved2;
        private boolean useVariableLengthEncoding;
        private boolean reserved3;
        private boolean reserved4;

        public Header() {
            version = 1;
            useVariableLengthEncoding = true;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public boolean isUseVariableLengthEncoding() {
            return useVariableLengthEncoding;
        }

        public void setUseVariableLengthEncoding(boolean useVariableLengthEncoding) {
            this.useVariableLengthEncoding = useVariableLengthEncoding;
        }
        
        

        public long write(ObjectOutputStream os) throws IOException {
            os.writeInt(version);
            os.writeInt(reserved1);
            os.writeInt(reserved2);
            os.writeBoolean(useVariableLengthEncoding);
            os.writeBoolean(reserved3);
            os.writeBoolean(reserved4);
            return 3 * 4 + 3 * 1;
        }

        public void read(ObjectInputStream is) throws IOException {
            version = is.readInt();
            reserved1 = is.readInt();
            reserved2 = is.readInt();

            useVariableLengthEncoding = is.readBoolean();
            reserved3 = is.readBoolean();
            reserved4 = is.readBoolean();
        }
    }


    /**
     * Translates the specified .irtg file to a .irtb file at the same location.
     * @param args 
     */
    public static void main(String[] args) throws IOException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromPath(args[0]);
        new BinaryIrtgOutputCodec().write(irtg, new FileOutputStream(args[0].substring(0, args[0].length()-4)+"irtb"));
    }
    
    

}
