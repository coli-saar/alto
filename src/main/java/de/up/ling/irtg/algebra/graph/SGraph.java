/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import de.up.ling.irtg.codec.SgraphAmrOutputCodec;
import de.up.ling.irtg.codec.SgraphAmrWithSourcesOutputCodec;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jgrapht.DirectedGraph;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;
import org.jgrapht.experimental.isomorphism.IsomorphismRelation;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * An s-graph, in the sense of <a href="http://www.cambridge.org/fr/academic/subjects/mathematics/logic-categories-and-sets/graph-structure-and-monadic-second-order-logic-language-theoretic-approach?format=HB">
 * Courcelle &amp; Engelfriet 2012</a>.
 * S-graphs are directed, node-labeled and edge-labeled graphs in which
 * nodes can be designated as <i>sources</i>. For instance, an s-graph
 * might have a node that is designated as the "root"-source and another
 * that is designated as the "subject"-source. There can be at most one
 * source node for each source name, but one node can be exported under
 * multiple source names (i.e. the same node may be the "root"-source and
 * the "subject"-source at the same time).<p>
 * 
 * This implementation supports multiple edges between the same nodes, as
 * long as these edges have different labels.
 * 
 * @author koller
 */
public class SGraph {
    private DirectedMultigraph<GraphNode, GraphEdge> graph;
    private Map<String, GraphNode> nameToNode;
    private Map<String, String> sourceToNodename;
    private SetMultimap<String, String> nodenameToSources;
    private ListMultimap<String, String> labelToNodename;
    private static long nextGensym = 1;
    private boolean hasCachedHashcode;
    private int cachedHashcode;
    private boolean equalsMeansIsomorphy;
    private boolean writeAsAMR;
    private Map<String, Set<String>> incomingEdgeLabels;
    private Map<String, Set<String>> outgoingEdgeLabels;
    private Map<String, String[]> incomingEdgeLabelsAsList;
    private Map<String, String[]> outgoingEdgeLabelsAsList;
    private List<String> allNodeNames;

    /**
     * Creates an empty s-graph.
     */
    public SGraph() {
        graph = new DirectedMultigraph<>(new GraphEdgeFactory());
        nameToNode = new HashMap<>();
        sourceToNodename = new HashMap<>();
        nodenameToSources = HashMultimap.create();
        hasCachedHashcode = false;
        equalsMeansIsomorphy = true;
        writeAsAMR = false;
    }
    
    private void invalidate() {
        hasCachedHashcode = false;
        incomingEdgeLabels = null;
        outgoingEdgeLabels = null;
    }

    /**
     * Creates an s-graph with the given underlying graph, and no sources.
     * 
     * @param graph 
     */
    SGraph(DirectedMultigraph<GraphNode, GraphEdge> graph) {
        this.graph = graph;

        sourceToNodename = new HashMap<>();
        nodenameToSources = HashMultimap.create();
        invalidate();
        equalsMeansIsomorphy = true;
        writeAsAMR = false;

        nameToNode = new HashMap<>();
        for (GraphNode u : graph.vertexSet()) {
            nameToNode.put(u.getName(), u);
        }
    }

    /**
     * Adds a node with the given name and label to the s-graph.
     * The label may be null to indicate that the node does not
     * have a label (yet). If a node with the given name already
     * exists in the graph and the label parameter is not null,
     * this method sets the label of that node.
     *
     * 
     * @param name
     * @param label
     * @return the newly created node
     */
    public GraphNode addNode(String name, String label) {
        GraphNode u = nameToNode.get(name);

        if (u != null) {
            if (label != null) {
                nameToNode.get(name).setLabel(label);
            }
        } else {
            u = new GraphNode(name, label);
            graph.addVertex(u);
            nameToNode.put(name, u);
        }

        invalidate();
        return u;
    }
    
    /**
     * safely removes a node. Returns true if successful, false if not
     * (may not remove a node with a source).
     * @param name
     * @return 
     */
    public boolean removeNode(String name) {
        if (!nameToNode.keySet().contains(name) || isSourceNode(name)) {
            return false;
        } else {
            graph.removeVertex(getNode(name));
            nameToNode.remove(name);
            return true;
        }
    }

    /**
     * Adds an "anonymous" node with the given label to the s-graph.
     * Anonymous nodes are assigned ad-hoc node names that start with
     * "_u". It is therefore a good idea to avoid the use of explicitly
     * named nodes whose names start with _u.
     * 
     * @param label
     * @return the newly created node
     */
    public GraphNode addAnonymousNode(String label) {
        String anonymousName = gensym("_u");
        GraphNode u = new GraphNode(anonymousName, label);
        graph.addVertex(u);
        nameToNode.put(anonymousName, u);
        invalidate();
        return u;
    }

