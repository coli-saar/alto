/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;
import org.jgrapht.experimental.isomorphism.IsomorphismRelation;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphModelAdapter.CellFactory;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 *
 * @author koller
 */
public class LambdaGraph {
    private DirectedGraph<GraphNode, GraphEdge> graph;
    private Map<String, GraphNode> nameToNode;
    private List<GraphNode> variables;
    private Map<String, String> oldToNewName;  // in renamed graph: nodename in original graph -> nodename in new graph
    private static long nextGensym = 1;
    private int cachedHashcode;
    private boolean hasCachedHashcode = false;

    public LambdaGraph() {
        graph = new DefaultDirectedGraph<GraphNode, GraphEdge>(new GraphEdgeFactory());
        nameToNode = new HashMap<String, GraphNode>();
        variables = new ArrayList<GraphNode>();
        oldToNewName = new HashMap<String, String>();
    }

    public GraphNode addNode(String name, String label) {
        GraphNode u = new GraphNode(name, label);
        graph.addVertex(u);
        nameToNode.put(name, u);
        hasCachedHashcode = false;
        return u;
    }

    public GraphNode addAnonymousNode(String label) {
        GraphNode u = new GraphNode(gensym("_u"), label);
        graph.addVertex(u);
        hasCachedHashcode = false;
        return u;
    }

    public GraphEdge addEdge(GraphNode src, GraphNode tgt, String label) {
        GraphEdge e = graph.addEdge(src, tgt);
        e.setLabel(label);
        hasCachedHashcode = false;
        return e;
    }

