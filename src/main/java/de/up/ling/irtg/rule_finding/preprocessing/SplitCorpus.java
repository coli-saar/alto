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
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author teichmann
 */
public class SplitCorpus {
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
        String outName1 = args[1];
        String outName2 = args[2];
        String putIntoFirst = args[3];

        List<String> names = new ArrayList<>();
        List<Algebra> algeb = new ArrayList<>();

        for (int i = 4; i < args.length; i += 2) {
            names.add(args[i]);

            Algebra alg = (Algebra) Class.forName(args[i + 1]).newInstance();
            algeb.add(alg);
        }

        String[] nam = names.toArray(new String[names.size()]);
        Algebra[] al = algeb.toArray(new Algebra[algeb.size()]);

        Reader input = new FileReader(inName);
        Corpus corp = BruteForceCorpusReader.read(input, nam, al);

        Corpus output = new Corpus();

        int size = Integer.parseInt(putIntoFirst);

        InterpretedTreeAutomaton ita = BruteForceCorpusReader.CreateIRTGFromStrings(nam, al);
        
        Iterator<Instance> insts = corp.iterator();
        
        try(Writer writ = new FileWriter(outName1)) {
            CorpusWriter cw = new CorpusWriter(ita, "", writ);
            
            for(int i=0;i<size && insts.hasNext();++i) {
                cw.writeInstance(insts.next());
            }
        }
        
        try(Writer writ = new FileWriter(outName2)) {
            CorpusWriter cw = new CorpusWriter(ita, "", writ);
            
            while(insts.hasNext()) {
                cw.writeInstance(insts.next());
            }
        }
    }
}