    /**
     * Adds an edge with the given label between the two nodes to the s-graph.
     * 
     * @param src
     * @param tgt
     * @param label
     * @return the newly created edge
     */
    public GraphEdge addEdge(GraphNode src, GraphNode tgt, String label) {
        GraphEdge e = new GraphEdge(src, tgt, label);
        boolean success = graph.addEdge(src, tgt, e);

        // TODO: figure out what causes e to be null, and
        // adjust parsing algorithm to make sure this is never attempted
        if (success) {
            e.setLabel(label);
            invalidate();
        } else {
//            System.err.println("addEdge null: " + src.repr() + " -" + label + "-> " + tgt.repr());
//            System.err.println("graph was: " + this);
        }

        return e;
    }

    /**
     * Designates the node with the given name as the source for the
     * given source name.
     * 
     * @param sourceName
     * @param nodename 
     */
    public void addSource(String sourceName, String nodename) {
        sourceToNodename.put(sourceName, nodename);
        nodenameToSources.put(nodename, sourceName);
        invalidate();
    }

    /**
     * Returns the node for the given source name. If there is no
     * source with this name, returns null.
     * 
     * @param sourceName
     * @return 
     */
    public String getNodeForSource(String sourceName) {
        return sourceToNodename.get(sourceName);
    }

    /**
     * Returns the node in this s-graph with the given name. If there
     * is no node with this name, returns null.
     * 
     * @param name
     * @return 
     */
    public GraphNode getNode(String name) {
        return nameToNode.get(name);
    }

    /**
     * Returns the collection of all node names in this s-graph.
     * 
     * @return 
     */
    public Collection<String> getAllNodeNames() {
        return nameToNode.keySet();
    }

    /**
     * Checks if a node with the given name exists in this s-graph.
     * 
     * @param name
     * @return 
     */
    public boolean containsNode(String name) {
        return nameToNode.containsKey(name);
    }

    /**
     * Returns true iff at least one node in the graph has a label.
     * @return 
     */
    public boolean hasLabelledNode() {
        Iterator<GraphNode> it = nameToNode.values().iterator();
        boolean ret = false;
        while (it.hasNext()) {
            if (it.next().getLabel()!=null) {
                ret = true;
            }
        }
        return ret;
    }
    
    /**
     * Merges this s-graph with another s-graph. The merge operation
     * combines two s-graphs into one. The resulting s-graph contains all
     * nodes that either of the two original graphs contained, with the same
     * node labels and edges as in these. For each source "a" that exists
     * in both original s-graphs, the a-source-nodes are fused into a single
     * node in the new s-graph, which has all the adjacent edges of the
     * two original a-sources. This source becomes the a-source of the new
     * s-graph. See Courcelle &amp; Engelfriet for details.<p>
     * 
     * This method returns a new s-graph object; the two original s-graphs
     * are not modified. It renames the nodes of the "other" s-graph to fresh
     * node names to avoid accidental fusing of nodes.
     * 
     * @param other
     * @return a new s-graph representing the merge of the two original s-graphs
     */
    public SGraph merge(SGraph other) {
//        if (!overlapsOnlyInSources(other)) {
//            Logging.get().fine(() -> "merge: graphs are not disjoint: " + this + ", " + other);
//            return null;
//        }

        // map node names of "other" to node names of "this" with same source
        // (if the same source node exists in both s-graphs)
        Set<String> nodesToBeMerged = new HashSet<>();
        Map<String, String> nodeRenaming = new HashMap<>();
        for (String source : other.sourceToNodename.keySet()) {
            if (this.sourceToNodename.containsKey(source)) {
                nodeRenaming.put(other.sourceToNodename.get(source), this.sourceToNodename.get(source));
                nodesToBeMerged.add(other.sourceToNodename.get(source));
            }
        }
        // map node names of "other" that are not to be merged, but share a node name, to a different name.
        for (String nodeName : other.getAllNodeNames()) {
            if (!nodesToBeMerged.contains(nodeName)) {
                String newName = nodeName;
                while (getAllNodeNames().contains(newName)) {
                    newName = gensym("u");
                    nodeRenaming.put(nodeName, newName);
                }
            }
        }

        SGraph ret = new SGraph();
        copyInto(ret);
        boolean ok = other.copyInto(ret, renamingF(nodeRenaming));

        return ok ? ret : null;
    }

    /**
     * Renames a source in this s-graph to another source name. If the original
     * s-graph had an "a"-source called "u", then in the s-graph that results from
     * renaming "a" to "b", "u" is no longer an "a"-source, but a "b"-source. The
     * resulting s-graph is the same as the original in all other ways.<p>
     * 
     * This method returns a new s-graph object; the original s-graph
     * is not modified.
     * 
     * @param oldName
     * @param newName
     * @return 
     */
    public SGraph renameSource(String oldName, String newName) {
        if (!sourceToNodename.containsKey(oldName)) {
            Logging.get().fine(() -> "renameSource(" + oldName + "," + newName + "): old source does not exist in graph " + this);
            return null;
        }

        if (oldName.equals(newName)) {
            return this;
        }

        // make fast, shallow copy of sgraph; this is okay if this sgraph
        // is not modified after making the copy
        SGraph ret = new SGraph();
        shallowCopyInto(ret);

        // rename the source
        ret.sourceToNodename = new HashMap<>();
        ret.sourceToNodename.putAll(sourceToNodename);
        String nodenameForSource = ret.sourceToNodename.remove(oldName);
        ret.sourceToNodename.put(newName, nodenameForSource);

        ret.nodenameToSources = HashMultimap.create(nodenameToSources);
        ret.nodenameToSources.remove(nodenameForSource, oldName);
        ret.nodenameToSources.put(nodenameForSource, newName);

        return ret;
    }
    
