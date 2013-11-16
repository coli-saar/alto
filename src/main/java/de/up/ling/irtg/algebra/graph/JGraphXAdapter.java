/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.base.Function;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;
import java.awt.FontMetrics;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import org.jgrapht.DirectedGraph;

/**
 *
 * @author koller
 */
public class JGraphXAdapter {
    public static <V, E, VU, EU> JComponent makeComponent(DirectedGraph<V, E> graph, Function<V, VU> nodeLabelF, Function<E, EU> edgeLabelF) {
        mxGraph jgraph = new mxGraph();
        Object parent = jgraph.getDefaultParent();
        FontMetrics metrics = mxUtils.getFontMetrics(mxUtils.getFont(jgraph.getStylesheet().getDefaultVertexStyle()));

        jgraph.setCellsResizable(false);
        jgraph.setCellsDeletable(false);
        jgraph.setCellsEditable(false);
        jgraph.setCellsBendable(false);
//        jgraph.setCellsSelectable(false);
        jgraph.setCellsCloneable(false);
        jgraph.setEdgeLabelsMovable(false);
        jgraph.setDropEnabled(false);
        jgraph.setSplitEnabled(false);
        jgraph.setDisconnectOnMove(false);
        jgraph.setAllowDanglingEdges(false);

        Map<V, Object> nodeMap = new HashMap<V, Object>();

        jgraph.getModel().beginUpdate();
        try {
            for (V v : graph.vertexSet()) {
                String label = nodeLabelF.apply(v).toString();
                Object mxv = jgraph.insertVertex(parent, null, label, 20, 20, metrics.stringWidth(label) + 20, metrics.getHeight());
                nodeMap.put(v, mxv);
            }

            for (E e : graph.edgeSet()) {
                String label = edgeLabelF.apply(e).toString();
                jgraph.insertEdge(parent, null, label, nodeMap.get(graph.getEdgeSource(e)), nodeMap.get(graph.getEdgeTarget(e)));
            }
        } finally {
            jgraph.getModel().endUpdate();
        }

//        mxIGraphLayout layout = new mxCompactTreeLayout(jgraph);
        mxHierarchicalLayout layout = new mxHierarchicalLayout(jgraph);
        layout.setMoveParent(true);
        layout.execute(parent);

        mxGraphComponent graphComponent = new mxGraphComponent(jgraph);
        graphComponent.setConnectable(false);
        return graphComponent;
    }
}
