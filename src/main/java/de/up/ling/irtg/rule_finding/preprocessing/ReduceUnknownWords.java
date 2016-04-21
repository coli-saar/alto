/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.rule_finding.preprocessing.geoquery.Reductioner;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author teichmann
 */
public class ReduceUnknownWords {
    /**
     * 
     * @param args
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, CorpusReadingException {
        String inName = args[0];
        String reduceName = args[1];
        String outName = args[2];
        String minCount = args[3];
        String reduceBy = args[4];
        String minLength = args[5];
        
        List<String> names = new ArrayList<>();
        List<Algebra> algeb = new ArrayList<>();

        for (int i = 6; i < args.length; i += 2) {
            names.add(args[i]);

            Algebra alg = (Algebra) Class.forName(args[i + 1]).newInstance();
            algeb.add(alg);
        }
        
        int count = Integer.parseInt(minCount);
        int ml = Integer.parseInt(minLength);
        int rb = Integer.parseInt(reduceBy);

        String[] nam = names.toArray(new String[names.size()]);
        Algebra[] al = algeb.toArray(new Algebra[algeb.size()]);
        
        InterpretedTreeAutomaton ita = BruteForceCorpusReader.CreateIRTGFromStrings(nam, al);
        
        Corpus c;
        try(Reader r = new FileReader(inName)) {
            c = Corpus.readCorpusLenient(r, ita);
        }
        
        Reductioner red = new Reductioner(count, c, nam[0],rb,ml);
        
        try(Reader r = new FileReader(reduceName)) {
            c = Corpus.readCorpusLenient(r, ita);
        }
        
        try(Writer w = new FileWriter(outName)) {
            BruteForceCorpusReader.writeCorpus(ita, new FunctionIterable<>(c,(Instance i) -> red.reduce(i, nam[0])), w);
        }
    }
}
