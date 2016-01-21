/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.ordered;

import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author christoph_teichmann
 */
public class OrderedDependencyGraph extends SGraph {
    /**
     * 
     */
    public static Comparator<String> ORDER = (String s1, String s2) -> {
        long i1 = Integer.parseInt(s1);
        long i2 = Integer.parseInt(s2);
        
        return Long.compare(i1, i2);
    };

    @Override
    public GraphNode addNode(String name, String label) {
        for(int i=0;i<name.length();++i) {
            char c = name.charAt(i);
            
            if(!Character.isDigit(c)) {
                throw new NumberFormatException();
            }
        }
        
        return super.addNode(name, label);
    }
    
    @Override
    public OrderedDependencyGraph merge(SGraph other) {
        long name = 0;
        SortedSet<String> nodes = new TreeSet<>(ORDER);
        nodes.addAll(this.getAllNodeNames());
        
        Map<String, String> nodeRenaming = new HashMap<>();
        
        for(String node : nodes) {
            nodeRenaming.put(node, Long.toString(name++));
        }
        
        OrderedDependencyGraph result = new OrderedDependencyGraph();
        
        Map<String,String> otherRenaming = new HashMap<>();        
        Set<String> sourcesHere = this.getAllSources();
        
        other.getAllSources().stream().filter((source) -> 
            (sourcesHere.contains(source))).forEach((source) -> {
                otherRenaming.put(other.getNodeForSource(source),
                nodeRenaming.get(this.getNodeForSource(source)));
        });
        
        nodes.clear();
        nodes.addAll(other.getAllNodeNames());
        
        for(String node : nodes) {
            if(!otherRenaming.containsKey(node)) {
                otherRenaming.put(node, Long.toString(name++));
            }
        }
        
        ((OrderedDependencyGraph) other).copyInto(result, otherRenaming);
        this.copyInto(result, nodeRenaming);
        
        return result;
    }

    @Override
    protected SGraph makeShallowCopy() {
        OrderedDependencyGraph odg = new OrderedDependencyGraph();
        shallowCopyInto(odg);
        
        return odg;
    }

    @Override
    public OrderedDependencyGraph forgetSourcesExcept(Set<String> retainedSources) {
        return (OrderedDependencyGraph) super.forgetSourcesExcept(retainedSources);
    }

    @Override
    public OrderedDependencyGraph swapSources(String sourceName1, String sourceName2) {
        return (OrderedDependencyGraph) super.swapSources(sourceName1, sourceName2);
    }

    @Override
    public OrderedDependencyGraph renameSource(String oldName, String newName) {
        return (OrderedDependencyGraph) super.renameSource(oldName, newName);
    }
}