    public SGraph swapSources(String sourceName1, String sourceName2) {
        if ((!sourceToNodename.containsKey(sourceName1))||(!sourceToNodename.containsKey(sourceName2))) {
            Logging.get().fine(() -> "swapSources(" + sourceName1 + "," + sourceName2 + "): old source does not exist in graph " + this);
            return null;
        }

        if (sourceName1.equals(sourceName2)) {
            return this;
        }

        // make fast, shallow copy of sgraph; this is okay if this sgraph
        // is not modified after making the copy
        SGraph ret = new SGraph();
        shallowCopyInto(ret);

        // rename the source
        ret.sourceToNodename = new HashMap<>();
        ret.sourceToNodename.putAll(sourceToNodename);
        String nodenameForSource1 = ret.sourceToNodename.remove(sourceName1);
        String nodenameForSource2 = ret.sourceToNodename.remove(sourceName2);
        ret.sourceToNodename.put(sourceName2, nodenameForSource1);
        ret.sourceToNodename.put(sourceName1, nodenameForSource2);

        ret.nodenameToSources = HashMultimap.create(nodenameToSources);
        ret.nodenameToSources.remove(nodenameForSource1, sourceName1);
        ret.nodenameToSources.remove(nodenameForSource2, sourceName2);
        ret.nodenameToSources.put(nodenameForSource1, sourceName2);
        ret.nodenameToSources.put(nodenameForSource2, sourceName1);

        return ret;
    }

    /**
     * Forgets all sources except for the specified set. All "a"-sources
     * for "a" not in the retainedSources will lose their status as "a"-sources.
     * The method returns a new s-graph object; the original s-graph
     * is not modified.
     * 
     * @param retainedSources
     * @return 
     */
    public SGraph forgetSourcesExcept(Set<String> retainedSources) {
        // make fast, shallow copy of sgraph; this is okay if this sgraph
        // is not modified after making the copy
        SGraph ret = new SGraph();
        shallowCopyInto(ret);

        // forget the other sources
        ret.sourceToNodename = new HashMap<>();
        ret.sourceToNodename.putAll(sourceToNodename);
        ret.sourceToNodename.keySet().retainAll(retainedSources);

        ret.nodenameToSources = HashMultimap.create(nodenameToSources);
        for (String nodename : nodenameToSources.keySet()) {
            Set<String> sourcesHere = ret.nodenameToSources.get(nodename);
            sourcesHere.retainAll(retainedSources);
            if (sourcesHere.isEmpty()) {
                ret.nodenameToSources.removeAll(nodename);
            }
        }

        return ret;
    }

    /**
     * Returns the set of all source names in this s-graph (e.g. {"root", "subject"}).
     * 
     * @return 
     */
    public Set<String> getAllSources() {
        return sourceToNodename.keySet();
    }

    /**
     * Returns the set of all node names of this s-graph that 
     * are an "a"-source for any source "a". (E.g. {u17, u28}.)
     * @return 
     */
    public Iterable<String> getAllSourceNodenames() {
        return nodenameToSources.keySet();
    }

    /**
     * Returns the set of all node names of this s-graph that
     * are not sources. This set is the complement of {@link #getAllNonSourceNodenames() }.
     * 
     * @return 
     */
    public Iterable<String> getAllNonSourceNodenames() {
        return Sets.difference(nameToNode.keySet(), nodenameToSources.keySet());
    }

    private static Function<String, String> renamingF(final Map<String, String> renamingMap) {
        return nodename -> {
            String mapped = renamingMap.get(nodename);
            if (mapped == null) {
                return nodename;
            } else {
                return mapped;
            }
        };
    }

    /**
     * Returns a copy in this s-graph in which all node names have been
     * assigned fresh names.
     * 
     * @return 
     */
    public SGraph withFreshNodenames() {
        Map<String, String> renaming = new HashMap<>();

        for (String nodename : nameToNode.keySet()) {
            renaming.put(nodename, gensym("u"));
        }

        SGraph ret = new SGraph();
        copyInto(ret, renamingF(renaming));

        return ret;
    }

    private void shallowCopyInto(SGraph into) {
        into.graph = graph;
        into.nameToNode = nameToNode;
        into.sourceToNodename = sourceToNodename;
        into.nodenameToSources = nodenameToSources;
    }

    private void copyInto(SGraph into) {
        copyInto(into, x -> {
            return x;
        });
    }

