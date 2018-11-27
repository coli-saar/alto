/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import static de.saar.coli.amrtagging.AlignmentTrackingAutomaton.SEPARATOR;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.Type;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extracts AM dependency trees from AMR graphs, that can be used as training data for a neural tagger.
 * To be used via DependencyExtractorCLI. 'Constraints' in this file means the dependency tree.
 * @author jonas
 */
public class DependencyExtractor {
    
    public static final String WHITESPACE_MARKER = "__ALTO_WS__";
    public static final String LEX_MARKER = "--LEX--";
    
    private final Map<SGraph, Map<Type, String>> graph2type2string;
    private final Writer sentWriter;
    private final Writer tagWriter;
    private final Writer labelWriter;
    private final Writer sentVocabWriter;
    private final Writer tagVocabWriter;
    private final Writer labelVocabWriter;
    private final Writer opWriter;
    private final Writer opVocabWriter;
    private final Set<String> sentVocab;
    private final Set<String> labelVocab;
    private final Set<String> opVocab;
    private final boolean fixedVocab;
    //get tagVocab from graph2string map.
    
    DependencyExtractor(String outPathPrefix) throws IOException {
        sentWriter = new FileWriter(outPathPrefix+"sentences.txt");
        tagWriter = new FileWriter(outPathPrefix+"tags.txt");
        labelWriter = new FileWriter(outPathPrefix+"labels.txt");
        sentVocabWriter = new FileWriter(outPathPrefix+"vocabSentences.txt");
        tagVocabWriter = new FileWriter(outPathPrefix+"vocabTags.txt");
        labelVocabWriter = new FileWriter(outPathPrefix+"vocabLabels.txt");
        opWriter = new FileWriter(outPathPrefix+"ops.txt");
        opVocabWriter = new FileWriter(outPathPrefix+"vocabOps.txt");
        this.graph2type2string = new HashMap<>();
        sentVocab = new HashSet<>();
        labelVocab = new HashSet<>();
        opVocab = new HashSet<>();
        fixedVocab = false;
    }
    