    public void addVariable(GraphNode node) {
        variables.add(node);
        hasCachedHashcode = false;
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

    private String getNodeNameAfterRenaming(String oldName) {
        String ret = oldToNewName.get(oldName);

        if (ret == null) {
            return oldName;
        } else {
            return ret;
        }
    }

    // g1 + apply(g2, [a,b,c]) = g1.renameNodes().addAll(renameNodes(g2).apply(g1.mapNodeNames(List(a,b,c))))
    public LambdaGraph renameNodes() {
        LambdaGraph ret = new LambdaGraph();
        Map<String, String> oldToNewNames = new HashMap<String, String>();

        for (GraphNode node : graph.vertexSet()) {
            String newName = gensym(node.getName());
            ret.addNode(newName, node.getLabel());
            oldToNewNames.put(node.getName(), newName);
        }

        for (GraphEdge edge : graph.edgeSet()) {
            ret.addEdge(ret.getNode(oldToNewNames.get(edge.getSource().getName())),
                    ret.getNode(oldToNewNames.get(edge.getTarget().getName())),
                    edge.getLabel());
        }

        for (GraphNode v : variables) {
            ret.addVariable(ret.getNode(oldToNewNames.get(v.getName())));
        }

        ret.oldToNewName = oldToNewNames;

        return ret;
    }

    public Function<String, String> renameNodeF() {
        return new Function<String, String>() {
            public String apply(String f) {
                return getNodeNameAfterRenaming(f);
            }
        };
    }

    List<String> mapNodeNames(List<String> nodes) {
        List<String> ret = new ArrayList<String>();

        Iterables.addAll(ret, Iterables.transform(nodes, renameNodeF()));

        return ret;
    }

    /**
     * This modifies both this graph and the other one.
     *
     * @param other
     * @return
     */
    public LambdaGraph merge(LambdaGraph other) {
        for (GraphNode node : other.graph.vertexSet()) {
            if (containsNode(node.getName())) {
                if (node.getLabel() != null) {
                    getNode(node.getName()).setLabel(node.getLabel());
                }
            } else {
                addNode(node.getName(), node.getLabel());
            }
        }

        for (GraphEdge edge : other.graph.edgeSet()) {
            addEdge(getNode(edge.getSource().getName()), getNode(edge.getTarget().getName()), edge.getLabel());
        }

        other.hasCachedHashcode = false;
        hasCachedHashcode = false;

        return this;
    }

    // leaves "this" untouched
    public LambdaGraph apply(List<String> nodeNames) {
        LambdaGraph ret = new LambdaGraph();
        Map<GraphNode, GraphNode> varnodeToNodeCopy = new HashMap<GraphNode, GraphNode>();

        for (int i = 0; i < nodeNames.size(); i++) {
            String nodeName = nodeNames.get(i);
            GraphNode copy = ret.addNode(nodeName, null);
            varnodeToNodeCopy.put(variables.get(i), copy);
        }

        for (GraphNode node : graph.vertexSet()) {
            GraphNode copyForVarnode = varnodeToNodeCopy.get(node);

            if (copyForVarnode != null) {
                // node is variable node
                copyForVarnode.setLabel(node.getLabel());
            } else {
                ret.addNode(node.getName(), node.getLabel());
            }
        }

        for (GraphEdge edge : graph.edgeSet()) {
            ret.addEdge(mapNode(edge.getSource(), varnodeToNodeCopy), mapNode(edge.getTarget(), varnodeToNodeCopy), edge.getLabel());
        }

        return ret;
    }

    private GraphNode mapNode(GraphNode node, Map<GraphNode, GraphNode> varnodeToNodeCopy) {
        GraphNode ret = varnodeToNodeCopy.get(node);

        if (ret != null) {
            return ret;
        } else {
            return getNode(node.getName());
        }
    }

    @Override
    public String toString() {
        String varpart = Iterables.transform(variables, GraphNode.nameF).toString();
        String nodepart = Iterables.transform(graph.vertexSet(), GraphNode.reprF).toString();
        String edgepart = Iterables.transform(graph.edgeSet(), GraphEdge.reprF).toString();

        return varpart + " -> " + nodepart + edgepart;
    }

    private String gensym(String prefix) {
        return prefix + "_" + (nextGensym++);
    }

    private static class MyModelAdapter extends JGraphModelAdapter<GraphNode, GraphEdge> {
        private CellFactory<GraphNode, GraphEdge> cf;

        public MyModelAdapter(Graph<GraphNode, GraphEdge> graph) {
            super(graph);

            cf = new MappingCellFactory<GraphNode, GraphEdge, String, String>(GraphNode.labelF, GraphEdge.labelF);
        }

        @Override
        public AttributeMap getDefaultEdgeAttributes() {
            AttributeMap map = new AttributeMap();

            GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
            GraphConstants.setEndFill(map, true);
            GraphConstants.setEndSize(map, 10);

            GraphConstants.setForeground(map, Color.black);
//            GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
            GraphConstants.setLineColor(map, Color.decode("#7AA1E6"));

            return map;
        }

        @Override
        public AttributeMap getDefaultVertexAttributes() {
            AttributeMap map = new AttributeMap();

            GraphConstants.setBounds(map, new Rectangle2D.Double(50, 50, 90, 30));
            GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black));
            GraphConstants.setForeground(map, Color.black);
//            GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
            GraphConstants.setOpaque(map, true);
            GraphConstants.setBackground(map, Color.white);

            return map;
        }