    private boolean copyInto(SGraph into, Function<String, String> nodeRenaming) {
        for (String nodename : nameToNode.keySet()) {
            into.addNode(nodeRenaming.apply(nodename), nameToNode.get(nodename).getLabel());
        }

        for (GraphEdge edge : graph.edgeSet()) {
            GraphEdge newEdge = into.addEdge(into.getNode(nodeRenaming.apply(edge.getSource().getName())),
                                             into.getNode(nodeRenaming.apply(edge.getTarget().getName())),
                                             edge.getLabel());

            if (newEdge == null) {
                // e.g. because an edge with the same label already existed between the two nodes -- Edit: this case should no longer happen, since multigraphs are allowed now.
                return false;
            }
        }

        for (String source : sourceToNodename.keySet()) {
            into.addSource(source, nodeRenaming.apply(sourceToNodename.get(source)));
        }

        return true;
    }

    /**
     * Returns the graph underlying this s-graph. This graph contains
     * all the nodes, edges, node labels, and edge labels of the s-graph,
     * but none of the source information. It can be processed further
     * using the <a href="http://jgrapht.org/">JGraphT graph library</a>.
     */
    public DirectedMultigraph<GraphNode, GraphEdge> getGraph() {
        return graph;
    }

    /**
     * Checks if the node with the given name is a source.
     * 
     * @param nodename
     * @return 
     */
    public boolean isSourceNode(String nodename) {
        return nodenameToSources.containsKey(nodename);
    }

    /**
     * Returns a string representation of the source status
     * of the given node. This representation contains all the source names
     * for which this node is a source, separated by commas.
     * 
     * @param nodename
     * @return 
     */
    public String getSourceLabel(String nodename) {
        return "<" + nodenameToSources.get(nodename).stream().collect(Collectors.joining(",")) + ">";
    }
    
    /**
     * Returns a collection containing all the source names
     * for which the given node is a source. The method returns
     * null if the node is not a source.
     * 
     * @param nodename
     * @return 
     */
    public Collection<String> getSourcesAtNode(String nodename) {
        return nodenameToSources.get(nodename);
    }

    private static String gensym(String prefix) {
        return prefix + "_" + (nextGensym++);
    }

//    private static final Pattern TOKEN_PATTERN = Pattern.compile("[-a-zA-z0-9]+");
//
//    private static String p(String s) {
//        if (TOKEN_PATTERN.matcher(s).matches()) {
//            return s;
//        } else {
//            return "\"" + s + "\"";
//        }
//    }
//
//    private void toAmrVisit(GraphNode u, Set<GraphNode> visitedNodes, StringBuilder ret) {
//        if (visitedNodes.contains(u)) {
//            ret.append(u.getName());
//        } else {
//            boolean nameShown = false;
//
//            visitedNodes.add(u);
//
//            if (!u.getName().startsWith("_")) { // suppress anonymous nodes
//                ret.append("(");
//                ret.append(p(u.getName()));
//                nameShown = true;
//            }
//
//            if (u.getLabel() != null) {
//                if (nameShown) {
//                    ret.append(" / ");
//                }
//                ret.append(p(u.getLabel()));
//            }
//
//            for (GraphEdge e : graph.outgoingEdgesOf(u)) {
//                ret.append("  :" + e.getLabel() + " ");
//                toAmrVisit(e.getTarget(), visitedNodes, ret);
//            }
//
//            if (nameShown) {
//                ret.append(")");
//            }
//        }
//    }

    /**
     * Returns a string representation of this s-graph in the format
     * of the ISI AMR-Bank. 
     * 
     * @return 
     */
    public String toIsiAmrString() {
        return new SgraphAmrOutputCodec().asString(this);
    }
    
    /**
     * Returns a string representation of this s-graph in the format
     * of the ISI AMR-Bank. 
     * 
     * @return 
     */
    public String toIsiAmrStringWithSources() {
        return new SgraphAmrWithSourcesOutputCodec().asString(this);
    }

    private void appendFullRepr(GraphNode node, Set<String> visitedNodes, StringBuilder buf) {
        buf.append(node.getName());

        if (!visitedNodes.contains(node.getName())) {
            if (nodenameToSources.containsKey(node.getName())) {
                buf.append("<" + nodenameToSources.get(node.getName()).stream().collect(Collectors.joining(",")) + ">");
            }

            if (node.getLabel() != null) {
                buf.append("/" + node.getLabel());
            }

            visitedNodes.add(node.getName());
        }
    }
    
    
    interface NodeRepresentationAppender {
        void append(GraphNode node, Set<String> visitedNodes, StringBuilder buf);
    }
    
