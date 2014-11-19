/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

/**
 * This class translates JGraphT graphs into JComponents containing JGraphX graphs.
 * The class replaces the JgraphX adapter from JGraphT, which is not available in
 * JGraphT 0.8.3 (current version in Maven Central right now). Unfortunately,
 * the JGraphX graphs look and feel very clunky, so I'm reverting to JGraph 5.
 * 
 * @author koller
 */
class JGraphXAdapter {
//    public static <V, E, VU, EU> JComponent makeComponent(DirectedGraph<V, E> graph, Function<V, VU> nodeLabelF, Function<E, EU> edgeLabelF) {
//        mxGraph jgraph = new mxGraph();
//        Object parent = jgraph.getDefaultParent();
//        FontMetrics metrics = mxUtils.getFontMetrics(mxUtils.getFont(jgraph.getStylesheet().getDefaultVertexStyle()));
//
//        jgraph.setCellsResizable(false);
//        jgraph.setCellsDeletable(false);
//        jgraph.setCellsEditable(false);
//        jgraph.setCellsBendable(false);
////        jgraph.setCellsSelectable(false);
//        jgraph.setCellsCloneable(false);
//        jgraph.setEdgeLabelsMovable(false);
//        jgraph.setDropEnabled(false);
//        jgraph.setSplitEnabled(false);
//        jgraph.setDisconnectOnMove(false);
//        jgraph.setAllowDanglingEdges(false);
//
//        Map<V, Object> nodeMap = new HashMap<V, Object>();
//
//        jgraph.getModel().beginUpdate();
//        try {
//            for (V v : graph.vertexSet()) {
//                String label = nodeLabelF.apply(v).toString();
//                Object mxv = jgraph.insertVertex(parent, null, label, 20, 20, metrics.stringWidth(label) + 20, metrics.getHeight());
//                nodeMap.put(v, mxv);
//            }
//
//            for (E e : graph.edgeSet()) {
//                String label = edgeLabelF.apply(e).toString();
//                jgraph.insertEdge(parent, null, label, nodeMap.get(graph.getEdgeSource(e)), nodeMap.get(graph.getEdgeTarget(e)));
//            }
//        } finally {
//            jgraph.getModel().endUpdate();
//        }
//
////        mxIGraphLayout layout = new mxCompactTreeLayout(jgraph);
//        mxHierarchicalLayout layout = new mxHierarchicalLayout(jgraph);
//        layout.setMoveParent(true);
//        layout.execute(parent);
//
//        mxGraphComponent graphComponent = new mxGraphComponent(jgraph);
//        graphComponent.setConnectable(false);
//        return graphComponent;
//    }
}
