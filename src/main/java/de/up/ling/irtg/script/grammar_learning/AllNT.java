/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.rule_finding.Variables;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author christoph_teichmann
 */
public class AllNT {

    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);

        String inFolder = props.getProperty("inputFolder");
        String outFolder = props.getProperty("outputFolder");
        String comment = props.getProperty("comment");

        File incoming = new File(inFolder);
        File outgoing = new File(outFolder);

        outgoing.mkdirs();

        File[] children = incoming.listFiles();

        for (File f : children) {
            String outName = outgoing.getAbsolutePath()+File.separator+f.getName();
            
            try (BufferedReader br = new BufferedReader(new FileReader(f));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(outName))) {
                String line;
                boolean first = true;

                while ((line = br.readLine()) != null) {
                    if (first) {
                        first = false;
                    } else {
                        bw.newLine();
                    }

                    if (line.startsWith(comment)) {
                        bw.write(line);
                    } else {
                        bw.write(line.replaceAll("__X__\\{[^{}]*\\}", Variables.createVariable("NT")));
                    }
                }
            }
        }
    }
}
