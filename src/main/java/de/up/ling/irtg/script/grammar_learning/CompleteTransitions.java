/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.grammar_post.ExtendStringRules;
import de.up.ling.irtg.signature.Signature;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *
 * @author teichmann
 */
public class CompleteTransitions {
    /**
     * 
     * @param args 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String grammarName = props.getProperty("inputGrammarFile");
        String interpretationName = props.getProperty("stringInterpretationName");
        String outputName = props.getProperty("outputGrammarFile");
        String prefix = props.getProperty("newSymbolPrefix");
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita;
        try(FileInputStream fis = new FileInputStream(grammarName)) {
            ita = iic.read(fis);
        }
        
        Signature sig = ita.getInterpretation(interpretationName).getAlgebra().getSignature();
        double amount = 1.0;
        for(Rule r : ita.getAutomaton().getRuleSet()) {
            amount = Math.min(amount, r.getWeight());
        }
        int size = ita.getAutomaton().getStateInterner().getKnownIds().size();
        amount /= (sig.getMaxSymbolId()*size);
        
        List<Signature> l = new ArrayList<>();
        l.add(sig);
        
        AtomicInteger ai = new AtomicInteger();
        Supplier<String> supp = () -> {return prefix+"_"+ai.getAndIncrement();};
        InterpretedTreeAutomaton qta = ExtendStringRules.extend(ita, interpretationName, l, amount, supp);
        
        File f = new File(outputName);
        if(f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(qta.toString());
        }
    }
}
