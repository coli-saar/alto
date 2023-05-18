package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.binarization.BkvBinarizer;
import de.up.ling.irtg.binarization.RegularSeed;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.util.ConsoleProgressBar;
import de.up.ling.irtg.util.Util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * A command-line script for binarizing a grammar. The script will read a grammar from a file, using any {@link InputCodec}
 * that returns an {@link InterpretedTreeAutomaton}, and can write the binarized grammar to a file, using any
 * {@link OutputCodec} that can ingest InterpretedTreeAutomata.<p>
 *
 * You will need to provide a binarization spec as command-line arguments that tell the {@link BkvBinarizer} how to
 * binarize each rule and what algebras to use for the binarized grammars. See the documentation of the command-line
 * options for details.
 */
public class GrammarBinarizer {
    private static JCommander jc;

    public static void main(String[] args) throws Exception {
        CmdLineParameters param = new CmdLineParameters();
        jc = new JCommander(param);
        jc.parse(args);

        if( param.grammarName == null ) {
            System.err.println("You must specify an input grammar with the --grammar argument.");
            System.exit(1);
        }

        // read grammar
        InputCodec ic = null;
        if( param.inputCodecName != null ) {
            ic = InputCodec.getInputCodecByName(param.inputCodecName);

            if (ic == null) {
                usage("Unknown input codec: " + param.inputCodecName);
            }
        } else {
            ic = InputCodec.getInputCodecByExtension(Util.getFilenameExtension(param.grammarName));
        }

        InterpretedTreeAutomaton irtg = (InterpretedTreeAutomaton) ic.read(new FileInputStream(param.grammarName));
//        System.err.println(irtg);
//        System.exit(0);

        // lookup table for regular seeds
        Map<String, Class<RegularSeed>> regularSeedLookup = new HashMap<>();
        Iterator<Class> rsit = RegularSeed.getAllRegularSeedClasses();
        while(rsit.hasNext() ) {
            Class<RegularSeed> cl = rsit.next();
            regularSeedLookup.put(cl.getSimpleName(), cl);
            regularSeedLookup.put(cl.getCanonicalName(), cl);
        }

        // lookup table for algebras
        Map<String, Class<Algebra>> algebraLookup = new HashMap<>();
        Iterator<Class> algit = Algebra.getAllAlgebraClasses();
        while(algit.hasNext()) {
            Class<Algebra> cl = algit.next();
            algebraLookup.put(cl.getSimpleName(), cl);
            algebraLookup.put(cl.getCanonicalName(), cl);
        }

        // read binarization options
        Map<String, RegularSeed> regularSeeds = new HashMap<>();
        Map<String, Algebra> outputAlgebras = new HashMap<>();

        for( String spec : param.binarizationSpec ) {
            String[] parts = spec.split(":");
            if( parts.length != 2 && parts.length != 3) {
                System.err.printf("Illegal binarization spec: %s\n", spec);
                System.exit(1);
            }

            String interp = parts[0];
            if( ! irtg.getInterpretations().containsKey(interp)) {
                System.err.printf("Grammar does not have an interpretation called '%s'.\n", interp);
                System.exit(1);
            }

            // look up algebra from spec
            Algebra inputAlgebra = irtg.getInterpretation(interp).getAlgebra();
            Algebra outputAlgebra = inputAlgebra;

            if( parts.length > 2 ) {
                Class<Algebra> clAlg = algebraLookup.get(parts[2]);
                if( clAlg == null ) {
                    System.err.printf("Unknown algebra: '%s'\n", parts[2]);
                    System.exit(1);
                }
                outputAlgebra = clAlg.getDeclaredConstructor().newInstance();
            }

            outputAlgebras.put(interp, outputAlgebra);

            // look up regular seed from spec
            Class<RegularSeed> clRs = regularSeedLookup.get(parts[1]);
            if (clRs == null) {
                System.err.printf("Unknown regular seed: '%s'\n", parts[1]);
                System.exit(1);
            }

            regularSeeds.put(interp, clRs.getDeclaredConstructor(Algebra.class, Algebra.class).newInstance(inputAlgebra, outputAlgebra));

            System.out.printf("Interpretation '%s' will be binarized from algebra '%s' into algebra '%s'\n", interp, inputAlgebra.getClass().getCanonicalName(), outputAlgebra.getClass().getCanonicalName());
            System.out.printf("     using regular seed '%s'.\n", clRs.getCanonicalName());
        }

        if( irtg.getInterpretations().size() != regularSeeds.size() ) {
            System.err.printf("Expected binarization options for %d interpretations, but got %d.\n", irtg.getInterpretations().size(), regularSeeds.size());
            System.exit(1);
        }

        // binarize
        System.out.println();
        ConsoleProgressBar bar = new ConsoleProgressBar(60, System.out);
        BkvBinarizer binarizer = new BkvBinarizer(regularSeeds);
        InterpretedTreeAutomaton binarized = binarizer.binarize(irtg, outputAlgebras, bar.createListener());
        bar.finish();

        // write binarized IRTG to file
        OutputCodec oc = OutputCodec.getOutputCodecByName(param.outputCodecName);
        FileOutputStream os = new FileOutputStream(param.outGrammarFilename);
        oc.write(binarized, os);
        os.flush();
        os.close();

        System.out.printf("\nWrote binarized grammar to '%s' using output codec '%s.\n", param.outGrammarFilename, param.outputCodecName);
    }


    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println("ERROR: " + errorMessage + "\n");
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.GrammarBinarizer <options>");
            jc.usage();

            System.err.println("If you do not specify a new algebra for an interpretation, we will use the same as the original grammar.");
            System.err.println("You can specify regular seeds and algebras either with the fully qualified class names or just");
            System.err.println("with the short name ('BinarizingAlgebraSeed'). In the latter case, the program will prepend");
            System.err.println("'de.up.ling.irtg.algebra' for algebras and 'de.up.ling.irtg.binarization' for regular seeds.");

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }
    private static class CmdLineParameters {
        @Parameter
        public List<String> binarizationSpec = new ArrayList<>();

        @Parameter(names = {"--grammar", "-g"}, description = "Binarization options of the form <interpretation>:<regular seed>[:<new algebra>]")
        public String grammarName = null;

        @Parameter(names = {"--out-grammar", "-o"}, description = "Filename to which the binarized grammar will be written.")
        public String outGrammarFilename = "binarized.irtg";

        @Parameter(names = {"--input-codec", "-ic"}, description = "Use the input codec with the given name.")
        public String inputCodecName = null;

        @Parameter(names = {"--output-codec", "-oc"}, description = "Use the output codec with the given name.")
        public String outputCodecName = "toString";

        @Parameter(names = {"--list-input-codecs", "-li"}, description = "List all input codecs.")
        public boolean listInputCodecs = false;

        @Parameter(names = {"--list-output-codecs", "-lo"}, description = "List all output codecs.")
        public boolean listOutputCodecs = false;

        @Parameter(names = "--help", help = true, description = "Print usage information.")
        private boolean help;
    }
}
