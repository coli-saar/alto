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
import de.saar.basic.StringTools;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.jgrapht.DirectedGraph;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;
import org.jgrapht.experimental.isomorphism.IsomorphismRelation;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 *
 * @author koller
 */
public class SGraph {

    private DirectedGraph<GraphNode, GraphEdge> graph;
    private Map<String, GraphNode> nameToNode;
    private Map<String, String> sourceToNodename;
    private SetMultimap<String, String> nodenameToSources;
    private ListMultimap<String, String> labelToNodename;
    private static long nextGensym = 1;
    private boolean hasCachedHashcode;
    private int cachedHashcode;

    public SGraph() {
        graph = new DefaultDirectedGraph<GraphNode, GraphEdge>(new GraphEdgeFactory());
        nameToNode = new HashMap<String, GraphNode>();
        sourceToNodename = new HashMap<>();
        nodenameToSources = HashMultimap.create();
        hasCachedHashcode = false;
    }

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

    public void addSource(String sourceName, String nodename) {
        sourceToNodename.put(sourceName, nodename);
        nodenameToSources.put(nodename, sourceName);
        hasCachedHashcode = false;
    }

    public String getNodeForSource(String sourceName) {
        return sourceToNodename.get(sourceName);
    }

    public GraphNode getNode(String name) {
        return nameToNode.get(name);
    }

    public Collection<String> getAllNodeNames() {
        return nameToNode.keySet();
    }

    public boolean containsNode(String name) {
        return nameToNode.containsKey(name);
    }

    public SGraph merge(SGraph other) {
        if (!overlapsOnlyInSources(other)) {
            Logging.get().fine(() -> "merge: graphs are not disjoint: " + this + ", " + other);
            return null;
        }

        // map node names of "other" to node names of "this" with same source
        // (if the same source node exists in both s-graphs)
        Map<String, String> nodeRenaming = new HashMap<>();
        for (String source : other.sourceToNodename.keySet()) {
            if (this.sourceToNodename.containsKey(source)) {
                nodeRenaming.put(other.sourceToNodename.get(source), this.sourceToNodename.get(source));
            }
        }

        SGraph ret = new SGraph();
        copyInto(ret);
        other.copyInto(ret, renamingF(nodeRenaming));

        // if "other" has source names that "this" does not,
        // then quietly forget these
        // --> actually, don't do this
//        ret.sourceToNodename = this.sourceToNodename;
//        ret.nodenameToSources = this.nodenameToSources;

        return ret;
    }

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
        for( String nodename : ret.nodenameToSources.keySet() ) {
            ret.nodenameToSources.get(nodename).retainAll(retainedSources);
        }

        return ret;
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

    private void copyInto(SGraph into, Function<String, String> nodeRenaming) {
        for (String nodename : nameToNode.keySet()) {
            into.addNode(nodeRenaming.apply(nodename), nameToNode.get(nodename).getLabel());
        }

        for (GraphEdge edge : graph.edgeSet()) {
            into.addEdge(into.getNode(nodeRenaming.apply(edge.getSource().getName())),
                    into.getNode(nodeRenaming.apply(edge.getTarget().getName())),
                    edge.getLabel());
        }

        for (String source : sourceToNodename.keySet()) {
            into.addSource(source, nodeRenaming.apply(sourceToNodename.get(source)));
        }
    }

    public DirectedGraph<GraphNode, GraphEdge> getGraph() {
        return graph;
    }

    public String getSourceLabel(String nodename) {
        return "<" + StringTools.join(nodenameToSources.get(nodename), ",") + ">";
    }

    private static String gensym(String prefix) {
        return prefix + "_" + (nextGensym++);
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

        // TODO - make sure all nodes are shown
        toAmrVisit(graph.vertexSet().iterator().next(), visitedNodes, buf);

        return buf.toString();
    }

