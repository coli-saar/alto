package de.up.ling.irtg.script;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.*;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.corpus.InterpretationPrintingPolicy;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import me.tongfei.progressbar.ProgressBar;

import java.io.*;
import java.util.*;

import static de.saar.coli.algebra.OrderedFeatureTreeAlgebra.OrderedFeatureTree;

/**
 * A command-line tool for generating a random corpus from a grammar.
 * You have detailed control over the way in which each instance is printed.
 * In particular, you can either include the "IRTG corpus" header, for later
 * processing with Alto, or you can simply print the generated instances,
 * for processing with other tools.
 *
 */
public class RandomCorpusGenerator {
    private static JCommander jc;

    public static void main(String[] args) throws IOException {
        Args cmd = new Args();
        jc = new JCommander(cmd);
        jc.parse(args);

        if (cmd.help) {
            usage(null);
        }

        System.err.printf("Generating corpus with %d instances.\n", cmd.count);

        // generate corpus
        InputCodec ic = InputCodec.getInputCodecByExtension(Util.getFilenameExtension(cmd.parameters.get(0)));
        InterpretedTreeAutomaton irtg = (InterpretedTreeAutomaton) ic.read(new FileInputStream(cmd.parameters.get(0)));

        // create printing policy and duplicates cache
        Set<String> duplicatesCache = cmd.suppressDuplicates != null ? new HashSet<>() : null;
        List<Pair<String, OutputCodec>> outputCodecs = new ArrayList<>();
        OutputCodec duplicateSuppressingInterpretationOc = null;

        for( Map.Entry<String,String> out : cmd.outputInterpretations.entrySet() ) {
            OutputCodec oc = null;

            if( "alg".equals(out.getValue()) ) {
                oc = new AlgebraStringRepresentationOutputCodec(irtg.getInterpretation(out.getKey()).getAlgebra());
            } else {
                oc = OutputCodec.getOutputCodecByName(out.getValue());
            }

            if( oc == null ) {
                System.err.printf("Could not resolve output codec for interpretation '%s': '%s'\n", out.getKey(), out.getValue());
                System.exit(1);
            }

            outputCodecs.add(new Pair(out.getKey(), oc));

            if( out.getKey().equals(cmd.suppressDuplicates) ) {
                duplicateSuppressingInterpretationOc = oc;
            }
        }

        InterpretationPrintingPolicy pp = new InterpretationPrintingPolicy(outputCodecs, new TreeAlgebra());

        // create corpus writer
        Writer w = (cmd.outputFile == null) ? new PrintWriter(System.out, false) : new FileWriter(cmd.outputFile);
        CorpusWriter cw = new CorpusWriter(irtg, cmd.comment, cmd.commentPrefix, pp, w);

        if( cmd.skipHeader ) {
            cw.skipHeader();
        }

        cw.setPrintSeparatorLines(cmd.blankLines);
        cw.setAnnotated(cmd.printDerivations);

        try (ProgressBar pb = new ProgressBar("Generating corpus", cmd.count)) {
            for (int i = 0; i < cmd.count; i++) {
                pb.stepTo(i);

                Tree<String> dt = irtg.getAutomaton().getRandomTreeFromRuleProbabilities();
                assert dt != null;

                // map to all interpretations
                Map<String,Object> values = irtg.interpret(dt);

                // check for duplicates
                if( cmd.suppressDuplicates != null ) {
                    String val = duplicateSuppressingInterpretationOc.asString(values.get(cmd.suppressDuplicates));
                    if( ! duplicatesCache.add(val) ) {
                        // instance is a duplicate => repeat this iteration
                        i--;
                        continue;
                    }
                }

                // write corpus instance
                Instance inst = new Instance();
                inst.setDerivationTree(irtg.getAutomaton().getSignature().mapSymbolsToIds(dt));
                inst.setInputObjects(values);

                cw.writeInstance(inst);
            }

            pb.stepTo(cmd.count);
        }

        System.err.printf("Generated %d instances.\n", cmd.count);
    }



    public static class Args {
        @Parameter
        private List<String> parameters = new ArrayList<>();

        @Parameter(names = "--count", description="How many instances to generate.")
        private int count = 10;

        @Parameter(names = "--suppress-duplicates", description = "Prevents generating multiple instances with the same value on this interpretation.")
        private String suppressDuplicates = null;

        @Parameter(names = "--print-derivations", description = "Print derivation tree for each instance.")
        private boolean printDerivations = false;

        @DynamicParameter(names = "-O", description = "Specifies which output interpretations should be printed and with what codec. Write '-Ointerpretation=alg' to use the algebra's default codec.")
        private Map<String, String> outputInterpretations = new LinkedHashMap<>();

        @Parameter(names = {"--output-file", "-o"}, description = "Write corpus to this file (or console if left blank).")
        private String outputFile = null;

        @Parameter(names = "--skip-header", description = "Don't write 'IRTG corpus' header at the start of the output file.")
        private boolean skipHeader = false;

        @Parameter(names = {"--blank-lines", "-b"}, description = "Insert a blank line between any two output instances.")
        public boolean blankLines = false;

        @Parameter(names = "--comment-prefix", description = "Use this string as a prefix to indicate comments.")
        public String commentPrefix = "//";

        @Parameter(names = "--comment", description = "Add this comment to the generated corpus.")
        public String comment = null;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }

    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println(errorMessage);
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.RandomCorpusGenerator [options] <inputfiles>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }
}