    /**
     * Returns a string representation of the graph.
     * @param graph
     * @param nra
     * @return 
     */
    static String graphToString(DirectedGraph<GraphNode,GraphEdge> graph, NodeRepresentationAppender nra) {
        StringBuilder buf = new StringBuilder();
        Set<String> visitedNodes = new HashSet<>();

        buf.append("[");
        for (GraphEdge edge : graph.edgeSet()) {
            if (!visitedNodes.isEmpty()) {
                buf.append("; ");
            }

            nra.append(edge.getSource(), visitedNodes, buf);
            buf.append(" -" + edge.getLabel() + "-> ");
            nra.append(edge.getTarget(), visitedNodes, buf);
        }

        for (GraphNode node : graph.vertexSet()) {
            String nodename = node.getName();

            if (!visitedNodes.contains(nodename)) {
                if (!visitedNodes.isEmpty()) {
                    buf.append("; ");
                }

                nra.append(node, visitedNodes, buf);
            }
        }

        buf.append("]");

        return buf.toString();
    }
    
    /**
     * Returns a string representation of the graph.
     */
    public static String graphToString(DirectedGraph<GraphNode,GraphEdge> graph) {
        return graphToString(graph, (node, visitedNodes, buf) -> {
            buf.append(node.getName());

            if (!visitedNodes.contains(node.getName())) {
                if (node.getLabel() != null) {
                    buf.append("/" + node.getLabel());
                }

                visitedNodes.add(node.getName());
            }
        });
    }

    /**
     * Returns a string representation of this s-graph.
     */
    @Override
    public String toString() {
        if (writeAsAMR) {
            return toIsiAmrStringWithSources();
        } else {
            return graphToString(graph, this::appendFullRepr);
        }
    }

    /**
     * Controls whether equality on this s-graph should be checked
     * using identity or isomorphy.
     * 
     * @see #equals(java.lang.Object) 
     * @param equalsMeansIsomorphy 
     */
    public void setEqualsMeansIsomorphy(boolean equalsMeansIsomorphy) {
        this.equalsMeansIsomorphy = equalsMeansIsomorphy;
    }
    
    /**
     * Controls whether the toString method uses the general one for graphs,
     * or the specific one for AMRs.
     */
    public void setWriteAsAMR(boolean writeAsAMR) {
        this.writeAsAMR = writeAsAMR;
    }

    /**
     * Checks whether this graph is equal to another. You can control
     * what "equal" means by using {@link #setEqualsMeansIsomorphy(boolean) }.
     * If "equals means isomorphy" is set to false, then equality is checked
     * by verifying that the two s-graphs have the same set of node names,
     * each node has the same label, any two nodes are connected by edges
     * with the same edge label, and all the sources are the same.<p>
     * 
     * If "equals means isomorphy" is set to true (the default), the equality
     * check first looks for an isomorphic mapping between the node names of
     * the two s-graphs, and then checks for node labels, edges, and sources
     * relative to this mapping. In other words, this mode ignores node names.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        final SGraph other = (SGraph) obj;

        if (equalsMeansIsomorphy) {
            return isIsomorphicAlsoEdges(other);
        } else {
            return isIdentical(other);
        }
    }

    /**
     * Checks whether this graph is identical to the other graph.
     * This corresponds to evaluating {@link #equals(java.lang.Object) }
     * in the mode where "equals means isomorphy" is set to false.
     * 
     * @param other
     * @return 
     */
    public boolean isIdentical(SGraph other) {
        if (this == other) {
            return true;
        } else if (!isIdenticalExceptSources(other)) {
            return false;
        } else return sourceToNodename.equals(other.sourceToNodename);
    }

    /**
     * Checks whether this graph is identical to the other graph, ignoring source assignments.
     * @param other
     * @return 
     */
    public boolean isIdenticalExceptSources(SGraph other) {
        if (this == other) {
            return true;
        } else if (!nameToNode.keySet().equals(other.nameToNode.keySet())) {
            return false;
        } else if (graph.edgeSet().size() != other.graph.edgeSet().size()) {
            return false;
        } else {
            for (String nodename : nameToNode.keySet()) {
                GraphNode nodeHere = nameToNode.get(nodename);
                GraphNode nodeOther = other.nameToNode.get(nodename);

                if (nodeOther == null) {
                    return false;
                } else if (nodeHere.getLabel() == null) {
                    if (nodeOther.getLabel() != null) {
                        return false;
                    }
                } else if (!nodeHere.getLabel().equals(nodeOther.getLabel())) {
                    return false;
                }
            }

            return other.graph.edgeSet().equals(graph.edgeSet());
        }

    }

