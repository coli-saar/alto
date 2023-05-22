/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.coarse_to_fine.CoarseToFineParser;
import de.up.ling.irtg.automata.coarse_to_fine.FineToCoarseMapping;
import de.up.ling.irtg.automata.coarse_to_fine.GrammarCoarsifier;
import de.up.ling.irtg.automata.pruning.NoPruningPolicy;
import de.up.ling.irtg.codec.AlgebraStringRepresentationOutputCodec;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.laboratory.BasicOperations;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General-purpose script for parsing a corpus with an IRTG.
 * The primary input is a {@link Corpus} and an {@link InterpretedTreeAutomaton}
 * with which it is parsed. You can specify the interpretations from which
 * the input should be taken and the output interpretations into which the parses
 * should be evaluated.
 * <p>
 *
 * If the corpus contains tree-based interpretations (e.g. into a {@link TreeAlgebra}),
 * the script can automatically evaluate its output against the gold annotations on
 * that interpretation using the Parseval measures.<p>
 *
 * The script also offers a number of options for controlling the parsing algorithm
 * that is to be used.
 *
 * @author koller
 */
public class ParsingEvaluator {

    private static JCommander jc;

    public static void main(String[] args) throws Exception {
        CmdLineParameters param = new CmdLineParameters();
        jc = new JCommander(param);
        jc.parse(args);

        if (param.help) {
            usage(null);
        }

        if (param.inputFiles.isEmpty()) {
            usage("No input files specified.");
        }

        if (param.grammarName == null) {
            usage("No grammar specified.");
        }

        if (param.inputInterpretations == null) {
            usage("No input interpretations specified.");
        }

        InputCodec<InterpretedTreeAutomaton> icGrammar = InputCodec.getInputCodecByNameOrExtension(param.grammarName, null);
        InterpretedTreeAutomaton irtg = icGrammar.read(new FileInputStream(param.grammarName)); // generalize to arbitrary input codecs  InterpretedTreeAutomaton.read(new FileInputStream(param.grammarName));
        List<String> interpretations = Arrays.asList(param.inputInterpretations.split(","));
        List<String> outputInterpretations = new ArrayList<>(param.outputCodecs.keySet());
        String firstInterp = interpretations.get(0); // will be used to display instance
        Algebra firstAlgebra = irtg.getInterpretation(firstInterp).getAlgebra();
        PrintWriter out = new PrintWriter(new FileWriter(param.outCorpusFilename));

        DoubleList precisions = new DoubleArrayList();
        DoubleList recalls = new DoubleArrayList();
        IntList precisionWeights = new IntArrayList();
        IntList recallWeights = new IntArrayList();

        Map<String, OutputCodec> ocForInterpretation = new HashMap<>();
        for (String interp : outputInterpretations) {
            String ocName = param.outputCodecs.get(interp);
            OutputCodec oc = null;

            if ("alg".equals(ocName)) {
                Interpretation i = irtg.getInterpretation(interp);
                oc = new AlgebraStringRepresentationOutputCodec(i.getAlgebra());
            } else {
                oc = OutputCodec.getOutputCodecByName(ocName);
            }

            if (oc == null) {
                System.err.println("Could not resolve output codec '" + ocName + "' for interpretation '" + interp + "'.");
                System.exit(1);
            } else {
                ocForInterpretation.put(interp, oc);
            }
        }

        // validate parsing algorithm
        // default/condensed/siblingfinder
        Algorithm algorithm = Algorithm.lookup(param.algorithm);

        if( algorithm == null ) {
            System.err.printf("Unknown parsing algorithm '%s'. Valid choices are default, condensed, siblingfinder.");
            System.exit(1);
        }

        if( param.ctf != null && algorithm != Algorithm.DEFAULT ) {
            System.err.printf("Coarse-to-fine parsing is only supported in conjunction with the 'default' parsing algorithm.");
            System.exit(1);
        }

        if( algorithm == Algorithm.SIBLING_FINDER ) {
            if (interpretations.size() != 1) {
                System.err.println("Sibling-finder parsing only supports a single input interpretation.");
                System.exit(1);
            }
        }

        // initialize coarse-to-fine parsing if requested
        CoarseToFineParser coarseToFineParser = null;
        String ctfInterpretation = null;

        if (param.ctf != null) {
            if (interpretations.size() != 1) {
                System.err.println("Coarse-to-fine parsing only supports a single input interpretation.");
                System.exit(1);
            }
            
            System.err.printf("Reading fine-to-coarse symbol map from %s ...\n", param.ctf);

            ctfInterpretation = interpretations.iterator().next();
            coarseToFineParser = makeCoarseToFineParserFromFile(irtg, ctfInterpretation, param.ctf, 1e-4);
        }

        // create chartsDirectory if it doesn't exist; make it empty otherwise
        File chartsDirectory = null;

        if( param.chartsDirectory != null ) {
            chartsDirectory = new File(param.chartsDirectory);

            if( ! chartsDirectory.exists() ) {
                chartsDirectory.mkdirs();
            } else {
                File[] allContents = chartsDirectory.listFiles();
                if (allContents != null) {
                    for (File file : allContents) {
                        file.delete();
                    }
                }
            }
        }

        long overallStart = System.nanoTime();

        for (String filename : param.inputFiles) {
            Corpus corpus = irtg.readCorpus(new FileReader(filename));
            System.err.println("Processing " + filename + " (" + corpus.getNumberOfInstances() + " instances) ...");
            int width = (int) (Math.ceil(Math.log10(corpus.getNumberOfInstances())));
            String formatString = "%0" + width + "d [%-50.50s] ";
            int pos = 1;

            for (Instance inst : corpus) {
                System.err.printf(formatString, pos, firstAlgebra.representAsString(inst.getInputObjects().get(firstInterp)));

                long start = System.nanoTime();

                TreeAutomaton chart = null;

                // select parsing algorithm
                switch(algorithm) {
                    case DEFAULT:
                        if (param.ctf == null) {
                            chart = irtg.parseInputObjects(inst.getRestrictedInputObjects(interpretations));
//                    dt = parseViterbi(irtg, inst, interpretations);
                        } else {
                            Object inp = inst.getInputObjects().get(ctfInterpretation);
                            chart = coarseToFineParser.parseInputObject(inp);
//                    dt = parseCtf(coarseToFineParser, inst, ctfInterpretation);
                        }
                        break;

                    case CONDENSED_BOTTOM_UP:
                        Map<String,Object> input = inst.getRestrictedInputObjects(interpretations);
                        chart = irtg.parseCondensedWithPruning(input, new NoPruningPolicy());
                        break;

                    case SIBLING_FINDER:
                        String interp = interpretations.get(0); // already checked above that there is only one
                        Object inp = inst.getInputObjects().get(interp);
                        chart = irtg.parseWithSiblingFinder(interp, inp);

//                        System.err.printf("Interpr: %s\n", interp);
//                        System.err.printf("Value: %s\n", inp);
//                        System.err.printf("Chart:\n%s\n", chart);
//                        System.exit(0);

                        break;
                }

                Tree<String> dt = chart.viterbi();


//                TreeAutomaton chart = irtg.parseInputObjects(inst.getRestrictedInputObjects(interpretations));
//                Tree<String> dt = chart.viterbi();
                System.err.print(Util.formatTimeSince(start));


                // write chart into file if requested
                if( chartsDirectory != null ) {
                    File chartsFile = new File(chartsDirectory, String.format("chart_%d.irtb", pos));
                    OutputStream os = new FileOutputStream(chartsFile);
                    new BinaryIrtgOutputCodec().write(chart.asInterpretedTreeAutomaton(), os);
                    os.flush();
                    os.close();
                }

                // write to output corpus
                if( ! param.noDerivations ) {
                    out.println(dt);
                }

                Map<String, Object> results = null;
                try {
                    results = irtg.interpret(dt);
                    for (String interp : outputInterpretations) {
                        if (dt == null) {
                            out.println("<null>");
                        } else {
                            OutputCodec oc = ocForInterpretation.get(interp);
                            out.println(oc.asString(results.get(interp)));
                        }
                    }
                } catch (Exception e) {
                    System.err.printf(" ** %s", e.getMessage());
                }

                if (param.blankLines) {
                    out.println();
                }

                out.flush();

                // collect parseval measures
                if (param.parseval != null) {
                    Tree gold = (Tree) inst.getInputObjects().get(param.parseval);

                    if (dt == null || results == null) {
                        // no parse found, or parse could not be interpreted
                        precisions.add(1);
                        recalls.add(0);

                        precisionWeights.add(0);
                        recallWeights.add(TreeAlgebra.countBrackets(gold));
                    } else {
                        Tree found = (Tree) results.get(param.parseval);

                        double P = TreeAlgebra.precision(found, gold) * 100;
                        double R = TreeAlgebra.recall(found, gold) * 100;
                        System.err.printf(", P=%02.0f R=%02.0f", P, R);

                        precisions.add(P);
                        recalls.add(R);
                        precisionWeights.add(TreeAlgebra.countBrackets(found));
                        recallWeights.add(TreeAlgebra.countBrackets(gold));
                    }
                }

                // new line in stderr output
                System.err.println();
                pos++;
            }
        }

        out.flush();
        out.close();

        System.err.println("Done, total time: " + Util.formatTimeSince(overallStart));

        if (param.parseval != null) {
            double overallP = BasicOperations.weightedAverageWithIntWeights(precisions.toArray(), precisionWeights.toArray());
            double overallR = BasicOperations.weightedAverageWithIntWeights(recalls.toArray(), recallWeights.toArray());
            System.err.printf("Overall precision = %05.2f, recall = %05.2f, F1 = %05.2f\n", overallP, overallR, 2 * overallP * overallR / (overallP + overallR));
        }
    }
    
