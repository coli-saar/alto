/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author koller
 */
public class AnnotatedCorpusParser {
    public static void main(String[] args) throws FileNotFoundException, ParseException, IOException {
        String printThisInterpretation = (args.length > 3) ? args[3] : null;        
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(args[0]));
        OutputStream ostream = new FileOutputStream(args[2]);
        AnnotatedCorpus.parseAnnotatedCorpusWithCharts(new FileReader(args[1]), irtg, ostream, printThisInterpretation);
    }
}
