/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.script;

import de.up.ling.irtg.corpus.CorpusConverter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.codec.PtbTreeInputCodec;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Iterator;

/**
 * Converts a treebank in Penn Treebank format into an
 * Alto corpus. Specify the files which make up the treebank
 * as command-line arguments. The converted corpus is written
 * to an Alto corpus file.<p>
 * 
 * The conversion uses an implicit IRTG with an interpretation
 * "string" (using a {@link StringAlgebra}) and an interpretation
 * "tree" representing the phrase-structure trees (using a {@link TreeWithAritiesAlgebra}).<p>
 * 
 * The strings and trees are taken literally from the treebank. No
 * postprocessing or normalization is performed.
 * 
 * @author koller
 */
public class PennTreebankConverter {
    public static void main(String[] args) throws Exception {
        PtbTreeInputCodec codec = new PtbTreeInputCodec();
        Writer w = new FileWriter("out.txt");

        CorpusConverter<Tree<String>> converter = new CorpusConverter<Tree<String>>(Joiner.on(" ").join(args),
                ImmutableMap.of("string", new StringAlgebra(), "tree", new TreeWithAritiesAlgebra()),
                ImmutableMap.of("string", (Tree<String> tree) -> tree.getLeafLabels(), "tree", x -> x),
                w);

        for (String filename : args) {
            System.err.println("Processing " + filename + " ...");
            InputStream corpus = new FileInputStream(filename);
            codec.readCorpus(corpus).forEach(converter);
        }

        w.flush();
        w.close();
        
        System.err.println("Done.");
    }
}
