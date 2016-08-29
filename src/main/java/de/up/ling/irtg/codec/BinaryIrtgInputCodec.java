/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec.Header;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec.TableOfContents;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.io.FixedNumberCodec;
import de.up.ling.irtg.io.NumberCodec;
import de.up.ling.irtg.io.StringCodec;
import de.up.ling.irtg.io.UtfStringCodec;
import de.up.ling.irtg.io.VariableLengthNumberCodec;
import de.up.ling.irtg.script.GrammarConverter;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An input codec for IRTGs in a binary file format. This codec will read files
 * that were written using the {@link BinaryIrtgInputCodec}. It does this much
 * faster than a text-based codec, such as {@link IrtgInputCodec}, could,
 * because no parsing is necessary.
 * <p>
 * To convert between binary and human-readable representations, see
 * {@link GrammarConverter}.
 *
 * @author koller
 */
@CodecMetadata(name = "irtg-bin", description = "IRTG grammar (binary format)", extension = "irtb", type = InterpretedTreeAutomaton.class)
public class BinaryIrtgInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
            InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(auto);

            // read header
            Header header = new Header();
            header.read(ois);

            NumberCodec nc = header.isUseVariableLengthEncoding() ? new VariableLengthNumberCodec(ois) : new FixedNumberCodec(ois);
            StringCodec sc = new UtfStringCodec(ois);

            // read TOC
            TableOfContents toc = new TableOfContents();
            toc.read(ois);

            // read interpretations
            List<String> interpNamesInOrder = readInterpretations(nc, sc, ret);

            // read signature-like objects
            readInterner(auto.getStateInterner(), nc, sc);
            readSignature(auto.getSignature(), nc, sc);
            for (String intp : interpNamesInOrder) {
                readSignature(ret.getInterpretation(intp).getAlgebra().getSignature(), nc, sc);
            }

            // read rules
            readRules(ret, interpNamesInOrder, nc);

            return ret;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new CodecParseException(e);
        }
    }

    private void readSignature(Signature sig, NumberCodec nc, StringCodec sc) throws IOException {
        int numSymbols = nc.readInt();

        for (int id = 1; id <= numSymbols; id++) {
            int arity = nc.readInt();
            String symbol = sc.readString();
            sig.addSymbol(symbol, arity);
        }
    }

    private void readInterner(Interner sig, NumberCodec nc, StringCodec sc) throws IOException {
        int numSymbols = nc.readInt();

        sig.setTrustingMode(true);

        for (int i = 0; i < numSymbols; i++) {
            int id = nc.readInt();
            String symbol = sc.readString();
            sig.addObjectWithIndex(id, symbol);
        }
        
        sig.setTrustingMode(false);
    }

    private List<String> readInterpretations(NumberCodec nc, StringCodec sc, InterpretedTreeAutomaton irtg) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        int numInterpretations = nc.readInt();
        List<String> ret = new ArrayList<>();

        for (int i = 0; i < numInterpretations; i++) {
            String name = sc.readString();
            String className = sc.readString();
            Algebra alg = (Algebra) getClass().getClassLoader().loadClass(className).newInstance();
            Homomorphism hom = new Homomorphism(irtg.getAutomaton().getSignature(), alg.getSignature());
            Interpretation intp = new Interpretation(alg, hom);

            irtg.addInterpretation(name, intp);
            ret.add(name);
        }

        return ret;
    }

    private void readRules(InterpretedTreeAutomaton irtg, List<String> interpNamesInOrder, NumberCodec nc) throws IOException {
        ConcreteTreeAutomaton<String> auto = (ConcreteTreeAutomaton<String>) irtg.getAutomaton();

        // read final states
        int numFinalStates = nc.readInt();
        for (int i = 0; i < numFinalStates; i++) {
            auto.addFinalState(nc.readInt());
        }

        // read rules
        long numRules = nc.readLong();
        for (long i = 0; i < numRules; i++) {
            // read rule itself
            int parent = nc.readInt();
            int label = nc.readInt();
            int arity = auto.getSignature().getArity(label);
            int[] children = new int[arity];
            for (int j = 0; j < arity; j++) {
                children[j] = nc.readInt();
            }
            double weight = nc.readDouble();

            Rule r = auto.createRule(parent, label, children, weight);
            auto.addRule(r);

            // read homomorphic images
            for (String interp : interpNamesInOrder) {
                Homomorphism hom = irtg.getInterpretation(interp).getHomomorphism();
                Tree<HomomorphismSymbol> h = readHomTree(hom.getTargetSignature(), nc);
                hom.add(label, h);
            }
        }
    }

    private Tree<HomomorphismSymbol> readHomTree(Signature sig, NumberCodec nc) throws IOException {
        int value = nc.readSignedInt();

        if (value <= 0) {
            HomomorphismSymbol hs = HomomorphismSymbol.createVariable(-value);
            return Tree.create(hs);
        } else {
            HomomorphismSymbol hs = HomomorphismSymbol.createConstant(value);
            int arity = sig.getArity(value);
            List<Tree<HomomorphismSymbol>> children = new ArrayList<>();

            for (int i = 0; i < arity; i++) {
                children.add(readHomTree(sig, nc));
            }

            return Tree.create(hs, children);
        }
    }

}
