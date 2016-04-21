/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author teichmann
 */
public class BruteForceCorpusReader {

    /**
     *
     * @param in
     * @param interpretationName
     * @param algebras
     * @return
     * @throws IOException
     * @throws CorpusReadingException
     */
    public static Corpus read(Reader in, String[] interpretationName, Algebra[] algebras) throws IOException, CorpusReadingException {
        InterpretedTreeAutomaton ita = CreateIRTGFromStrings(interpretationName, algebras);

        return Corpus.readCorpusLenient(in, ita);
    }

    /**
     *
     * @param interpretationName
     * @param algebras
     * @return
     */
    public static InterpretedTreeAutomaton CreateIRTGFromStrings(String[] interpretationName, Algebra[] algebras) {
        TreeAutomaton empt = new ConcreteTreeAutomaton();
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(empt);
        Map<String, Interpretation> inters = new HashMap<>();
        for (int i = 0; i < interpretationName.length && i < algebras.length; ++i) {
            Algebra ag = algebras[i];

            Homomorphism hom = new Homomorphism(empt.getSignature(), ag.getSignature());
            Interpretation inter = new Interpretation(ag, hom);

            inters.put(interpretationName[i], inter);
        }
        ita.addAllInterpretations(inters);
        return ita;
    }
    
    /**
     * 
     * @param ita
     * @param it
     * @param w
     * @throws IOException 
     */
    public static void writeCorpus(InterpretedTreeAutomaton ita, Iterable<Instance> it, Writer w) throws IOException {
        CorpusWriter cw = new CorpusWriter(ita, "", w);
        
        for(Instance i : it) {
            cw.writeInstance(i);
        }
        
        w.flush();
    }
}