    private static CoarseToFineParser makeCoarseToFineParserFromFile(InterpretedTreeAutomaton irtg, String interpretation, String ftcMapFilename, double theta) throws IOException, ParseException {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(StringTools.slurp(new FileReader(ftcMapFilename)));
        return new CoarseToFineParser(irtg, interpretation, ftc, theta);
    }

    private static Tree<String> parseViterbi(InterpretedTreeAutomaton irtg, Instance inst, List<String> interpretations) {
        TreeAutomaton chart = irtg.parseInputObjects(inst.getRestrictedInputObjects(interpretations));
        Tree<String> dt = chart.viterbi();
        return dt;
    }


    public static Tree<String> parseCtf(CoarseToFineParser ctfp, Instance inst, String interpretation) {
        Object inp = inst.getInputObjects().get(interpretation);
        TreeAutomaton chart = ctfp.parseInputObject(inp);

//        System.err.println(chart);
        Tree<String> dt = chart.viterbi();

//        System.err.println(dt);
//        System.exit(0);
        return dt;
    }

    private static class CmdLineParameters {

        @Parameter
        public List<String> inputFiles = new ArrayList<>();

        @Parameter(names = {"--grammar", "-g"}, description = "IRTG to be used in parsing.")
        public String grammarName = null;

        @Parameter(names = {"--input-interpretations", "-I"}, description = "Comma-separated list of interpretations from which inputs are taken.")
        public String inputInterpretations = null;

