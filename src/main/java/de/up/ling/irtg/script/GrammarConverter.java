/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.codec.ToStringOutputCodec;
import static de.up.ling.irtg.script.CodecConverter.listInputCodecs;
import static de.up.ling.irtg.script.CodecConverter.listOutputCodecs;
import de.up.ling.irtg.util.Util;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a grammar from one format to another.
 * <p>
 * The input is read from a file if one is specified on the
 * command-line, or otherwise from stdin. The input codec is determined,
 * in this order, (a) from the --input-codec command-line argument
 * if one is given, or (b) from the filename, if one is given and its
 * extension can be resolved to an input codec. If no input codec can be
 * determined, the script aborts.
 * <p>
 * The output is written to a file if one is specified with the
 * --output-file command-line argument, or otherwise to stdout.
 * The output codec is determined, in this order, (a) from the --output-codec
 * command-line argument if one is given; (b) from the output file name
 * if one was specified and its extension can be resolved to an output codec;
 * (c) the {@link ToStringOutputCodec} if no other output codec can be
 * determined.
 * 
 * @author koller
 */
public class GrammarConverter {
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
        
        
        // determine input codec and input stream
        InputCodec ic = null;
        String inputFilename = null;
        
        if( ! param.inputFiles.isEmpty() ) {
            inputFilename = param.inputFiles.get(0);
        }
        
        if( param.inputCodecName != null ) {
            ic = InputCodec.getInputCodecByName(param.inputCodecName);
            
            if( ic == null ) {
                usage("Unknown input codec: " + param.inputCodecName);
            }
        } else if( ! param.inputFiles.isEmpty() ) {            
            String ext = Util.getFilenameExtension(inputFilename);
            ic = InputCodec.getInputCodecByExtension(ext);
            
            if( ic == null ) {
                usage("Could not determine input codec from filename extension '" + ext + "'");
            }
        } else {
            usage("Specify an input codec, either explicitly or through the filename extension of the input file.");
        }
        
        InputStream is = (inputFilename == null) ? System.in : new FileInputStream(inputFilename);
        
        // determine output codec and output stream
        // If no output codec can be determined, fall back to toString.
        OutputCodec oc = null;
        
        if( param.outputCodecName != null ) {
            oc = OutputCodec.getOutputCodecByName(param.outputCodecName);
            
            if( oc == null ) {
                usage("Unknown output codec: " + param.outputCodecName);
            }
        } else if( param.outputFilename != null ) {
            String ext = Util.getFilenameExtension(param.outputFilename);
            oc = OutputCodec.getOutputCodecByExtension(ext);
            
            if( oc == null ) {
                oc = defaultOutputCodec();
            }
        } else {
            oc = defaultOutputCodec();
        }
        
        OutputStream os = (param.outputFilename == null) ? System.out : new FileOutputStream(param.outputFilename);
        
        // check compatibility
        if( ! oc.getMetadata().type().isAssignableFrom(ic.getMetadata().type())) {
            usage("The input codec '" + ic.getMetadata().name() + "' produces values of type " 
                    + ic.getMetadata().type().getSimpleName() + ", but the output codec '" + oc.getMetadata().name()
                    + " cannot encode these.");
        }
        
        // perform the conversion
        Object value = ic.read(is);
        is.close();
        
        oc.write(value, os);
        os.close();
    }

    private static OutputCodec defaultOutputCodec() {
        return new ToStringOutputCodec();
    }
    
    
    private static class CmdLineParameters {
        @Parameter
        public List<String> inputFiles = new ArrayList<>();

        @Parameter(names = {"--input-codec", "-ic"}, description = "Use the input codec with the given name.")
        public String inputCodecName = null;

        @Parameter(names = {"--output-codec", "-oc"}, description = "Use the output codec with the given name.")
        public String outputCodecName = null;

        @Parameter(names = {"--output-file", "-o"}, description = "Write the converted grammar to this file.")
        public String outputFilename;

        @Parameter(names = {"--list-input-codecs", "-li"}, description = "List all input codecs.")
        public boolean listInputCodecs = false;

        @Parameter(names = {"--list-output-codecs", "-lo"}, description = "List all output codecs.")
        public boolean listOutputCodecs = false;

        @Parameter(names = "--help", help = true, description = "Print usage information.")
        private boolean help;
    }

    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println("ERROR: " + errorMessage + "\n");
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.GrammarConverter <inputfiles>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

}
