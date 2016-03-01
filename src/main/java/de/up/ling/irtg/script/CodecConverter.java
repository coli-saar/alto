/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
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
        
        if( param.listInputCodecs ) {
            listInputCodecs();
            System.exit(0);
        }
        
        if( param.listOutputCodecs ) {
            listOutputCodecs();
            System.exit(0);
        }
        
        if( param.inputCodecName == null ) {
            System.err.println("You must specify an input codec using the --input-codec option.");
            System.exit(1);
        }
        
        if( param.outputCodecSpec == null ) {
            System.err.println("You must give an output codec specification using the --output-codec option.");
            System.exit(1);
        }
        
        InputCodec ic = InputCodec.getInputCodecByName(param.inputCodecName);
        List<OutputCodec> ocs = new ArrayList<>();
        for( String ocname : param.outputCodecSpec.split(",")) {
            if( "blank".equals(ocname)) {
                ocs.add(new BlankLineOutputCodec());
            } else {
                ocs.add(OutputCodec.getOutputCodecByName(ocname));
            }
        }
        
        for( String infile : param.inputFiles ) {
            String outfile = infile + ".conv";
            OutputStream os = new FileOutputStream(outfile);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
            BufferedReader r = new BufferedReader(new FileReader(infile));
            String line = null;
            
            while( (line = r.readLine()) != null ) {
                String strip = line.trim();
                if( strip.length() > 0 ) {
                    Object x = ic.read(strip);
                    
                    for( OutputCodec oc : ocs ) {
                        oc.write(x, os);
                        pw.println();
                        pw.flush();
                    }
                }
            }
            
            os.flush();
            os.close();
        }

        if (param.inputFiles.isEmpty()) {
            usage("No input files specified.");
        }
    }

    private static void listInputCodecs() {
        for( InputCodec ic : InputCodec.getAllInputCodecs() ) {
            System.out.println(ic.getMetadata());
        }
    }

    private static void listOutputCodecs() {
        for( OutputCodec oc: OutputCodec.getAllOutputCodecs() ) {
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
    
    private static class BlankLineOutputCodec extends OutputCodec<Object> {
        @Override
        public void write(Object object, OutputStream ostream) throws IOException, UnsupportedOperationException {

        }        
    }
}