        @Override
        public CellFactory<GraphNode, GraphEdge> getCellFactory() {
            return cf;
        }
    }

    public static class MappingCellFactory<VV, EE, VU, EU> implements CellFactory<VV, EE>, Serializable {
        private static final long serialVersionUID = 3690194343461861173L;
        private Function<VV, VU> nodeMapper;
        private Function<EE, EU> edgeMapper;

        public MappingCellFactory(Function<VV, VU> nodeMapper, Function<EE, EU> edgeMapper) {
            this.nodeMapper = nodeMapper;
            this.edgeMapper = edgeMapper;
        }

        public DefaultEdge createEdgeCell(EE jGraphTEdge) {
            return new DefaultEdge(edgeMapper.apply(jGraphTEdge));
        }

        public DefaultGraphCell createVertexCell(VV jGraphTVertex) {
            return new DefaultGraphCell(nodeMapper.apply(jGraphTVertex));
        }
    }

    public JComponent makeComponent() {
        JGraphModelAdapter<GraphNode, GraphEdge> adapter = new MyModelAdapter(graph);

        JGraph jgraph = new JGraph(adapter);

        JGraphFacade facade = new JGraphFacade(jgraph);
        JGraphLayout layout = new JGraphHierarchicalLayout();
        layout.run(facade);

        final Map nestedMap = facade.createNestedMap(true, true);
        jgraph.getGraphLayoutCache().edit(nestedMap);

        return jgraph;

//        return JGraphXAdapter.makeComponent(graph, GraphNode.labelF, GraphEdge.labelF);
    }

    public void draw() {
        JComponent jgraph = makeComponent();

        JFrame frame = new JFrame();
        frame.getContentPane().add(jgraph);
        frame.setTitle("LambdaGraph");
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public int hashCode() {
        if (hasCachedHashcode) {
            return cachedHashcode;
        } else {
            cachedHashcode = 17 * variables.size();

            for (GraphEdge edge : graph.edgeSet()) {
                int x = edge.getSource().getLabel() == null ? 29 : 5 * edge.getSource().getLabel().hashCode();
                int y = edge.getLabel() == null ? 31 : 7 * edge.getLabel().hashCode();
                int z = edge.getTarget().getLabel() == null ? 41 : 11 * edge.getTarget().getLabel().hashCode();
                cachedHashcode += x + y + z;  // this needs to be equal for different orders in which the edges are enumerated
            }

            hasCachedHashcode = true;
            return cachedHashcode;
        }
    }

    public boolean isIsomorphic(LambdaGraph other) {
        GraphIsomorphismInspector iso =
                AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
                getGraph(),
                other.getGraph(),
                new GraphNode.NodeLabelEquivalenceComparator(),
                null);

        if (!iso.isIsomorphic()) {
            return false;
        } else {
            while (iso.hasNext()) {
                final IsomorphismRelation<GraphNode, GraphEdge> ir = (IsomorphismRelation<GraphNode, GraphEdge>) iso.next();
                List<GraphNode> rewrittenVariables = new ArrayList<GraphNode>();

                Iterables.addAll(rewrittenVariables, Iterables.transform(variables, new Function<GraphNode, GraphNode>() {
                    public GraphNode apply(GraphNode f) {
                        return ir.getVertexCorrespondence(f, true);
                    }
                }));

                if (rewrittenVariables.equals(other.variables)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final LambdaGraph other = (LambdaGraph) obj;

        return isIsomorphic(other);
    }
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[-a-zA-z0-9]+");

    private static String p(String s) {
        if (TOKEN_PATTERN.matcher(s).matches()) {
            return s;
        } else {
            return "\"" + s + "\"";
        }
    }

    private void toAmrVisit(GraphNode u, Set<GraphNode> visitedNodes, StringBuilder ret) {
        if (visitedNodes.contains(u)) {
            ret.append(u.getName());
        } else {
            boolean nameShown = false;

            visitedNodes.add(u);

            if (!u.getName().startsWith("_")) { // suppress anonymous nodes
                ret.append("(");
                ret.append(p(u.getName()));
                nameShown = true;
            }

            if (u.getLabel() != null) {
                if (nameShown) {
                    ret.append(" / ");
                }
                ret.append(p(u.getLabel()));
            }

            for (GraphEdge e : graph.outgoingEdgesOf(u)) {
                ret.append("  :" + e.getLabel() + " ");
                toAmrVisit(e.getTarget(), visitedNodes, ret);
            }

            if (nameShown) {
                ret.append(")");
            }
        }
    }

    public String toIsiAmrString() {
        final StringBuilder buf = new StringBuilder();
        final Set<GraphNode> visitedNodes = new HashSet<GraphNode>();

        toAmrVisit(graph.vertexSet().iterator().next(), visitedNodes, buf);

        return buf.toString();
    }
}
