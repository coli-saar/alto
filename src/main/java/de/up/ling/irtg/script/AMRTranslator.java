/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.algebra.StringAlgebra;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates the AMR format (as of v1_4 of the Little Prince) to the Alto corpus format.
 * First parameter is source file path, second is target file path.
 * Third parameter is 0 for the format as of v1_4 of the Little Prince, and
 * 1 for the larger AMR bank we have stored (TODO: find out what it is called).
 * 
 * @author groschwitz
 */
public class AMRTranslator {
    private static final Pattern sentenceSplitPattern = Pattern.compile("(\\S+)\\s+(.*)\\s+(\\S+)");
    
    public static void main(String[] args) {


        try {
            FileReader corpusReader = new FileReader(args[0]);
            BufferedReader br = new BufferedReader(corpusReader);
            FileWriter targetFileWriter = new FileWriter(args[1]);
            
            switch (Integer.parseInt(args[2])) {
                case 0:
                    parseLittlePrinceFormat(br, targetFileWriter);
                    break;
                case 1:
                    parseLargeAMRFormat(br, targetFileWriter);
                    break;
            }
            
            targetFileWriter.close();
            br.close();
            
            
            
            
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }
    }
    
    private static void parseLittlePrinceFormat(BufferedReader br, FileWriter targetFileWriter) throws IOException {
        writeHeader(targetFileWriter);
        int state = 0; // 0 = preamble; 1 = just read AMR; 2 = now reading AMR; 3 = just read sentence (assumes that sentence is exactly one line).
        int id = 1;
        StringAlgebra stringAlgebra = new StringAlgebra();
        StringBuilder currentAmr = new StringBuilder();
        while (true) {
            String line = br.readLine();

            if (line == null) {
                break;
            } else {
                if (line.matches("\\s*")) {
                    switch (state) {
                        case 0:
                            state = 1;
                            break;

                        case 1:
                            break;//just continue

                        case 2:
                            targetFileWriter.write(currentAmr.toString().trim()+"\n\n");
                            currentAmr = new StringBuilder();
                            id++;

                            state = 1;
                            break;
                        case 3: 
                            break;//just continue
                    }
                } else {
                    switch (state) {
                        case 0:
                            break;

                        case 1:
                            Matcher m = sentenceSplitPattern.matcher(line);
                            if (m.matches()) {
                                targetFileWriter.write( "\n"+ "# " + line + "\n\n");
                                targetFileWriter.write(stringAlgebra.representAsString(stringAlgebra.parseString(m.group(2)))+"\n\n");
                            }
                            state = 3;
                            break;

                        case 2:
                        case 3:
                            currentAmr.append(" "+line.trim());//to get rid of additional spaces, for readability.
                            state = 2;
                            break;

                    }
                }
            }
        }
    }
    
    private static void parseLargeAMRFormat(BufferedReader br, FileWriter targetFileWriter) throws IOException {
        writeHeader(targetFileWriter);
        int state = 0; // 0 = not reading AMR; 1 = reading AMR;
        StringBuilder currentAmr = new StringBuilder();
        while (true) {
            String line = br.readLine();

            if (line == null) {
                break;
            } else {
                if (line.startsWith("# ::snt")) {
                    targetFileWriter.write(line.substring("# ::snt ".length()) + "\n\n");
                } else if (line.startsWith("#") || line.length() == 0) {
                    if (state == 1) {
                        //then we were reading the AMR and are now done.
                        targetFileWriter.write(currentAmr.toString().trim()+"\n\n");
                        currentAmr = new StringBuilder();
                        state = 0;
                    }
                    //else do nothing with this line
                } else {
                    //then we are in an AMR
                    state = 1;
                    currentAmr.append(" "+line.trim());//trim for readability, appending whitespace is necessary to mark new edge etc
                }
            }
        }
    }
    
    //taken from CorpusWriter#makeHeader and adapted to the specific situation.
    private static void writeHeader(FileWriter writer) throws IOException {
        StringBuilder buf = new StringBuilder();

        buf.append("# IRTG unannotated corpus file, v1.0\n");
        buf.append("# \n");
        

        buf.append("# interpretation string : de.up.ling.irtg.algebra.StringAlgebra\n");
        buf.append("# interpretation graph : de.up.ling.irtg.algebra.graph.GraphAlgebra\n");

        buf.append("\n");
        
        writer.write(buf.toString());
    }
    
   
}
