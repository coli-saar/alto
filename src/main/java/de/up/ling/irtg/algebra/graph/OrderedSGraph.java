/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.Logging;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class OrderedSGraph extends SGraph {
    /**
     * 
     */
    public static Comparator<String> comp = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            int n1 = Integer.parseInt(o1);
            int n2 = Integer.parseInt(o2);
            
            return Integer.compare(n1, n2);
        }
    };
    
    @Override
    public SGraph merge(SGraph other) {
        if (!overlapsOnlyInSources(other)) {
            Logging.get().fine(() -> "merge: graphs are not disjoint: " + this + ", " + other);
            return null;
        }
        
        int name = 0;
        SortedSet<String> nodes = new TreeSet<>(comp);
        nodes.addAll(this.getAllNodeNames());
        
        Map<String, String> nodeRenaming = new HashMap<>();
        
        for(String node : nodes) {
            nodeRenaming.put(node, Integer.toString(name++));
        }
        
        SGraph result = new SGraph();
        this.copyInto(result, nodeRenaming);
        
        Map<String,String> otherRenaming = new HashMap<String,String>();        
        Set<String> sourcesHere = this.getAllSources();
        
        for (String source : other.getAllSources()) {
            if (sourcesHere.contains(source)) {
                otherRenaming.put(other.getNodeForSource(source),
                              otherRenaming.get(this.getNodeForSource(source)));
            }
        }
        
        nodes.clear();
        nodes.addAll(other.getAllNodeNames());
        
        for(String node : nodes) {
            if(!otherRenaming.containsKey(node)) {
                otherRenaming.put(node, Integer.toString(name++));
            }
        }
        
        other.copyInto(result, otherRenaming);
        
        return result;
    }
}
