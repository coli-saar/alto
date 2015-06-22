/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.corpus.Charts;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This reads an (annotated or unannotated) corpus, parses all inputs,
 * and saves the parse charts into a separate file.<p>
 * 
 * Usage: java CorpusParser &lt;IRTG&gt; &lt;corpus&gt; &lt;chart file&gt;
 * 
 * @author koller
 */
public class CorpusParser {
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, CodecParseException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(args[0]));
        OutputStream ostream = new FileOutputStream(args[2]);
        Corpus corpus = Corpus.readCorpus(new FileReader(args[1]), irtg);
        Charts.computeCharts(corpus, irtg, ostream);
    }
}
