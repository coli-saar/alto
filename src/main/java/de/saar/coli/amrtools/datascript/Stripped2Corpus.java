/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jonas
 */
public class Stripped2Corpus {
    /**
     * First argument is output folder containing files raw.en and raw.amr; second
     * argument is path to stanford grammar file enlishPCFG.txt
     * @param args
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
    
        String outputPath = args[0];
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath+"/";
        }
        String pFileName = args[1];
        
        stripped2Corpus(outputPath, pFileName);
        
    }
    
    /**
     * First argument is output folder containing files raw.en and raw.amr; second
     * argument is path to stanford grammar file enlishPCFG.txt. If the latter is null,
     * a corpus without trees is created.
     * @param path
     * @param stanfordGrammarFile 
     * @throws java.io.IOException 
     */
    public static void stripped2Corpus(String path, String stanfordGrammarFile) throws IOException {
        
        //load grammar, if one is provided (only then trees are added to the corpus)
        boolean makeTrees = stanfordGrammarFile != null;
        LexicalizedParser parser = makeTrees ? LexicalizedParser.loadModel(stanfordGrammarFile) : null;
        
        //create a new tokenizer (does not need grammar)
        TokenizerFactory<CoreLabel> tokenizerFactory = 
            PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
        
        //input and output files
        BufferedReader enRD = new BufferedReader(new FileReader(path+"raw.en"));
        BufferedReader amrRD = new BufferedReader(new FileReader(path+"raw.amr"));
        FileWriter resWR = new FileWriter(path+"raw.corpus");
        
        //corpus header
        String treeHeaderLine = makeTrees ? "/// interpretation tree: class de.up.ling.irtg.algebra.TreeWithAritiesAlgebra\n" : "";
        resWR.write("/// IRTG unannotated corpus file, v1.0\n" +
            "///\n" +
            "/// Semeval 2017 train corpus\n" +
            "///\n" +
            "/// interpretation string: class de.up.ling.irtg.algebra.StringAlgebra\n" +
            treeHeaderLine +
            "/// interpretation graph: class de.up.ling.irtg.algebra.graph.GraphAlgebra\n" +
            "\n\n");
        
        //iterate over instances
        int i = 0;
        while (enRD.ready() && amrRD.ready()) {
            
            //sentence
            String line = enRD.readLine();
            Tokenizer<CoreLabel> tok =
                tokenizerFactory.getTokenizer(new StringReader(line));
            List<CoreLabel> rawWords = tok.tokenize();
            resWR.write("[string] "+rawWords.stream().map(coreLabel -> coreLabel.originalText()).collect(Collectors.joining(" "))+"\n");
            
            //tree
            if (makeTrees) {
                edu.stanford.nlp.trees.Tree tree = parser.apply(rawWords);
                if (rawWords.size() != tree.getLeaves().size()) {
                    System.err.println("Unequal word and tree leaf count! "+rawWords.size()+" vs "+tree.getLeaves().size());
                    System.err.println(rawWords);
                    System.err.println(tree);
                }
                resWR.write("[tree] "+tree.toString()+"\n");
            }
            
            //amr
            String amr = amrRD.readLine();
            resWR.write("[graph] "+amr+"\n\n");
            i++;
            if (i%200 == 0) {
                System.err.println(i);
            }
        }
        
        enRD.close();
        amrRD.close();
        resWR.close();
    }
    
}