    private void appendFullRepr(GraphNode node, Set<String> visitedNodes, StringBuilder buf) {
        buf.append(node.getName());

        if (!visitedNodes.contains(node.getName())) {
            if (nodenameToSources.containsKey(node.getName())) {
                buf.append("<" + StringTools.join(nodenameToSources.get(node.getName()), ",") + ">");
            }

            if (node.getLabel() != null) {
                buf.append("/" + node.getLabel());
            }

            visitedNodes.add(node.getName());
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Set<String> visitedNodes = new HashSet<>();

        buf.append("[");
        for (GraphEdge edge : graph.edgeSet()) {
            if (!visitedNodes.isEmpty()) {
                buf.append("; ");
            }

            appendFullRepr(edge.getSource(), visitedNodes, buf);
            buf.append(" -" + edge.getLabel() + "-> ");
            appendFullRepr(edge.getTarget(), visitedNodes, buf);
        }

        for (GraphNode node : graph.vertexSet()) {
            String nodename = node.getName();

            if (!visitedNodes.contains(nodename)) {
                if (!visitedNodes.isEmpty()) {
                    buf.append("; ");
                }

                appendFullRepr(node, visitedNodes, buf);
            }
        }

        buf.append("]");

        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final SGraph other = (SGraph) obj;

        return isIsomorphic(other);
    }

    public boolean isIdentical(SGraph other) {
        if (!nameToNode.keySet().equals(other.nameToNode.keySet())) {
            return false;
        } else if (graph.edgeSet().size() != other.graph.edgeSet().size()) {
            return false;
        } else if (!sourceToNodename.equals(other.sourceToNodename)) {
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

            for (GraphEdge edge : graph.edgeSet()) {
                GraphEdge edgeInOther = other.findEdge(edge, x -> x);
                if (edgeInOther == null) {
                    return false;
                } else if (edge.getLabel() == null && edgeInOther.getLabel() != null) {
                    return false;
                } else if (!edge.getLabel().equals(edgeInOther.getLabel())) {
                    return false;
                }
            }
        }

        return true;
    }

    private GraphEdge findEdge(GraphEdge originalEdge, Function<String, String> nodeRenaming) {
        String remappedSrc = nodeRenaming.apply(originalEdge.getSource().getName());
        String remappedTgt = nodeRenaming.apply(originalEdge.getTarget().getName());
        GraphNode src = getNode(remappedSrc);
        GraphNode tgt = getNode(remappedTgt);

        if (src == null || tgt == null) {
            return null;
        } else {
            return graph.getEdge(src, tgt);
        }
    }

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
                GraphEdge eInSuper = getGraph().getEdge(src, tgt);

                if (eInSuper == null) {
                    return false;
                } else if (e.getLabel() != null && !e.getLabel().equals(eInSuper.getLabel())) {
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

    public List<SGraph> getMatchingSubgraphs(SGraph subgraph) {
        final List<SGraph> ret = new ArrayList<>();
        foreachMatchingSubgraph(subgraph, s -> ret.add(s));
        return ret;
    }

    public void foreachMatchingSubgraph(SGraph subgraph, Consumer<SGraph> fn) {
        Map<String, Set<String>> possibleNodeRenamings = new HashMap<>();

        ensureNodeIndices();

        // initialize node renamings with all nodes that have the same label
        for (String nodename : subgraph.getAllNodeNames()) {
            GraphNode node = subgraph.getNode(nodename);
            String nodelabel = node.getLabel();
            Set<String> possibleRenamingsHere = null;

            if (nodelabel == null) {
                possibleRenamingsHere = Util.mapSet(graph.vertexSet(), x -> x.getName());
            } else {
                List<String> possibleNodenames = labelToNodename.get(nodelabel);
                possibleRenamingsHere = new HashSet<String>(possibleNodenames);
            }

            // filter out node renamings that don't have correct adjacent edge labels
            for (GraphEdge e : subgraph.graph.outgoingEdgesOf(node)) {
                possibleRenamingsHere.removeIf(renamedNodeName
                        -> !getGraph().outgoingEdgesOf(getNode(renamedNodeName))
                        .stream()
                        .anyMatch(re -> re.getLabel().equals(e.getLabel()))
                );
            }

            for (GraphEdge e : subgraph.graph.incomingEdgesOf(node)) {
                possibleRenamingsHere.removeIf(renamedNodeName
                        -> !getGraph().incomingEdgesOf(getNode(renamedNodeName))
                        .stream()
                        .anyMatch(re -> re.getLabel().equals(e.getLabel()))
                );
            }

            possibleNodeRenamings.put(nodename, possibleRenamingsHere);
        }

        // iterate over all combinations
        List<String> nodesInOrder = new ArrayList<>(possibleNodeRenamings.keySet());
        _foreachMatchingSubgraph(nodesInOrder, 0, possibleNodeRenamings, new HashMap<String, String>(), subgraph, fn);
    }

    private void _foreachMatchingSubgraph(List<String> nodes, int pos, Map<String, Set<String>> possibleRenamings, Map<String, String> selectedRenaming, SGraph subgraph, Consumer<SGraph> fn) {
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

    public boolean isIsomorphic(SGraph other) {
        GraphIsomorphismInspector iso
                = AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
                        getGraph(),
                        other.getGraph(),
                        new GraphNode.NodeLabelEquivalenceComparator(),
                        null);

        if (!iso.isIsomorphic()) {
            return false;
        } else {
            while (iso.hasNext()) {
                final IsomorphismRelation<GraphNode, GraphEdge> ir = (IsomorphismRelation<GraphNode, GraphEdge>) iso.next();

                Map<String, String> rewrittenSources = new HashMap<>(sourceToNodename);
                rewrittenSources.replaceAll((k, v) -> ir.getVertexCorrespondence(getNode(v), true).getName());

                if (rewrittenSources.equals(other.sourceToNodename)) {
                    return true;
                }
            }

            return false;
        }
    }

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

            hasCachedHashcode = true;
            return cachedHashcode;
        }
    }
}