        @Parameter(names = {"--out-corpus", "-o"}, description = "Filename to which the parsed corpus will be written.")
        public String outCorpusFilename = "out.txt";

        @DynamicParameter(names = "-O", description = "Output interpretations with their output codecs (e.g. -Ostring=toString). As special case, use -Ostring=alg to use the algebra's default string representation.")
        public Map<String, String> outputCodecs = new HashMap<>();

        @Parameter(names = {"--blank-lines", "-b"}, description = "Insert a blank line between any two output instances.")
        public boolean blankLines = false;

        @Parameter(names = {"--no-derivations"}, description = "Print only the output interpretation values, not the derivation trees.")
        public boolean noDerivations = false;

        @Parameter(names = "--parseval", description = "Measure precision and recall on this interpretation.")
        public String parseval = null;

        @Parameter(names = "--ctf", description = "Perform coarse-to-fine parsing with the given CTF map file.")
        public String ctf = null;

        @Parameter(names = "--algorithm", description = "Parsing algorithm to use, options: default/condensed/siblingfinder.")
        public String algorithm = "default";

        @Parameter(names = "--save-charts", description = "Save charts in this directory. Charts will be written as IRTGs with zero interpretations using the binary IRTG codecs.")
        public String chartsDirectory = null;

        @Parameter(names = "--verbose", description = "Print some debugging output.")
        public boolean verbose = false;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }

    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println(errorMessage);
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.ParsingEvaluator <inputfiles>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    public static enum Algorithm {
        DEFAULT("default"),
        CONDENSED_BOTTOM_UP("condensed"),
        SIBLING_FINDER("siblingfinder");

        private String label;

        Algorithm(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static Algorithm lookup(String label) {
            for( Algorithm alg : Algorithm.values() ) {
                if( alg.label.equals(label)) {
                    return alg;
                }
            }
            return null;
        }
    }

}
