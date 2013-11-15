/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.Map;
import javax.swing.JFrame;
import org.jgraph.JGraph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

/**
 *
 * @author koller
 */
public class GraphAlgebra extends Algebra<DirectedGraph<String, String>> {

    public static void main(String[] args) {
        DirectedGraph<GraphNode, GraphEdge> graph = new DefaultDirectedGraph<GraphNode, GraphEdge>(new GraphEdgeFactory());

        GraphNode n1 = new GraphNode("x", "xl");
        GraphNode n2 = new GraphNode("y", "ul");
        
        graph.addVertex(n1);
        graph.addVertex(n2);

        GraphEdge e = graph.addEdge(n1, n2);
        e.setLabel("hallo");

        JGraphModelAdapter<GraphNode, GraphEdge> adapter = new JGraphModelAdapter<GraphNode, GraphEdge>(graph);
        JGraph jgraph = new JGraph(adapter);


        JGraphFacade facade = new JGraphFacade(jgraph);
        JGraphLayout layout = new JGraphHierarchicalLayout();
        layout.run(facade);

        final Map nestedMap = facade.createNestedMap(true, true);
        jgraph.getGraphLayoutCache().edit(nestedMap);

        JFrame frame = new JFrame();
        frame.getContentPane().add(jgraph);
        frame.setTitle("JGraphT Adapter to JGraph Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
//        frame.setSize(500, 500);
        frame.setVisible(true);
    }
    
    public static void drawGraph(DirectedGraph<GraphNode, GraphEdge> graph) {
        JGraphModelAdapter<GraphNode, GraphEdge> adapter = new JGraphModelAdapter<GraphNode, GraphEdge>(graph);
        JGraph jgraph = new JGraph(adapter);


        JGraphFacade facade = new JGraphFacade(jgraph);
        JGraphLayout layout = new JGraphHierarchicalLayout();
        layout.run(facade);

        final Map nestedMap = facade.createNestedMap(true, true);
        jgraph.getGraphLayoutCache().edit(nestedMap);

        JFrame frame = new JFrame();
        frame.getContentPane().add(jgraph);
        frame.setTitle("JGraphT Adapter to JGraph Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
//        frame.setSize(500, 500);
        frame.setVisible(true);
    }

    @Override
    public DirectedGraph<String, String> evaluate(Tree<String> t) {
        DirectedGraph<String, String> x = new DirectedMultigraph<String, String>(String.class);

        x.addVertex("x");
        x.addEdge(null, null, null);


        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TreeAutomaton decompose(DirectedGraph<String, String> value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DirectedGraph<String, String> parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Signature getSignature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
