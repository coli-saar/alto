/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools;

import de.saar.coli.amrtagging.Alignment;
import de.saar.coli.amrtagging.Alignment.Span;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Counter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Small script to create named entity type lookup chart from corpus.
 * @author JG
 */
public class MakeNETypeLookup {
    
    /**
     * Small script to create named entity type lookup chart from corpus.
     * First argument is path to corpus created by RareWordsAnnotater,
     * second argument is path to output file.
     * @param args
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void main(String[] args) throws IOException, CorpusReadingException {
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        Signature dummySig = new Signature();
        loaderIRTG.addInterpretation("repgraph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("string", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("repstring", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("repalignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("spanmap", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader(args[0]), loaderIRTG);
        
        Map<String, Counter<String>> lit2types = new HashMap<>();
        
        Counter<String> debug = new Counter<>();
        for (Instance inst : corpus) {
            debug.add("instances");
            List<String> repsent = (List)inst.getInputObjects().get("repstring");
            List<String> sent = (List)inst.getInputObjects().get("string");
            List<String> als = (List)inst.getInputObjects().get("repalignment");
            List<String> spanmap = (List)inst.getInputObjects().get("spanmap");
            SGraph graph = (SGraph)inst.getInputObjects().get("repgraph");
            Int2ObjectMap<Alignment> id2al = new Int2ObjectOpenHashMap();
            for (String alStr : als) {
                Alignment al = Alignment.read(alStr);
                if (al != null) {
                    id2al.put(al.span.start, al);
                }
            }
            
            for (int i = 0; i<repsent.size(); i++) {
                String t = repsent.get(i);
                if (t.equals("_NAME_")) {
                    debug.add("names");
                    Alignment al = id2al.get(i);
                    Span span = new Span(spanmap.get(i));
                    StringJoiner sj = new StringJoiner("_");
                    for (int j = span.start; j<span.end; j++) {
                        sj.add(sent.get(j));
                    }
                    String lit = sj.toString();
                    String type = null;
                    for (String lexN : al.lexNodes) {
                        GraphNode n = graph.getNode(lexN);
                        if (n != null) {
                            for (GraphEdge e : graph.getGraph().incomingEdgesOf(n)) {
                                if (e.getLabel().equals("name")) {
                                    type = e.getSource().getLabel();
                                }
                            }
                        } else {
                            System.err.println("node not found!");
                        }
                    }
                    if (type != null) {
                        count(lit2types, lit, type);
                        debug.add("names added");
                    } else {
                        System.err.println("no type!");
                    }
                }
            }
        }
        
        FileWriter w = new FileWriter(args[1]);
        for (String key : lit2types.keySet()) {
            w.write(key+"\t"+lit2types.get(key).argMax()+"\n");
        }
        w.close();
        
        debug.printAllSorted();
        
    }
    
    public static void count(Map<String, Counter<String>> map, String key, String value) {
        Counter<String> c = map.get(key);
        if (c == null) {
            c = new Counter<>();
            map.put(key, c);
        }
        c.add(value);
    }
    
}