    /**
     * Checks whether two s-graphs have any nodes in common that are not sources.
     * The method returns true if there is such an overlap, and false otherwise.
     * 
     * @param other
     * @return 
     */
    public boolean overlapsOnlyInSources(SGraph other) {
        Sets.SetView<String> sharedNodeNames = Sets.intersection(nameToNode.keySet(), other.nameToNode.keySet());

        for (String u : sharedNodeNames) {
            // u is a node name that exists in both s-graphs
            Set<String> thisSource = nodenameToSources.get(u);
            Set<String> otherSource = nodenameToSources.get(u);

            // in this case, there must be some source s such that
            // u is the s-node in both graphs
            // in both graphs
            if (thisSource == null || otherSource == null || Collections.disjoint(thisSource, otherSource)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether two s-graphs have consistent node names for the sources.
     * The method checks for each source name "a" that exists in both s-graphs
     * whether the "a"-sources in
     * each s-graph have the same node name. It returns true if this is true
     * for all shared source names, and false otherwise.
     * 
     * @param other
     * @return 
     */
    public boolean nodenamesForSourcesAgree(SGraph other) {
        Sets.SetView<String> sharedSources = Sets.intersection(sourceToNodename.keySet(), other.sourceToNodename.keySet());

        for (String source : sharedSources) {
            if (!sourceToNodename.get(source).equals(other.sourceToNodename.get(source))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether the s-graph contains the given graph as a sub-graph.
     * The map "nodeRenaming" is a mapping of node names in the subgraph to
     * node names in the s-graph.
     * 
     * @param subgraph
     * @param nodeRenaming
     * @return 
     */
    public boolean containsAsSubgraph(DirectedGraph<GraphNode, GraphEdge> subgraph, Map<String, String> nodeRenaming) {
        // check that all nodes in subgraph exist in supergraph,
        // and labels are the same
        for (GraphNode u : subgraph.vertexSet()) {
            String remappedNodename = nodeRenaming.get(u.getName());
            GraphNode uInSuper = getNode(remappedNodename);

            if (uInSuper == null) {
                return false;
            } else if (u.getLabel() != null && !u.getLabel().equals(uInSuper.getLabel())) {
                return false;
            }
        }

        // check that all edges in subgraph exist in supergraph,
        // and labels are the same
        for (GraphEdge e : subgraph.edgeSet()) {
            String remappedSrc = nodeRenaming.get(e.getSource().getName());
            String remappedTgt = nodeRenaming.get(e.getTarget().getName());
            GraphNode src = getNode(remappedSrc);
            GraphNode tgt = getNode(remappedTgt);

            if (src == null || tgt == null) {
                return false;
            } else {
                Set<GraphEdge> esInSuper = getGraph().getAllEdges(src, tgt);

                boolean hasMatchingLabel = false;
                for (GraphEdge eInSuper : esInSuper) {
                    if (Objects.equals(eInSuper.getLabel(), e.getLabel())) {
                        hasMatchingLabel = true;
                        break;
                    }
                }
                if (!hasMatchingLabel) {
                    return false;
                }
            }
        }

        return true;
    }

    private void ensureNodeIndices() {
        if (labelToNodename == null) {
            labelToNodename = ArrayListMultimap.create();

            for (GraphNode u : graph.vertexSet()) {
                if (u.getLabel() != null) {
                    labelToNodename.put(u.getLabel(), u.getName());
                }
            }
        }
    }

    /**
     * Returns a list of matching subgraphs.
     * 
     * @see #foreachMatchingSubgraph(de.up.ling.irtg.algebra.graph.SGraph, java.util.function.Consumer) 
     * @param subgraph
     * @return 
     */
    public List<SGraph> getMatchingSubgraphs(SGraph subgraph) {
        final List<SGraph> ret = new ArrayList<>();
        foreachMatchingSubgraph(subgraph, s -> ret.add(s));
        return ret;
    }
    
    private void recomputeAdjacencyCache() {
        if( incomingEdgeLabels == null ) {
            incomingEdgeLabels = new HashMap<>();
            outgoingEdgeLabels = new HashMap<>();
            incomingEdgeLabelsAsList = new HashMap<>();
            outgoingEdgeLabelsAsList = new HashMap<>();
            
            String[] x = new String[0];
            
            for( GraphNode node : graph.vertexSet() ) {
                String nodename = node.getName();
                Set<String> out = Util.mapToSet(getGraph().outgoingEdgesOf(node), e -> e.getLabel());
                outgoingEdgeLabels.put(nodename, out);                
                outgoingEdgeLabelsAsList.put(nodename, out.toArray(x));
                
                Set<String> in = Util.mapToSet(getGraph().incomingEdgesOf(node), e -> e.getLabel());
                incomingEdgeLabels.put(nodename, in);
                
                incomingEdgeLabelsAsList.put(nodename, in.toArray(x));
            }
            
            allNodeNames = Util.mapToList(graph.vertexSet(), u -> u.getName());
        }
    }
    
    

    /**
     * Applies the given function "fn" to all sub-s-graphs of this s-graph
     * that match the given "subgraph". The sub-s-graphs to which "fn"
     * is applied will all be isomorphic to the "subgraph", but with its
     * nodes renamed to node names of this s-graph.
     * 
     * @param subgraph
     * @param fn 
     */
    public void foreachMatchingSubgraph(SGraph subgraph, Consumer<SGraph> fn) {
        Map<String, Collection<String>> possibleNodeRenamings = new HashMap<>();

        ensureNodeIndices();
        recomputeAdjacencyCache();
        
        subgraph.recomputeAdjacencyCache();
        
//        System.err.println("\nmatch complete graph: " + this);
//        System.err.println("   - subgraph: " + subgraph);

        // initialize node renamings with all nodes that have the same label
        for (String nodename : subgraph.getAllNodeNames()) {
            GraphNode node = subgraph.getNode(nodename);
            String nodelabel = node.getLabel();
            
            String[] outgoingEdgeLabelsOfNode = subgraph.outgoingEdgeLabelsAsList.get(nodename);
            String[] incomingEdgeLabelsOfNode = subgraph.incomingEdgeLabelsAsList.get(nodename);
            
            Collection<String> maybePossibleRenamingsHere = (nodelabel == null) ? allNodeNames : labelToNodename.get(nodelabel);
            List<String> actuallyPossibleRenamingsHere = new ArrayList<>();
            
            renamedNodeLoop:
            for( String renamedNode : maybePossibleRenamingsHere ) {                
                Set<String> outgoingEdgeLabelsOfRenamed = outgoingEdgeLabels.get(renamedNode);
                Set<String> incomingEdgeLabelsOfRenamed = incomingEdgeLabels.get(renamedNode);

                // only collect node if its outgoing edges cover all the outgoing edge labels of node to match
                for( int i = 0; i < outgoingEdgeLabelsOfNode.length; i++ ) {
                    if( ! outgoingEdgeLabelsOfRenamed.contains(outgoingEdgeLabelsOfNode[i]) ) {
                        continue renamedNodeLoop;
                    }
                }
                
                // same for incoming edge labels
                for( int i = 0; i < incomingEdgeLabelsOfNode.length; i++ ) {
                    if( ! incomingEdgeLabelsOfRenamed.contains(incomingEdgeLabelsOfNode[i])) {
                        continue renamedNodeLoop;
                    }
                }
                
                actuallyPossibleRenamingsHere.add(renamedNode);
            }
            
            
            
//            System.err.println("- initial guess: " + possibleRenamingsHere);

//            // filter out node renamings that don't have correct adjacent edge labels
//            for (GraphEdge e : subgraph.graph.outgoingEdgesOf(node)) {
//                maybePossibleRenamingsHere.removeIf(renamedNodeName
//                        -> !getGraph().outgoingEdgesOf(getNode(renamedNodeName))
//                        .stream()
//                        .anyMatch(re -> re.getLabel().equals(e.getLabel()))
//                );
//            }
//            
////            System.err.println("- after outgoing edge filter: " + possibleRenamingsHere);
//
//            for (GraphEdge e : subgraph.graph.incomingEdgesOf(node)) {
//                maybePossibleRenamingsHere.removeIf(renamedNodeName -> !getGraph().incomingEdgesOf(getNode(renamedNodeName))
//                        .stream()
//                        .anyMatch(re -> re.getLabel().equals(e.getLabel()))
//                );
//            }
            
//            System.err.println("- after incoming edge filter: " + possibleRenamingsHere);

            possibleNodeRenamings.put(nodename, actuallyPossibleRenamingsHere);
        }

        // iterate over all combinations
        List<String> nodesInOrder = new ArrayList<>(possibleNodeRenamings.keySet());
        _foreachMatchingSubgraph(nodesInOrder, 0, possibleNodeRenamings, new HashMap<>(), subgraph, fn);
    }

    private void _foreachMatchingSubgraph(List<String> nodes, int pos, Map<String, Collection<String>> possibleRenamings, Map<String, String> selectedRenaming, SGraph subgraph, Consumer<SGraph> fn) {
        if (pos == nodes.size()) {
            // check that this is actually a subgraph
            if (containsAsSubgraph(subgraph.getGraph(), selectedRenaming)) {
                // construct renamed subgraph
                SGraph renamed = new SGraph();
                subgraph.copyInto(renamed, selectedRenaming::get);
                fn.accept(renamed);
            }
        } else {
            String nodeHere = nodes.get(pos);
            possibleRenamings.get(nodeHere).forEach(renamingHere -> {
                selectedRenaming.put(nodeHere, renamingHere);
                _foreachMatchingSubgraph(nodes, pos + 1, possibleRenamings, selectedRenaming, subgraph, fn);
            });
        }
    }

    /**
     * Checks whether two s-graphs are isomorphic. This corresponds to
     * evaluating {@link #equals(java.lang.Object) } in the mode where
     * "equals means isomorphy" is true. Does not check edge labels.
     * 
     * @param other
     * @return 
     */
    public boolean isIsomorphic(SGraph other) {
        GraphIsomorphismInspector iso
                = AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
                        getGraphAsSimpleGraph(),
                        other.getGraphAsSimpleGraph(),
                        new GraphNode.NodeLabelEquivalenceComparator(),
                        null);

        if (!iso.isIsomorphic()) {
            return false;
        } else {
            while (iso.hasNext()) {
                final IsomorphismRelation<GraphNode, GraphEdge> ir = (IsomorphismRelation<GraphNode, GraphEdge>) iso.next();
                
                Map<String, String> rewrittenSources = new HashMap<>();                
                for( String source : sourceToNodename.keySet() ) {
                    GraphNode newNode = ir.getVertexCorrespondence(getNode(sourceToNodename.get(source)), true);
                    String newNodename = newNode.getName();
                    rewrittenSources.put(source, newNodename);
                }
                
                if (rewrittenSources.equals(other.sourceToNodename)) {
                    return true;
                }
            }

            return false;
        }
    }
    
    /**
     * Checks whether two s-graphs are isomorphic. This corresponds to
     * evaluating {@link #equals(java.lang.Object) } in the mode where
     * "equals means isomorphy" is true. Also compares whether edges are
     * labelled equally.
     * 
     * @param other
     * @return 
     */
    public boolean isIsomorphicAlsoEdges(SGraph other) {
        GraphIsomorphismInspector iso
                = AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
                        getGraphAsSimpleGraph(),
                        other.getGraphAsSimpleGraph(),
                        new GraphNode.NodeLabelEquivalenceComparator(),
                        new GraphEdge.EdgeLabelEquivalenceComparator());

        if (!iso.isIsomorphic()) {
            return false;
        } else {
            while (iso.hasNext()) {
                final IsomorphismRelation<GraphNode, GraphEdge> ir = (IsomorphismRelation<GraphNode, GraphEdge>) iso.next();
                
                Map<String, String> rewrittenSources = new HashMap<>();                
                for( String source : sourceToNodename.keySet() ) {
                    GraphNode newNode = ir.getVertexCorrespondence(getNode(sourceToNodename.get(source)), true);
                    String newNodename = newNode.getName();
                    rewrittenSources.put(source, newNodename);
                }
                
                if (rewrittenSources.equals(other.sourceToNodename)) {
                    return true;
                }
            }

            return false;
        }
    }
    
    private static final String MULTIEDGE_SEPARATOR = "__MULTIEDGE__";

    private SimpleDirectedGraph<GraphNode, GraphEdge> getGraphAsSimpleGraph() {
        SimpleDirectedGraph<GraphNode, GraphEdge> simpleGraph = new SimpleDirectedGraph<>(GraphEdge.class);
        for (GraphNode node : graph.vertexSet()) {
            simpleGraph.addVertex(node);
        }
        for (GraphNode n1 : graph.vertexSet()) {
            for (GraphNode n2 : graph.vertexSet()) {
                Set<GraphEdge> edges = graph.getAllEdges(n1, n2);
                if (!edges.isEmpty()) {
                    String newLabel = edges.stream().map(edge -> edge.getLabel()).sorted().collect(Collectors.joining(MULTIEDGE_SEPARATOR));
                    simpleGraph.addEdge(n1, n2, new GraphEdge(n1, n2, newLabel));
                }
            }
        }
        return simpleGraph;
    }
    
    
    /**
     * Computes a hash code for the s-graph. This implementation of hashCode
     * adds up generic label, edge, and source information for the nodes in this
     * s-graph, and is therefore consistent with both variants of {@link #equals(java.lang.Object) }
     * in this class. However, it is a very weak implementation in that many
     * s-graphs that are actually not equals will have the same hash code.
     * One should therefore avoid actually using HashMaps or HashSets of s-graphs,
     * because performance will be poor.
     * 
     * @return 
     */
    @Override
    public int hashCode() {
        if (hasCachedHashcode) {
            return cachedHashcode;
        } else {
            // compare only source names, because names of the actual
            // nodes may be different 
            cachedHashcode = 17 * sourceToNodename.keySet().hashCode();

            for (GraphEdge edge : graph.edgeSet()) {
                int x = edge.getSource().getLabel() == null ? 29 : 5 * edge.getSource().getLabel().hashCode();
                int y = edge.getLabel() == null ? 31 : 7 * edge.getLabel().hashCode();
                int z = edge.getTarget().getLabel() == null ? 41 : 11 * edge.getTarget().getLabel().hashCode();
                cachedHashcode += x + y + z;  // this needs to be equal for different orders in which the edges are enumerated
            }

            for (GraphNode node : graph.vertexSet()) {
                cachedHashcode += (node.getLabel() == null) ? 53 : 13 * node.getLabel().hashCode();
            }

            hasCachedHashcode = true;
            return cachedHashcode;
        }
    }

    /**
     * Checks whether two s-graphs have a source name in common.
     * 
     * @param other
     * @return 
     */
    public boolean hasCommonSource(SGraph other) {
        if (getGraph().vertexSet().isEmpty() || other.getGraph().vertexSet().isEmpty()) {
            return true;
        } else {
            boolean commonSources = !Collections.disjoint(sourceToNodename.keySet(), other.sourceToNodename.keySet());
            return commonSources;
        }
    }
    
    @OperationAnnotation(code = "nrNodes")
    public int nodeCount() {
        return getAllNodeNames().size();
    }

}
