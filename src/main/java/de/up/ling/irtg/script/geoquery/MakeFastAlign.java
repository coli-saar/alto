/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.geoquery;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.tree.Tree;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author christoph_teichmann
 */
public class MakeFastAlign {

    public static void main(String... args) throws IOException, CorpusReadingException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);

        String inputFile = props.getProperty("inputCorpus");
        String outputFile = props.getProperty("outputCorpus");
        String treeAlgebraName = props.getProperty("treeAlgebraName");
        String treeAlgebraType = props.getProperty("treeAlgebraType");
        String stringAlgebraName = props.getProperty("stringAlgebraName");
        String stringAlgebraType = props.getProperty("stringAlgebraType");

        Algebra stringAlg = (Algebra) Class.forName(stringAlgebraType).newInstance();
        Algebra treeAlg = (Algebra) Class.forName(treeAlgebraType).newInstance();

        Map<String, Algebra> map = new HashMap<>();
        map.put(treeAlgebraName, treeAlg);
        map.put(stringAlgebraName, stringAlg);
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(map);

        Corpus input;
        File out = new File(outputFile);
        File par = out.getParentFile();
        if (par != null) {
            par.mkdirs();
        }

        try (FileReader readIn = new FileReader(inputFile)) {
            input = Corpus.readCorpusLenient(readIn, ita);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            boolean first = true;
            for (Instance i : input) {
                String s = makeString((List<String>) i.getInputObjects().get(stringAlgebraName));
                String t = makeTreeString((Tree<String>) i.getInputObjects().get(treeAlgebraName));

                if (first) {
                    first = false;
                } else {
                    bw.newLine();
                }
                
                bw.write(s);
                bw.write(" ||| ");
                bw.write(t);
            }
        }
    }

    /**
     *
     * @param list
     * @return
     */
    private static String makeString(List<String> list) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (String s : list) {
            if (s.matches("\\|\\|\\|+")) {
                s = "||";
            }
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }

            sb.append(s);
        }

        return sb.toString();
    }

    /**
     * 
     * @param tree
     * @return 
     */
    private static String makeTreeString(Tree<String> tree) {
        StringBuilder sb = new StringBuilder();
        
        String s = tree.getLabel();
        s = s.replaceAll("\\s+", "_");
        
        sb.append(s);
        
        for(Tree<String> child : tree.getChildren()) {
            sb.append(" ");
            sb.append(makeTreeString(child));
        }
        
        return sb.toString();
    }
}
