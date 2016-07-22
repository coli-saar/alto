/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This script intends to give all graphs in an AMR corpus explicit node names.
 * The regular expression might need to be updated for different corpora.
 * @author groschwitz
 */
public class MakeAnonymousAMRNodesExplicit {
    
    private static int i = 0;
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String regex = "([^:])(:[A-Za-z0-9-]+) (\"[^\"]\"|\\+|\\-|interrogative|expressive|imperative|__NE__|__UNKNOWN__|[0-9.,]+)";
        BufferedReader corpus = new BufferedReader(new FileReader(args[0]));
        
        Pattern p = Pattern.compile(regex);
        FileWriter writer = new FileWriter(args[1]);
        corpus.lines().forEach(line -> {
            String res = line;
            Matcher m = p.matcher(line);
            while (m.find()) {
                res = m.replaceFirst("$1$2 (explicitanon"+i+" / $3)");
                m.reset(res);
                i++;
            }
            try {
                writer.write(res+"\n");
            } catch (IOException ex) {
                System.err.println(ex);
            }
            //System.err.println("line done");
        });
        
        
        writer.close();
    }
    
}
