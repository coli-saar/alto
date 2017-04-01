/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphModelAdapter;

/**
 * A tool for drawing an s-graph into a Swing component.
 * 
 * @author koller
 */
public class SGraphDrawer {
    // MAYBEFIX - Previous problem: the adapter was meant to be parameterized by functions that
    // map nodes and edges to the strings that are displayed visually.
    // However, the create*Cell methods of the factory were never called,
    // and so the default string representations was used after all.
    // New Problem: Find a more elegant solution
    private static class MyModelAdapter extends JGraphModelAdapter<GraphNode, GraphEdge> {
        private JGraphModelAdapter.CellFactory<GraphNode, GraphEdge> cf;

        public MyModelAdapter(Graph<GraphNode, GraphEdge> graph, Function<GraphNode, String> nodeF, Function<GraphEdge, String> edgeF) {
            super(graph, makeVertexAttr(), makeEdgeAttr(), new MappingCellFactory<>(nodeF, edgeF));

            
        }

        @Override
        public AttributeMap getDefaultEdgeAttributes() {
            return makeEdgeAttr();
        }

        @Override
        public AttributeMap getDefaultVertexAttributes() {
            return makeVertexAttr();
        }

        @Override
        public JGraphModelAdapter.CellFactory<GraphNode, GraphEdge> getCellFactory() {
            return cf;
        }
        
        private static AttributeMap makeVertexAttr() {
            AttributeMap map = new AttributeMap();

            GraphConstants.setBounds(map, new Rectangle2D.Double(50, 50, 90, 30));
            GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black));
            GraphConstants.setForeground(map, Color.black);
//            GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
            GraphConstants.setOpaque(map, true);
            GraphConstants.setBackground(map, Color.white);

            return map;
        }
        
        private static AttributeMap makeEdgeAttr() {
            AttributeMap map = new AttributeMap();

            GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
            GraphConstants.setEndFill(map, true);
            GraphConstants.setEndSize(map, 10);

            GraphConstants.setForeground(map, Color.black);
//            GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
            GraphConstants.setLineColor(map, Color.decode("#7AA1E6"));

            return map;
        }
    }

    private static class MappingCellFactory<VV, EE, VU, EU> implements JGraphModelAdapter.CellFactory<VV, EE>, Serializable {

        private static final long serialVersionUID = 3690194343461861173L;
        private Function<VV, VU> nodeMapper;
        private Function<EE, EU> edgeMapper;

        public MappingCellFactory(Function<VV, VU> nodeMapper, Function<EE, EU> edgeMapper) {
            this.nodeMapper = nodeMapper;
            this.edgeMapper = edgeMapper;
        }

        @Override
        public DefaultEdge createEdgeCell(EE jGraphTEdge) {
            return new DefaultEdge(edgeMapper.apply(jGraphTEdge));
        }

        @Override
        public DefaultGraphCell createVertexCell(VV jGraphTVertex) {
            return new DefaultGraphCell(nodeMapper.apply(jGraphTVertex));
        }
    }

    /**
     * Returns a Swing component that displays the
     * given s-graph.
     * 
     * @param sgraph
     * @return 
     */
    public static JComponent makeComponent(SGraph sgraph) {
        JGraphModelAdapter<GraphNode, GraphEdge> adapter = 
                new MyModelAdapter(sgraph.getGraph(), 
                                    node -> {
                                        Collection<String> sources = sgraph.getSourcesAtNode(node.getName());
                                        if (!sources.isEmpty()) {
                                            return node.getLabel() + "/" + sgraph.getSourceLabel(node.getName());
//                                            return node.getName()+"/"+node.getLabel() + "/" + sgraph.getSourceLabel(node.getName());
                                        } else {
                                            return node.getLabel();
//                                            return node.getName()+"/"+node.getLabel();
                                        }
                                        
                                                },
                                    edge -> edge.getLabel());

        JGraph jgraph = new JGraph(adapter);
        
        JGraphFacade facade = new JGraphFacade(jgraph);
        JGraphLayout layout = new JGraphHierarchicalLayout();
        layout.run(facade);

        final Map nestedMap = facade.createNestedMap(true, true);
        jgraph.getGraphLayoutCache().edit(nestedMap);
        
        return jgraph;
    }

    /**
     * Opens a new Swing window with the given title
     * and draws the s-graph in it.
     * 
     * @param sgraph
     * @param title 
     */
    public static void draw(SGraph sgraph, String title) {
        JComponent jgraph = makeComponent(sgraph);

        JFrame frame = new JFrame();
        frame.getContentPane().add(jgraph);
        frame.setTitle(title);
        frame.pack();
        frame.setVisible(true);
    }
}
