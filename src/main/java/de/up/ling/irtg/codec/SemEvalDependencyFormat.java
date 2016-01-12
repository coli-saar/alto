/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.OrderedSGraph;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class SemEvalDependencyFormat extends InputCodec<OrderedSGraph> {
    /**
     * 
     */
    private static final String POSITIVE = "+";
    
    /**
     * 
     */
    private static final String UNSET = "_";
    
    @Override
    public OrderedSGraph read(InputStream is) throws CodecParseException, IOException {
        List<String[]> lines = new ArrayList<>();
        
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            
            while((line = br.readLine()) != null) {
                line = line.trim();
                
                if(line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                
                String[] parts = line.split("\t");
                
                for(int i=0;i<parts.length;++i){
                    parts[i] = parts[i].trim();
                }
                
                lines.add(parts);
            }
        }
        
        Int2IntMap predicateToNode = new Int2IntOpenHashMap();
        int pos = 0;
        int counter = 0;
        OrderedSGraph sgr = new OrderedSGraph();
        GraphNode root = sgr.addNode(Integer.toString(counter++), "");
        
        List<GraphNode> nodes = new ArrayList<>();
        
        for(String[] line : lines) {
            int node = counter++;
            
            GraphNode no = sgr.addNode(Integer.toString(node), line[3]);
            nodes.add(no);
            
            if(line[4].trim().equals(POSITIVE)) {
                sgr.addEdge(root, no, "isAttached");
            }else {
                sgr.addEdge(root, no, "unattached");
            }
            
            if(line[5].trim().equals(POSITIVE)) {
                predicateToNode.put(pos++, node-1);
            }
        }
        
        counter = 1;
        for(String[] line : lines) {
            GraphNode node = nodes.get((counter++) - 1);
            
            for(int i=0;i+7<line.length;++i) {
                String label = line[i+7].trim();
                
                if(!label.equals(UNSET)) {
                    sgr.addEdge(nodes.get(predicateToNode.get(i)), node, label);
                }
            }
        }
        
        return sgr;
    }
}
