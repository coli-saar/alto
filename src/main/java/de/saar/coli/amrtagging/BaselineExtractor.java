/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.DirectedGraph;



/**
 * Extracts the neural network training data for the baseline model of
 * 'AMR Dependency Parsing with a Typed Semantic Algebra' (ACL2018)
 * @author matthias
 */
public class BaselineExtractor {
    
    /**
     * Extracts the neural network training data for the baseline model of
     * 'AMR Dependency Parsing with a Typed Semantic Algebra' (ACL2018).
     * First parameter is the path containing the corpus file (output
     * will be put into a subfolder named 'baselineData'), second
     * parameter is corpus filename (just filename, not path), and third
     * argument is path to stanford POS tagger model.
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception{
        
        String path = args[0];
        if (!path.endsWith("/")) {
            path +="/";
        }
        String outPath = path+"baselineData/";
        String corpusName = args[1];
        String posTaggerPath = args[2];
        
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton());
        Signature dummySig = new Signature();
        loaderIRTG.addInterpretation("repgraph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("repstring", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("repalignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("string", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("spanmap", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        
        String corpuspath = path+corpusName;
        new File(outPath).mkdir();
        String nodeFile = outPath+"tags.txt";
        String edgeFile = outPath+"ops.txt";
        String lexicNodesFile = outPath+"labels.txt";

        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader(corpuspath), loaderIRTG);

        PrintWriter nodeWriter = new PrintWriter(nodeFile);
        PrintWriter edgeWriter = new PrintWriter(edgeFile);
        PrintWriter lexicWriter = new PrintWriter(lexicNodesFile);
        PrintWriter sentWriter = new PrintWriter(outPath+"sentences.txt");
        FileWriter posWriter = new FileWriter(outPath+"pos.txt");
        Set<String> allPosTags = new HashSet<>();
        FileWriter literalWriter = new FileWriter(outPath+"literal.txt");
        MaxentTagger tagger = new MaxentTagger(posTaggerPath);
        HashMap<SGraph,String> lookup = new HashMap();
        int instanceI = 0;
        outer:
        for (Instance inst : corpus) {
             instanceI++;
            SGraph graph = (SGraph)inst.getInputObjects().get("repgraph");
            List<String> sent = (List)inst.getInputObjects().get("repstring");
            List<String> origSent = (List)inst.getInputObjects().get("string");
            List<String> spanmap = (List)inst.getInputObjects().get("spanmap");
//            if (!alBr.ready()) {
//                break;
//            }
            List<String> als =(List)inst.getInputObjects().get("repalignment");
            if (als.size() == 1 && als.get(0).equals("")) {
                //System.err.println("Repaired empty alignment!");
                als = new ArrayList<>();
            }
            HashMap<Integer,String> outputRepr = new HashMap();
            HashMap<String,Integer> nodesNamesToSpans = new HashMap();
            HashMap<Integer,String> nodePosToLexic = new HashMap();
            
            String[] alStrings = als.toArray(new String[0]);
            DirectedGraph<GraphNode,GraphEdge> g = graph.getGraph();
            
            //find concepts:
            for (String alString : alStrings) {
                Alignment al = Alignment.read(alString, 0);
                
                if (al.lexNodes.size() > 1){
                    continue outer;
                }
                for (String nodename : al.lexNodes){
                   nodePosToLexic.put(al.span.start, graph.getNode(nodename).getLabel());
                   graph.getNode(nodename).setLabel(DependencyExtractor.LEX_MARKER);
                   
                }
                HashSet<GraphNode> hn = new HashSet();
                SGraph ng = new SGraph();
                for (String n: al.nodes){
                    nodesNamesToSpans.put(n, al.span.start);
                    ng.addNode(n,graph.getNode(n).getLabel());
                    hn.add(graph.getNode(n));
                }
                for (GraphEdge e: g.edgeSet()){
                    GraphNode source = e.getSource();
                    GraphNode target = e.getTarget();
                    if (al.nodes.contains(source.getName()) && al.nodes.contains(target.getName())){
                        //ng.addNode(e.getSource().getName(),e.getSource().getLabel());
                        //ng.addNode(e.getTarget().getName(),e.getTarget().getLabel());
                        ng.addEdge(ng.getNode(e.getSource().getName()),ng.getNode(e.getTarget().getName()), e.getLabel());
                    } 
                }
               
               
               String strRepr = "NULL";
               if (ng.getAllNodeNames().isEmpty()){
                   System.err.println(ng);
               } else {
                    
                    Optional<GraphNode> rootNode = ng.getGraph().vertexSet().stream().max((x,y) ->
                                        Long.compare( //count edges for each node but don't include edges between nodes in ng
                                                g.edgesOf(x).stream().filter((someEdge) -> !(
                                                                    ng.containsNode(someEdge.getSource().getName())
                                                                  && ng.containsNode(someEdge.getTarget().getName()) ) ).count(),
                                                
                                                g.edgesOf(y).stream().filter((someEdge) -> !(
                                                                    ng.containsNode(someEdge.getSource().getName())
                                                                  && ng.containsNode(someEdge.getTarget().getName()) ) ).count()));
                    if (rootNode.isPresent()){
                        ng.addSource("root",rootNode.get().getName());
                    } else {
                        System.err.println("Warning about #"+instanceI);
                        ng.addSource("root", (String) ng.getAllNodeNames().toArray()[0]);
                    }
                    if (lookup.keySet().contains(ng)){
                        strRepr = lookup.get(ng);
                    } else {
                        try {
                            strRepr = ng.toIsiAmrStringWithSources();
                        } catch (UnsupportedOperationException ex){
                            //throw new Exception();
                            continue outer;
                        }
                        lookup.put(ng, strRepr);
                    }
                    }
               
               outputRepr.put(al.span.start, strRepr);
                
            }
            String o = "";
            String o2 = "";
            for (int i = 0; i<sent.size() ; i++){
                if (nodePosToLexic.get(i) == null){
                    o2 = o2 + " NULL";
                } else {
                    //if (nodePosToLexic.get(i).contains(" ")) {System.err.println("JA!");}
                    o2 = o2 + " " + nodePosToLexic.get(i).replace(" ","_");
                }
                
                if (outputRepr.get(i) == null){
                    o = o + " NULL";
                } else {
                o = o + " " +outputRepr.get(i).replaceAll(" ",DependencyExtractor.WHITESPACE_MARKER);
                }
            }

            nodeWriter.println(o.substring(1)); //remove whitespace in first position
            lexicWriter.println(o2.substring(1));
            
        //make POS and literal output, from original sentence, using span map
        List<TaggedWord> origPosTags = tagger.apply(origSent.stream().map(word -> new Word(word)).collect(Collectors.toList()));
        origPosTags.stream().forEach(t -> allPosTags.add(t.tag()));
        List<String> posTags = new ArrayList<>();
        List<String> literals = new ArrayList<>();
        for (String spanString : spanmap) {
            Alignment.Span span = new Alignment.Span(spanString);
            List<String> origWords = new ArrayList<>();
            for (int l = span.start; l<span.end; l++) {
                origWords.add(origSent.get(l));
            }
            literals.add(origWords.stream().collect(Collectors.joining("_")));
            posTags.add(origPosTags.get(span.start).tag());
        }
        posWriter.write(posTags.stream().collect(Collectors.joining(" "))+"\n");
        literalWriter.write(literals.stream().collect(Collectors.joining(" "))+"\n");
        sentWriter.println(sent.stream().collect(Collectors.joining(" ")));
        //find edges that don't belong to the concepts but connects them
        
        
        String edgeO = "";
        for (GraphEdge e : g.edgeSet()){
            GraphNode source = e.getSource();
            GraphNode target = e.getTarget();
            int sIdx = -100;
            int tIdx = -100 ;
            //try {
                sIdx = nodesNamesToSpans.get(source.getName());
                tIdx = nodesNamesToSpans.get(target.getName());
           
                if (sIdx != tIdx){
                    edgeO = edgeO + e.getLabel()+"["+sIdx+","+tIdx+"] ";
                    //System.out.println(e.getLabel()+"["+sIdx+","+tIdx+"]");
                }
            //} catch (NullPointerException ex){
            //    System.err.println(inst.toString());
            //    System.err.println(e);
            //    System.err.println(sIdx+"  "+tIdx);
            //    throw new Exception();
            //}
        }
        if (!edgeO.isEmpty()){
            edgeWriter.println(edgeO.substring(0,edgeO.length()-1));
            //System.out.println();
        } else edgeWriter.println(); //no extra edges
        }
     nodeWriter.close();
     edgeWriter.close();
     posWriter.close();
     literalWriter.close();
     sentWriter.close();
     lexicWriter.close();
    }
    
}
