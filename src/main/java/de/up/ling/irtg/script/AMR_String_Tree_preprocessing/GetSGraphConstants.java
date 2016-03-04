/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
public class GetSGraphConstants {
    
    
    
    public static void main(String[] args) throws IOException, CorpusReadingException {
        
        Object2IntMap<SGraph> graph2Count = new Object2IntOpenHashMap<>();
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrg.irtg")); //just for loading corpus
        
        Corpus corpus = Corpus.readCorpus(new FileReader("examples/AMRAllCorpus.txt"), irtg);
        
        
        Set<String> attachToTarget = new HashSet<>();
        attachToTarget.add("calendar");
        attachToTarget.add("century");
        attachToTarget.add("day");
        attachToTarget.add("dayperiod");
        attachToTarget.add("decade");
        attachToTarget.add("era");
        attachToTarget.add("month");
        attachToTarget.add("quarter");
        attachToTarget.add("season");
        attachToTarget.add("timezone");
        attachToTarget.add("weekday");
        attachToTarget.add("year");
        attachToTarget.add("year2");
        attachToTarget.add("unit");
        attachToTarget.add("value");
        attachToTarget.add("mode");
        attachToTarget.add("compared-to");
        attachToTarget.add("degree");
        attachToTarget.add("direction");
        attachToTarget.add("scale");
        
        Set<String> attachToSource = new HashSet<>();
        for (int i = 0; i<10; i++) {
            attachToSource.add("ARG"+i);
            attachToSource.add("op"+i);
            attachToSource.add("snt"+i);
        }
        
        
        for (String lbl : attachToSource) {
            if (attachToTarget.contains(lbl)) {
                System.err.println("Warning: "+lbl+" is supposed to be attached to both source and target.");
            }
        }
        
        
        for (Instance instance : corpus) {
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            
            for (SGraph cGraph : getConstantGraphs(graph, attachToSource, attachToTarget)) {
                if (graph2Count.containsKey(cGraph)) {
                    graph2Count.put(cGraph, graph2Count.get(cGraph)+1);
                } else {
                    graph2Count.put(cGraph, 1);
                }
            }
            
            
        }
        
        FileWriter writer = new FileWriter("output/constantCounts.txt");
        
        for (Object2IntMap.Entry<SGraph> entry : graph2Count.object2IntEntrySet().stream().sorted((entry1, entry2) -> entry2.getIntValue()-entry1.getIntValue()).collect(Collectors.toList())) {
            writer.write(entry.getKey().toIsiAmrString()+"  : "+entry.getIntValue()+"\n");
        }
        
    }
    
    private static Iterable<SGraph> getConstantGraphs(SGraph graph, Set<String> attachToSource, Set<String> attachToTarget) {
        List<SGraph> ret = new ArrayList<>();
        for (String nodeName : graph.getAllNodeNames()) {
            GraphNode node = graph.getNode(nodeName);
            
            SGraph newG = new SGraph();
            newG.setEqualsMeansIsomorphy(true);
            
            GraphNode newGNode = newG.addNode(nodeName, node.getLabel());
            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
                if (attachToSource.contains(e.getLabel())) {
                    GraphNode targetNode = newG.addNode(e.getTarget().getName(), null);
                    newG.addEdge(newGNode, targetNode, e.getLabel());
                } else {
                    if (!attachToTarget.contains(e.getLabel())) {
                        SGraph localEdgeGraph = new SGraph();
                        localEdgeGraph.setEqualsMeansIsomorphy(true);
                        GraphNode src = localEdgeGraph.addNode("src", null);
                        GraphNode tgt = localEdgeGraph.addNode("tgt", null);
                        localEdgeGraph.addEdge(src, tgt, e.getLabel());
                        ret.add(localEdgeGraph);
                    }
                }
            }
            
            for (GraphEdge e : graph.getGraph().incomingEdgesOf(node)) {
                if (attachToTarget.contains(e.getLabel())) {
                    GraphNode sourceNode = newG.addNode(e.getSource().getName(), null);
                    newG.addEdge(sourceNode, newGNode, e.getLabel());
                }
            }
            
            
            ret.add(newG);
        }
        return ret;
    }
    
}
