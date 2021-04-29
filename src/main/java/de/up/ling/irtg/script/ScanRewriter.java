package de.up.ling.irtg.script;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.SynchronousCfgInputCodec;
import de.up.ling.tree.Tree;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanRewriter {
    private final static String REGEX = "IN:\\s*([^O]*)(.*)";
    private final static Pattern PATTERN = Pattern.compile(REGEX);

    public static void main(String[] args) throws IOException, ParserException {
        String grammarFilename = args[0];
        String corpusFilename = args[1];

        InterpretedTreeAutomaton irtg = new SynchronousCfgInputCodec().read(new FileInputStream(grammarFilename));
        Interpretation rightInterp = irtg.getInterpretation("right");
        Algebra rightAlgebra = rightInterp.getAlgebra();

        try (BufferedReader br = new BufferedReader(new FileReader(corpusFilename))) {
            String corpusLine = null;

            while( (corpusLine = br.readLine()) != null ) {
                Matcher m = PATTERN.matcher(corpusLine);
                if( m.matches() ) {
                    TreeAutomaton chart = irtg.parse(Map.of("left", m.group(1)));
                    Tree<String> t = chart.viterbi();

                    if( t == null ) {
                        System.err.printf("Could not parse: %s\n", m.group(1));
                        System.exit(1);
                    }

                    String rewritten = rightAlgebra.representAsString(rightInterp.interpret(t));
                    System.out.printf("IN: %s %s\n", rewritten, m.group(2));
                } else {
                    System.out.println(corpusLine);
                }
            }
        }
    }
}
