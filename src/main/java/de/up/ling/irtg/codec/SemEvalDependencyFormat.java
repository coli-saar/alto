/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.dependency_graph.DependencyGraph;
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
public class SemEvalDependencyFormat extends InputCodec<DependencyGraph> {
    /**
     * 
     */
    private static final String POSITIVE = "+";
    
    /**
     * 
     */
    private static final String UNSET = "_";
    
    @Override
    public DependencyGraph read(InputStream is) throws CodecParseException, IOException {
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
        DependencyGraph dg = new DependencyGraph();
        
        for(String[] line : lines) {
            int node = counter++;
            
            dg.addNode(line[3]);
            
            if(line[4].trim().equals(POSITIVE)) {
                dg.addRootEdge(counter, "rootEdge");
            }
            
            if(line[5].trim().equals(POSITIVE)) {
                predicateToNode.put(pos++, node);
            }
        }
        
        counter = 0;
        for(String[] line : lines) {
            int node = counter++;
            
            for(int i=0;i+7<line.length;++i) {
                String label = line[i+7].trim();
                
                if(!label.equals(UNSET)) {
                    dg.addEdge(predicateToNode.get(i), node, label);
                }
            }
        }
        
        return dg;
    }
}
