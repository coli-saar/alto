/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import org.jgraph.JGraph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 *
 * @author koller
 */
public class LambdaGraph {
    private DirectedGraph<GraphNode, GraphEdge> graph;
    private Map<String,GraphNode> nameToNode;
    private List<GraphNode> variables;
    private int nextGensym = 1;

    public LambdaGraph() {
        graph = new DefaultDirectedGraph<GraphNode, GraphEdge>(new GraphEdgeFactory());
        nameToNode = new HashMap<String, GraphNode>();
        variables = new ArrayList<GraphNode>();
    }
    
    public GraphNode addNode(String name, String label) {
        GraphNode u = new GraphNode(name, label);
        graph.addVertex(u);
        nameToNode.put(name, u);
        return u;
    }
    
    public GraphNode addAnonymousNode(String label) {
        GraphNode u = new GraphNode(gensym(), label);
        graph.addVertex(u);
        return u;
    }
    
    public GraphEdge addEdge(GraphNode src, GraphNode tgt, String label) {
        GraphEdge e = graph.addEdge(src, tgt);
        e.setLabel(label);
        return e;
    }
    
    public void addVariable(GraphNode node) {
        variables.add(node);
    }
    
    public GraphNode getNode(String name) {
        return nameToNode.get(name);
    }
    
    public boolean containsNode(String name) {
        return nameToNode.containsKey(name);
    }

    public DirectedGraph<GraphNode, GraphEdge> getGraph() {
        return graph;
    }

    public List<GraphNode> getVariables() {
        return variables;
    }
    
    

    @Override
    public String toString() {
        String varpart = Iterables.toString(Iterables.transform(variables, new Function<GraphNode, String>() {
            public String apply(GraphNode f) {
                return f.getName();
            }            
        }));
        
        return varpart + " -> " + graph;
    }
    
    private String gensym() {
        return "_u" + (nextGensym++);
    }
    
    
    public JComponent makeComponent() {
        JGraphModelAdapter<GraphNode, GraphEdge> adapter = new JGraphModelAdapter<GraphNode, GraphEdge>(graph);
        JGraph jgraph = new JGraph(adapter);

        JGraphFacade facade = new JGraphFacade(jgraph);
        JGraphLayout layout = new JGraphHierarchicalLayout();
        layout.run(facade);

        final Map nestedMap = facade.createNestedMap(true, true);
        jgraph.getGraphLayoutCache().edit(nestedMap);
        
        return jgraph;
    }
    
}
