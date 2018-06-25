/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import de.saar.coli.amrtagging.Alignment;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.MutableInteger;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to more or less arbitrarily align remaining unaligned nodes.
 * For now, only applies to repalignment and repalignmentp.
 * @author JG
 */
public class FixUnalignedNodes {
    
    /**
     * Runs fixUnalignedWords with the first argument as the corpus path,
     * and the second argument as maxrange.
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException {
        fixUnalignedNodes(args[0], Integer.parseInt(args[1]));
    }
    
    /**
     * Runs over all graphs in a corpus, and if a node is unaligned, aligns it
     * to the word closest to a word that a neighboring node is aligned to.
     * Ties for 'closest' are broken arbitrarily, and a word may not have more
     * distance than maxRange.
     * For now, only applies to repalignment and repalignmentp.
     * @param corpusPath
     * @param maxRange
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void fixUnalignedNodes(String corpusPath, int maxRange) throws IOException, CorpusReadingException {
        Map<String, Interpretation> interps = new HashMap<>();
        
        Signature dummySig = new Signature();
        interps.put("graph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("tree", new Interpretation(new TreeWithAritiesAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("string", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("repgraph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("reptree", new Interpretation(new TreeWithAritiesAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("repstring", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("alignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("repalignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("alignmentp", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("repalignmentp", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        interps.put("spanmap", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        loaderIRTG.addAllInterpretations(interps);
        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader(corpusPath), loaderIRTG);
        Corpus retCorpus = new Corpus();
        
        FileWriter alignmentW = new FileWriter(corpusPath.substring(0, corpusPath.length()-".corpus".length())+".fixedAlign");
        
        MutableInteger success = new MutableInteger(0);
        MutableInteger successP = new MutableInteger(0);
        
        int i = 0;
        for (Instance inst : corpus) {
            List<String> als = (List)inst.getInputObjects().get("repalignment");
            List<String> pals = (List)inst.getInputObjects().get("repalignmentp");
            SGraph graph = (SGraph)inst.getInputObjects().get("repgraph");
            List<String> sent = (List)inst.getInputObjects().get("repstring");
            
            if (i == 95) {
                System.err.println();
            }
            int temp = success.getValue();
            List<Alignment> newAls = addAlignments(als, sent, graph, success, true, maxRange);
            if (temp == success.getValue()) {
                System.err.println("Failed: "+i + " (size "+graph.getGraph().vertexSet().size()+")");
            }
            List<Alignment> newPAls = addAlignments(pals, sent, graph, successP, false, maxRange);
            
            Instance newInst = new Instance();
            newInst.setInputObjects(new HashMap<>());
            newInst.getInputObjects().putAll(inst.getInputObjects());
            
            newInst.getInputObjects().put("repalignment", newAls.stream().map(al -> al.toString()).collect(Collectors.toList()));
            alignmentW.write(newAls.stream().map(al -> al.toString()).collect(Collectors.joining(" "))+"\n");
            newInst.getInputObjects().put("repalignmentp", newPAls.stream().map(al -> al.toString()).collect(Collectors.toList()));
            
            ((SGraph)newInst.getInputObjects().get("graph")).setWriteAsAMR(true);
            ((SGraph)newInst.getInputObjects().get("repgraph")).setWriteAsAMR(true);
            
            retCorpus.addInstance(newInst);
            if ((i+1) % 500 == 0) {
                System.err.println("Successes for absolute alignments: "+success.getValue()+"/"+i);
                System.err.println("Successes for probabilistic alignments: "+successP.getValue()+"/"+i);
            }
            i++;
        }
        
        new CorpusWriter(loaderIRTG, " with added alignments for unaligned stuff", "///###",
                new FileWriter(corpusPath.substring(0, corpusPath.length()-".corpus".length())+"_AlsFixed.corpus")).writeCorpus(retCorpus);
        alignmentW.close();
    }
    
    
    
    private static List<Alignment> addAlignments(List<String> als, List<String> sent, SGraph graph, MutableInteger successes, boolean onlyFirst, int maxRange) {
        List<Alignment> newAlignments = new ArrayList<>();//contains all alignments we want to use in the end
        Map<String, IntSet> nn2Indices = new HashMap<>();//contains all aligned node names, and which indices they are aligned to (sets are singletons for the fixed (i.e. non-p) alignments)
        
        IntSet unalignedWords = new IntOpenHashSet();//contains the indices of all still unaligned words -- we don't want to use the aligned ones
        for (int i = 0; i<sent.size(); i++) {
            unalignedWords.add(i);
        }
        
        //insert all alignments, we can move from there
        for (Alignment al : als.stream().map(s -> Alignment.read(s)).collect(Collectors.toList())) {
            newAlignments.add(al);
            for (String nn : al.nodes) {
                IntSet indsHere = nn2Indices.get(nn);
                if (indsHere == null) {
                    indsHere = new IntOpenHashSet();
                    nn2Indices.put(nn, indsHere);
                }
                indsHere.add(al.span.start);
                unalignedWords.remove(al.span.start);
            }
        }
        //backup-plan: if alignments are empty, align root to first word and go from there
        if (nn2Indices.isEmpty()) {
            String rootNn = graph.getNodeForSource("root");
            newAlignments.add(new Alignment(rootNn, 0));
            unalignedWords.remove(0);
            IntSet indsHere = new IntOpenHashSet();
            indsHere.add(0);
            nn2Indices.put(rootNn, indsHere);
        }
        
        int prevSize = 0;//loop while nn2Indices is growing
        while (prevSize < nn2Indices.size()) {
            prevSize = nn2Indices.size();
            for (GraphNode node : graph.getGraph().vertexSet()) {
                
                //only need to fix this, if it is unaligned
                if (!nn2Indices.containsKey(node.getName())) {
                    
                    IntSet allCandidates = new IntOpenHashSet();
                    outer:
                    for (int max = 1; max<=maxRange && allCandidates.isEmpty(); max++) {
                        for (GraphEdge edge : graph.getGraph().edgesOf(node)) {
                            GraphNode other = BlobUtils.otherNode(node, edge);
                            if (nn2Indices.containsKey(other.getName())) {
                                for (int index : nn2Indices.get(other.getName())) {
                                    for (int i = 1; i<= max; i++) {
                                        if (unalignedWords.contains(index-i)) {
                                            allCandidates.add(index-i);
                                            if (onlyFirst) {
                                                break outer;
                                            }
                                        }
                                        if (unalignedWords.contains(index+i)) {
                                            allCandidates.add(index+i);
                                            if (onlyFirst) {
                                                break outer;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!allCandidates.isEmpty()) {
                        nn2Indices.put(node.getName(), allCandidates);
                        unalignedWords.remove(allCandidates.iterator().nextInt());// it is enough to only reserve one of these
                        for (int index : allCandidates) {
                            Set<String> nodes = Collections.singleton(node.getName());
                            newAlignments.add(new Alignment(nodes, new Alignment.Span(index, index+1), nodes, 0, 0.1/(double)allCandidates.size()));
                        }
                    }
                }
            }
        }
        Set<String> remainingUnalinedNns = new HashSet(graph.getGraph().vertexSet().stream().map(node -> node.getName()).collect(Collectors.toSet()));
        remainingUnalinedNns.removeAll(nn2Indices.keySet());
        
        //this loop *should* not be infinite, since 
        int i = 0;
        while (!remainingUnalinedNns.isEmpty()) {
            
            for (String nn : new HashSet<>(remainingUnalinedNns)) {
                GraphNode node = graph.getNode(nn);
                // add it to all adjacent alignments
                boolean foundOne = false;
                for (GraphEdge edge : graph.getGraph().edgesOf(node)) {
                    GraphNode other = BlobUtils.otherNode(node, edge);
                    for (Alignment al : newAlignments) {
                        if (al.nodes.contains(other.getName())) {
                            al.nodes.add(nn);
                            IntSet indsHere = nn2Indices.get(nn);
                            if (indsHere == null) {
                                indsHere = new IntOpenHashSet();
                                nn2Indices.put(nn, indsHere);
                            }
                            indsHere.add(al.span.start);
                            foundOne = true;
                            //don't need to change newAlignments or unalignedWords directly here
                        }
                    }
                }
                if (foundOne) {
                    remainingUnalinedNns.remove(nn);
                }
            }
            i++;
            if (i>10000) {
                System.err.println("***probably infinite loop! breaking out of it.");
                break;
            }
        }
        //success if nn2Indices contains all node names
        if (graph.getGraph().vertexSet().stream().map(node -> nn2Indices.containsKey(node.getName())).collect(Collectors.minBy(Comparator.naturalOrder())).get()) {
            successes.incValue();
        } else {
            System.err.println(nn2Indices);
            System.err.println(unalignedWords);
            System.err.println(newAlignments);
        }
        return newAlignments;
    }
    
    
    
}
