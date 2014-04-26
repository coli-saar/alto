/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.signature.Signature;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class PtbParsingEvaluator {
    public static void main(String[] args) throws Exception {
        String grammarFilename = args[0];
        String corpusFilename = args[1];

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileReader(grammarFilename));
//        Homomorphism hom = irtg.getInterpretation("i").getHomomorphism();
        Signature sig = irtg.getAutomaton().getSignature();
        Corpus corpus = irtg.readCorpus(new FileReader(corpusFilename));
        int i = 1;

        for (Instance inst : corpus) {
            Map<String, Object> input = new HashMap<String, Object>();
            List<String> sentence = (List<String>) inst.getInputObjects().get("i");
            input.put("i", sentence);

            long start = System.nanoTime();
            TreeAutomaton chart = irtg.parseInputObjects(input);
            long duration = System.nanoTime() - start;

            System.out.print(i + "\t" + sentence.size() + "\t" + duration / 1000000 + "\t");

            try {
                boolean annotatedIsAccepted = chart.acceptsRaw(inst.getDerivationTree());
                System.out.print(annotatedIsAccepted + "\t");
            } catch (Exception e) {
                System.out.print(e.getClass() + "\t");
            }

            try {
                boolean viterbiEqualsAccepted = sig.resolve(inst.getDerivationTree()).equals(chart.viterbi());
                System.out.println(viterbiEqualsAccepted);
            } catch (Exception e) {
                System.out.println(e.getClass());
            }
            
            i++;
        }
    }
}