    /**
     * Reads graph vocab file in trainPathPrefix, so that we use the same strings
     * for the same graphs
     * @param outPathPrefix
     * @param trainPathPrefix
     * @throws IOException 
     */
    DependencyExtractor(String outPathPrefix, String trainPathPrefix) throws IOException {
        sentWriter = new FileWriter(outPathPrefix+"sentences.txt");
        tagWriter = new FileWriter(outPathPrefix+"tags.txt");
        labelWriter = new FileWriter(outPathPrefix+"labels.txt");
        sentVocabWriter = new FileWriter(outPathPrefix+"vocabSentences.txt");
        tagVocabWriter = new FileWriter(outPathPrefix+"vocabTags.txt");
        labelVocabWriter = new FileWriter(outPathPrefix+"vocabLabels.txt");
        opWriter = new FileWriter(outPathPrefix+"ops.txt");
        opVocabWriter = new FileWriter(outPathPrefix+"vocabOps.txt");
        
        //filling graph2type2string is enough
        fixedVocab = false;
        this.graph2type2string = new HashMap<>();
        BufferedReader tagBr = new BufferedReader(new FileReader(trainPathPrefix+"vocabTags.txt"));
        IsiAmrInputCodec codec = new IsiAmrInputCodec();
        while (tagBr.ready()) {
            String line = tagBr.readLine();
            if (!line.trim().equals("")) {
                String[] graphAndType = line.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP);
                SGraph g = codec.read(graphAndType[0].replaceAll(WHITESPACE_MARKER, " "));
                Type type;
                try {
                    type = new Type(graphAndType[1]);
                } catch (ArrayIndexOutOfBoundsException | ParseException ex) {
                    System.err.println("***WARNING*** could not read type in line '"+line+"'. Continuing with empty type.");
                    type = Type.EMPTY_TYPE;
                }
                g.setEqualsMeansIsomorphy(true);
                if (!graph2type2string.containsKey(g)) {
                    graph2type2string.put(g, new HashMap<>());
                }
                graph2type2string.get(g).put(type, line);
            }
        }
        sentVocab = new HashSet<>();
        labelVocab = new HashSet<>();
        opVocab = new HashSet<>();
    }
    
    List<String> tree2constraints(Tree<String> tree, Set<String> lexNodes) {
        List<String> constraints = new ArrayList<>();
        IsiAmrInputCodec codec = new IsiAmrInputCodec();
        tree.dfs((Tree<String> node, List<Void> childrenValues) -> {
            String[] label = node.getLabel().split(SEPARATOR);
            if (label[1].startsWith(ApplyModifyGraphAlgebra.OP_APPLICATION)) {
                String[] indices = label[0].split("_");
                constraints.add(label[1]+"["+indices[0]+","+indices[1]+"]");
            } else if (label[1].startsWith(ApplyModifyGraphAlgebra.OP_MODIFICATION)) {
                String[] indices = label[0].split("_");
                constraints.add(label[1]+"["+indices[0]+","+indices[1]+"]");
            } else if (label[1].startsWith(ApplyModifyGraphAlgebra.OP_COREF)) {
                //do nothing?
            } else {
                String c = label[1];
                //remove coref marker before constant
                if (label[1].startsWith(ApplyModifyGraphAlgebra.OP_COREFMARKER)) {
                    c = label[1].substring(ApplyModifyGraphAlgebra.OP_COREFMARKER.length());
                    c = c.substring(c.indexOf("_")+1);
                }
                //split graph and type
                String[] graphAndType = c.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP);
                SGraph g = codec.read(graphAndType[0]);
                Type type;
                try {
                    type = new Type(graphAndType[1]);
                } catch (ParseException ex) {
                    System.err.println("***WARNING*** could not read type!! Continuing with empty type.");
                    type = Type.EMPTY_TYPE;
                }
                g.setEqualsMeansIsomorphy(true);
                String lexLabel = "";
                for (GraphNode n : g.getGraph().vertexSet()) {
                    if (n.getLabel() != null && !n.getLabel().equals("") && lexNodes.contains(n.getName())) {
//                        if (!lexLabel.equals("")) {
//                            System.err.println("***WARNING*** multiple lex nodes!");
//                        }
                        lexLabel += n.getLabel();
                        n.setLabel(LEX_MARKER);
                    }
                }
                
                //recover graph string if we had one before
                String graphString;
                if (graph2type2string.containsKey(g) && graph2type2string.get(g).containsKey(type)) {
                    graphString = graph2type2string.get(g).get(type);
                } else if (!fixedVocab) {
                    graphString = g.toIsiAmrStringWithSources().replaceAll(" ", WHITESPACE_MARKER)+ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP+type.toString().replaceAll(" ", "");//need whitespace markers in graph to reconstruct it, but not in graph type 
                    if (!graph2type2string.containsKey(g)) {
                        graph2type2string.put(g, new HashMap<>());
                    }
                    graph2type2string.get(g).put(type, graphString);
                } else {
                    graphString = "UNK";
                }
                
                
                if (lexLabel.equals("")) {
                    System.err.println("***WARNING*** no lex node! Fragment: "+graphString);
                    lexLabel = "--NOLEX--";
                }
                constraints.add(graphString+" "+lexLabel+" "+label[0]);
            }
            return null;
        });
        return constraints;
    }
    
    void close() throws IOException {
        sentWriter.close();
        tagWriter.close();
        labelWriter.close();
        sentVocabWriter.close();
        tagVocabWriter.close();
        labelVocabWriter.close();
        opWriter.close();
        opVocabWriter.close();
    }
    
    void writeTrainingdataFromConstraints(List<String> constraints, List<String> sent) throws IOException {
        
        Int2ObjectMap<String> ind2tag = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<String> ind2label = new Int2ObjectOpenHashMap<>();
        List<String> ops = new ArrayList<>();
        for (String c : constraints) {
            String[] parts = c.split(" ");
            switch (parts.length) {
                case 3:
                    //constant
                    int index = Integer.valueOf(parts[2]);
                    if (ind2tag.containsKey(index)) {
                        System.err.println("***WARNING*** multiple tags for the same word!");
                    }
                    ind2tag.put(index, parts[0]);
                    ind2label.put(index, parts[1]);
                    break;
                case 1:
                    //operation
                    ops.add(c);
                    opVocab.add(c.split("\\[")[0]);
                    break;
                default:
                    System.err.println("**WARNING** unidentified constraint "+c+", ignoring it.");
                    break;
            }
        }
        opWriter.write(ops.stream().collect(Collectors.joining(" "))+"\n");
        ind2tag.defaultReturnValue("NULL");
        ind2label.defaultReturnValue("NULL");
        if (!fixedVocab) {
            sentVocab.addAll(sent.stream().map(word -> word.toLowerCase()).collect(Collectors.toList()));
            labelVocab.addAll(ind2label.values());
        }
        for (int i = 0; i<sent.size(); i++) {
            String space = (i == sent.size()-1) ? "\n" : " ";
            String word = sent.get(i).toLowerCase();
            if (fixedVocab) {
                if (!sentVocab.contains(word)) {
                    word = "UNK";
                }
            }
            sentWriter.write(word+space);
            tagWriter.write(ind2tag.get(i)+space);//handled UNK tag already in constraints
            String label = ind2label.get(i);
            if (fixedVocab) {
                if (!labelVocab.contains(label)) {
                    label = "UNK";
                }
            }
            labelWriter.write(label+space);
        }
    }
    
    void writeVocab() throws IOException {
        labelVocab.add("NULL");
        labelVocab.add("UNK");
        labelVocab.forEach(label -> {
            try {
                labelVocabWriter.write(label+"\n");
            } catch (IOException ex) {
                Logger.getLogger(DependencyExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        sentVocab.add("UNK");
        sentVocab.forEach(word -> {
            try {
                sentVocabWriter.write(word+"\n");
            } catch (IOException ex) {
                Logger.getLogger(DependencyExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        tagVocabWriter.write("NULL\n");
        tagVocabWriter.write("UNK\n");
        graph2type2string.values().forEach(type2string -> {
            type2string.values().forEach(string -> {
                try {
                    tagVocabWriter.write(string+"\n");
                } catch (IOException ex) {
                    Logger.getLogger(DependencyExtractor.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        });
        opVocab.add("NULL");
        opVocab.forEach(word -> {
            try {
                opVocabWriter.write(word+"\n");
            } catch (IOException ex) {
                Logger.getLogger(DependencyExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    
    
}
