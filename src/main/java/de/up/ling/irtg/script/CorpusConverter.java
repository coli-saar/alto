/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.google.common.collect.ImmutableMap;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.codec.PtbTreeInputCodec;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.function.Function;

/**
 *
 * @author koller
 */
public class CorpusConverter<E> {

    public static void main(String[] args) throws Exception {
        InputStream corpus = new FileInputStream(args[0]);
        InputCodec<Tree<String>> codec = new PtbTreeInputCodec();
        
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        Algebra s = new StringAlgebra();
        Algebra t = new TreeWithAritiesAlgebra();
        
        irtg.addInterpretation("string", new Interpretation(s, new Homomorphism(irtg.getAutomaton().getSignature(), s.getSignature())));
        irtg.addInterpretation("tree", new Interpretation(t, new Homomorphism(irtg.getAutomaton().getSignature(), t.getSignature())));
        
        Writer w = new FileWriter("out.txt");
        CorpusWriter writer = new CorpusWriter(irtg, false, "Converted from " + args[0] + "\non " + new Date().toString(), w);
        
        new CorpusConverter<Tree<String>>().convert(corpus, writer, codec, tree -> {
            Instance inst = new Instance();            
            inst.setInputObjects(ImmutableMap.of("string", tree.getLeafLabels(), "tree", tree));            
            return inst;
        });
        
        w.flush();
        w.close();
    }

    public void convert(InputStream corpus, CorpusWriter writer, InputCodec<E> codec, Function<E, Instance> conv) throws ParseException, IOException {
        try {
            do {
                E element = codec.read(corpus);
                System.err.println("element: " + element);
                Instance instance = conv.apply(element);
                writer.accept(instance);
            } while (true);
        } catch (EOFException e) {
            // done reading
        } 
    }
}
