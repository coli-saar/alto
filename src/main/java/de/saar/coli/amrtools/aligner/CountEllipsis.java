/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import de.saar.coli.amrtagging.Alignment;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author JG
 */
public class CountEllipsis {
    
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException {
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        Signature dummySig = new Signature();
        loaderIRTG.addInterpretation(new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig), "repalignment"));
        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader(args[0]), loaderIRTG);
        
        int totalGraphs = 0;
        int graphsWithEllipsis = 0;
        outer:
        for (Instance inst : corpus) {
            totalGraphs++;
//            Set<String> seenNodes = new HashSet<>();
            for (String alString : (List<String>)inst.getInputObjects().get("repalignment")) {
                Alignment al = Alignment.read(alString);
                if (al.lexNodes.size() > 1) {
                    graphsWithEllipsis++;
                    continue outer;
                }
//                for (String nn : al.nodes) {
//                    if (seenNodes.contains(nn)) {
//                        graphsWithEllipsis++;
//                        continue outer;
//                    } else {
//                        seenNodes.add(nn);
//                    }
//                }
            }
        }
        System.err.println("total graphs: "+totalGraphs);
        System.err.println("with ellipsis: "+graphsWithEllipsis);
        System.err.println("proportional: "+(graphsWithEllipsis/(double)totalGraphs));
        System.err.println("without ellipsis: "+(totalGraphs-graphsWithEllipsis));
    }
    
}
