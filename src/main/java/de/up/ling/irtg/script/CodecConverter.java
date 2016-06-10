/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.codec.CodecMetadata;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.util.Util;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a corpus from one codec to another. The input file is assumed to
 * represent a list of instances, one per line, using some input codec; blank lines are allowed.
 * This tool will iterate over these instances and convert each into a string, using
 * given output codecs, which it writes into an output file. Each instance may
 * be converted by multiple output codecs or just a single one.
 * 
 * @author koller
 */
public class CodecConverter {
    private static JCommander jc;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        CmdLineParameters param = new CmdLineParameters();
        jc = new JCommander(param, args);

        if (param.help) {
            usage(null);
        }

        if (param.listInputCodecs) {
            listInputCodecs();
            System.exit(0);
        }

        if (param.listOutputCodecs) {
            listOutputCodecs();
            System.exit(0);
        }

        if (param.inputCodecName == null) {
            System.err.println("You must specify an input codec using the --input-codec option.");
            System.exit(1);
        }

        if (param.outputCodecSpec == null) {
            System.err.println("You must give an output codec specification using the --output-codec option.");
            System.exit(1);
        }

        InputCodec ic = InputCodec.getInputCodecByName(param.inputCodecName);
        if (param.codecOptions.containsKey(param.inputCodecName)) {
            ic.addOptions(param.codecOptions.get(param.inputCodecName));
        }

        List<OutputCodec> ocs = new ArrayList<>();
        for (String ocname : param.outputCodecSpec.split(",")) {
            OutputCodec oc = null;
            if ("blank".equals(ocname)) {
                oc = new BlankLineOutputCodec();
            } else {
                oc = OutputCodec.getOutputCodecByName(ocname);
            }

            if (param.codecOptions.containsKey(ocname)) {
                oc.addOptions(param.codecOptions.get(ocname));
            }

            ocs.add(oc);
        }

        System.err.println("Converting from " + ic.getMetadata().name() + " to " + String.join(", ", Util.mapToList(ocs, oc -> oc.getMetadata().name())) + ".");

        int i = 1;
        for (String infile : param.inputFiles) {
            String outfile = infile + ".conv";

            System.err.println(String.format("[%02d] %s -> %s", i++, infile, outfile));

            OutputStream os = new FileOutputStream(outfile);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
            BufferedReader r = new BufferedReader(new FileReader(infile));
            String line = null;
            int lineNo = 1;

            while ((line = r.readLine()) != null) {
                String strip = line.trim();
                if (strip.length() > 0) {
                    try {
                        Object x = ic.read(strip);

                        for (OutputCodec oc : ocs) {
                            oc.write(x, os);
                            pw.println();
                            pw.flush();
                        }
                    } catch (CodecParseException e) {
                        System.err.println(String.format("WARNING: Exception in line %d: %s", lineNo, e.toString()));

                        if (param.errorLine != null) {
                            for (OutputCodec oc : ocs) {
                                if (oc.getClass() == BlankLineOutputCodec.class) {
                                    pw.println();
                                } else {
                                    pw.println(param.errorLine + " - input line " + lineNo
                                    );
                                }
                            }
                            pw.flush();
                        }
                    }
                }

                lineNo++;
            }

            os.flush();
            os.close();
        }

        System.err.println("Done.");

        if (param.inputFiles.isEmpty()) {
            usage("No input files specified.");
        }
    }

    public static void listInputCodecs() {
        for (InputCodec ic : InputCodec.getAllInputCodecs()) {
            System.out.println(ic.getMetadata());
        }
    }

    public static void listOutputCodecs() {
        for (OutputCodec oc : OutputCodec.getAllOutputCodecs()) {
            System.out.println(oc.getMetadata());
        }
    }

    private static class CmdLineParameters {
        @Parameter
        public List<String> inputFiles = new ArrayList<>();

        @Parameter(names = {"--input-codec", "-ic"}, description = "Use the input codec with the given name.")
        public String inputCodecName = null;

        @Parameter(names = {"--output-codecs", "-oc"}, description = "Use the given output codecs.")
        public String outputCodecSpec = null;

        @DynamicParameter(names = {"-O"}, description = "Specify input or output codec option: -O<codec>=<option>:<value>,<option>:<value>")
        public Map<String, String> codecOptions = new HashMap<>();

        @Parameter(names = {"--list-input-codecs", "-li"}, description = "List all input codecs.")
        public boolean listInputCodecs = false;

        @Parameter(names = {"--list-output-codecs", "-lo"}, description = "List all output codecs.")
        public boolean listOutputCodecs = false;

        @Parameter(names = {"--error-line"}, description = "Line to print instead of outputs when input could not be parsed.")
        public String errorLine = null;

        @Parameter(names = "--help", help = true, description = "Print usage information.")
        private boolean help;
    }

    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println("No input files specified.");
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.CodecConverter <inputfiles>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    @CodecMetadata(name = "blank", description = "Prints a blank line", type = Object.class)
    private static class BlankLineOutputCodec extends OutputCodec<Object> {
        @Override
        public void write(Object object, OutputStream ostream) throws IOException, UnsupportedOperationException {

        }
    }
}
