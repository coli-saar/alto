/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.corpus.Charts;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This reads an (annotated or unannotated) corpus, parses all inputs,
 * and saves the parse charts into a separate file.
 * 
 * Usage: java CorpusParser <IRTG> <corpus> <chart file>
 * 
 * @author koller
 */
public class CorpusParser {
    public static void main(String[] args) throws FileNotFoundException, ParseException, IOException, CorpusReadingException {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(args[0]));
        OutputStream ostream = new FileOutputStream(args[2]);
        Corpus corpus = Corpus.readCorpus(new FileReader(args[1]), irtg);
        Charts.computeCharts(corpus, irtg, ostream);
    }
}
