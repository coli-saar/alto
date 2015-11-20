/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractLinesforFastAlign {   
    
    /**
     * 
     * @param sentenceLine
     * @param funqlLine
     * @param in
     * @param out
     * @throws IOException 
     */
    public static void getGeoQueryFunql(int sentenceLine, int funqlLine, InputStream in, OutputStream out)
                                                        throws IOException{
        Function<String,String> sent = (String s) -> s;
        Function<String, String> funql = (String s) ->  {
            try {
                Tree<String> t = TreeParser.parse(s.replaceAll("\\d+", "__NUMBER__"));
                t = t.map((String q) -> q.replaceAll("\\s+", "__WHITESPACE__"));
                s = t.toString();
            } catch (ParseException ex) {
                Logger.getLogger(ExtractLinesforFastAlign.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return s.replaceAll("[\\(\\)]+", " ").replaceAll(",+", " ").replaceAll("\\s+"," ");
        };

        getLines(sent, funql, sentenceLine, funqlLine, in, out);
    }
    
    /**
     * 
     * @param sentenceLine
     * @param otherLine
     * @param in
     * @param out
     * @throws IOException 
     */
    public static void getStringToString(int sentenceLine, int otherLine, InputStream in, OutputStream out)
                                            throws IOException{
        Function<String,String> f = (String s) -> s;
        
        getLines(f, f, sentenceLine, otherLine, in, out);
    }

    /**
     *
     * @param processFirst
     * @param processSecond
     * @param firstLine
     * @param secondLine
     * @param in
     * @param out
     * @throws IOException
     */
    public static void getLines(Function<String, String> processFirst,
            Function<String, String> processSecond,
            int firstLine, int secondLine,
            InputStream in, OutputStream out) throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(in));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out))) {

            boolean first = true;
            String line;
            ArrayList<String> list = new ArrayList<>();
            while ((line = input.readLine()) != null) {
                if (line.trim().equals("")) {
                    if (!list.isEmpty()) {
                        if (first) {
                            first = false;
                        } else {
                            output.newLine();
                        }
                        dump(firstLine, secondLine, processFirst, processSecond, list, output);
                    }
                    list.clear();
                } else {
                    list.add(line.trim());
                }
            }

            if (!list.isEmpty()) {
                if (!first) {
                    output.newLine();
                }
                dump(firstLine, secondLine, processFirst, processSecond, list, output);
            }
        }
    }

    /**
     * 
     * @param firstLine
     * @param secondLine
     * @param processFirst
     * @param processSecond
     * @param list
     * @param out 
     */
    private static void dump(int firstLine, int secondLine,
            Function<String, String> processFirst, Function<String, String> processSecond,
            ArrayList<String> list, BufferedWriter out) throws IOException {
        out.write(processFirst.apply(list.get(firstLine)).trim()+" ||| "+processSecond.apply(list.get(secondLine)).trim());
    }
}
