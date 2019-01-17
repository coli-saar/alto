/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.io.NumberCodec;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Use this with NoSignatureBinaryIrtgOutputCodec, together with a reference IRTG that contains
 * the signatures that are not written here (note that the mapping from IDs to symbols
 * in the reference IRTG must be identical to the one in the IRTG written here).
 * @author Jonas
 */
public class NoSignatureBinaryIrtgInputCodec extends BinaryIrtgInputCodec {
    
    InterpretedTreeAutomaton referenceIRTG;
    
    public NoSignatureBinaryIrtgInputCodec(InterpretedTreeAutomaton referenceIRTG) {
        this.referenceIRTG = referenceIRTG;
    }

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        InterpretedTreeAutomaton in = super.read(is); //To change body of generated methods, choose Tools | Templates.
        ConcreteTreeAutomaton<String> gAuto = new ConcreteTreeAutomaton<>(referenceIRTG.getAutomaton().getSignature());
        for (int id : in.getAutomaton().getStateInterner().getKnownIds()) {
            gAuto.getStateInterner().addObjectWithIndex(id, in.getAutomaton().getStateInterner().resolveId(id));
        }
        for (Rule rule : in.getAutomaton().getRuleSet()) {
            gAuto.addRule(rule);
        }
        for (int finalState : in.getAutomaton().getFinalStates()) {
            gAuto.addFinalState(finalState);
        }
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(gAuto);
        for (Map.Entry<String, Interpretation<?>> interp : in.getInterpretations().entrySet()) {
            Homomorphism hom = new Homomorphism(gAuto.getSignature(), referenceIRTG.getInterpretation(interp.getKey()).getHomomorphism().getTargetSignature());
            for (int i = 1; i<=gAuto.getSignature().getMaxSymbolId(); i++) {
                Tree<HomomorphismSymbol> term = interp.getValue().getHomomorphism().get(i);
                if (term != null) {
                    hom.add(i, term);
                }
            }
            ret.addInterpretation(new Interpretation(referenceIRTG.getInterpretation(interp.getKey()).getAlgebra(), hom, interp.getKey()));
        }
        return ret;
    }

    @Override
    protected void readRules(InterpretedTreeAutomaton irtg, List<String> interpNamesInOrder, NumberCodec nc) throws IOException {
        ConcreteTreeAutomaton<String> auto = (ConcreteTreeAutomaton<String>) irtg.getAutomaton();
        //System.err.println(referenceIRTG.getAutomaton().getSignature().toString().substring(0, 1000));
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
            int arity = referenceIRTG.getAutomaton().getSignature().getArity(label);
            if (arity < 0) {
                System.err.println(label);
                continue;
            }
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
                Tree<HomomorphismSymbol> h = readHomTree(referenceIRTG.getInterpretation(interp).getHomomorphism().getTargetSignature(), nc);
                hom.add(label, h);
            }
        }
    }

    
    
    
    
    /**
     * translates first argument IRTG into normal irtb format, using second argument
     * IRTG as reference for signatures
     * @param args
     * @throws FileNotFoundException
     * @throws CodecParseException
     * @throws IOException 
     */
    public static void main(String[] args) throws FileNotFoundException, CodecParseException, IOException {
        
        InterpretedTreeAutomaton reference = new BinaryIrtgInputCodec().read(new FileInputStream(args[1]));
        InterpretedTreeAutomaton input = new NoSignatureBinaryIrtgInputCodec(reference).read(new FileInputStream(args[0]));
        System.err.println(input.getAutomaton().getNumberOfRules());
        new BinaryIrtgOutputCodec().write(input, new FileOutputStream(args[2]));
        
        
        
        //TODO: use smth like this as test
//        InterpretedTreeAutomaton input = new BinaryIrtgInputCodec().read(new FileInputStream("../../experimentData/falken-3/22845.irtb"));
////        for (Rule rule : input.getAutomaton().getRuleSet()) {
////            System.err.println(rule);
////        }
//        InterpretedTreeAutomaton reference = new BinaryIrtgInputCodec().read(new FileInputStream("../../experimentData/falken-3/graphSummary.irtb"));
//        new NoSignatureBinaryIrtgOutputCodec().write(input, new FileOutputStream("../../experimentData/temp/222845.irtb"));
//        InterpretedTreeAutomaton loaded = new NoSignatureBinaryIrtgInputCodec(reference).read(new FileInputStream("../../experimentData/temp/222845.irtb"));
////        System.err.println("loaded");
////        for (Rule rule : loaded.getAutomaton().getRuleSet()) {
////            System.err.println(rule);
////        }
//        //System.err.println(input);
//        //System.err.println(loaded);
//        new BinaryIrtgOutputCodec().write(loaded, new FileOutputStream("../../experimentData/temp/222845_test.irtb"));
//        System.err.println(input.equals(loaded));
//        Tree<String> vit = loaded.getAutomaton().viterbi();
//        System.err.println(vit);
//        Tree homVit = loaded.getInterpretation("induction").getHomomorphism().apply(vit);
//        System.err.println(homVit);
//        System.err.println(loaded.getInterpretation("induction").getAlgebra().evaluate(vit));
    }
    
    
}
