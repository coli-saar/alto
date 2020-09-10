package de.up.ling.irtg.script;

import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeBottomUpVisitor;
import edu.stanford.nlp.util.Sets;

import java.io.*;
import java.util.*;

public class AMRReentrancyVisualization {

    public static void main(String[] args) throws IOException, InterruptedException {
        IsiAmrInputCodec codec = new IsiAmrInputCodec();
        File dir = new File(args[0]);
        File[] directoryListing = dir.listFiles();
        List<String> sentences = new ArrayList<>();
        List<SGraph> graphs = new ArrayList<>();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (file.getName().endsWith(".txt")) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    boolean isReadingGraph = false;
                    StringBuilder graphBuilder = new StringBuilder();
                    while (br.ready()) {
                        String line = br.readLine();
                        if (!isReadingGraph) {
                            if (line.startsWith("# ::snt")) {
                                sentences.add(line.substring("# ::snt".length()).trim());
                            } else if (line.startsWith("(")) {
                                isReadingGraph = true;
                                graphBuilder.append(line.trim());
                            }
                        } else {
                            if (line.trim().length() == 0) {
                                isReadingGraph = false;
                                try {
                                    graphs.add(codec.read(graphBuilder.toString()));
                                } catch (Exception | Error ex) {
                                    sentences.remove(sentences.size()-1);
                                    System.err.println(graphBuilder.toString());
                                }
                                graphBuilder = new StringBuilder();
                            } else {
                                graphBuilder.append(" "+line.trim());
                            }
                        }
                    }
                    if (isReadingGraph) {
                        graphs.add(codec.read(graphBuilder.toString()));
                    }
                }
                System.out.println("sentences found: "+sentences.size());
                System.out.println("graphs found: "+graphs.size());
            }

            for (int i = 0; i<graphs.size(); i++) {//
                SGraph graph = graphs.get(i);
                String sent = sentences.get(i);
                System.out.println(sent);
                System.out.println(graph.toIsiAmrStringWithSources());
                Tree<GraphNode> tree = buildTree(graph, graph.getGraph().vertexSet().iterator().next(), null,
                        new HashSet<>(), new HashSet<>());
                System.out.println(tree);
                Map<GraphNode, GraphNode> node2lowestCommonAncestor = new HashMap<>();
                tree.dfs(new TreeBottomUpVisitor<GraphNode, Set<GraphNode>>() {
                    @Override
                    public Set<GraphNode> combine(Tree<GraphNode> node, List<Set<GraphNode>> childrenValues) {
                        Set<GraphNode> ret = Collections.singleton(node.getLabel());
                        for (int i = 0; i<childrenValues.size(); i++) {
                            Set<GraphNode> set = childrenValues.get(i);
                            ret = Sets.union(ret, set);
                            for (int j = 0; j<i; j++) {
                                Set<GraphNode> secondSet = childrenValues.get(j);
                                Set<GraphNode> intersection = Sets.intersection(set, secondSet);
                                for (GraphNode commonChild : intersection) {
                                    node2lowestCommonAncestor.put(commonChild, node.getLabel());
                                }
                            }
                        }
                        return ret;
                    }
                });
                System.out.println(node2lowestCommonAncestor);

                Set<GraphNode> loopyNodes = new HashSet<>();
                loopyNodes.addAll(node2lowestCommonAncestor.keySet());
                Set<GraphEdge> loopyEdges = new HashSet<>();
                tree.dfs(new TreeBottomUpVisitor<GraphNode, Set<GraphNode>>() {
                    @Override
                    public Set<GraphNode> combine(Tree<GraphNode> node, List<Set<GraphNode>> childrenValues) {
                        Set<GraphNode> ret = Collections.singleton(node.getLabel());
                        for (int i = 0; i<childrenValues.size(); i++) {
                            Set<GraphNode> set = childrenValues.get(i);
                            ret = Sets.union(ret, set);
                            for (GraphNode nodeBelow : set) {
                                if (node2lowestCommonAncestor.containsKey(nodeBelow)
                                && !set.contains(node2lowestCommonAncestor.get(nodeBelow))) {
                                    loopyNodes.add(node.getLabel());
                                }
                            }
                        }
                        return ret;
                    }
                });
                System.out.println(loopyNodes);
                if (!loopyNodes.isEmpty()) {
                    StringJoiner sj = new StringJoiner("\n");
                    sj.add("digraph G {");

                    //nodes
                    for (GraphNode node : graph.getGraph().vertexSet()) {
                        String s = node.getName();
                        s += " [label=\""+node.getLabel()+"\"";
                        if (loopyNodes.contains(node)) {
                            s += ", style=bold";
                        }
                        s+="];";
                        sj.add(s);
                    }

                    //edges
                    for (GraphEdge edge: graph.getGraph().edgeSet()) {
                        String s = edge.getSource().getName() +"->" + edge.getTarget().getName();
                        s += " [label=\""+edge.getLabel()+"\"";
                        if (loopyNodes.contains(edge.getSource()) && loopyNodes.contains(edge.getTarget())) {
                            s += ", style=bold";
                        }
                        s+="];";
                        sj.add(s);
                    }

                    //sentence
                    sj.add("{rank=sink;");
                    sj.add("sentence [label=\""+sent+"\", shape=box];");
                    sj.add("}");

                    sj.add("}");
                    FileWriter w = new FileWriter(Integer.toString(i)+".txt");
                    w.write(sj.toString());
                    w.close();
                    Runtime rt = Runtime.getRuntime();
                    Process pr = rt.exec("dot -T pdf -o "+i+".pdf "+i+".txt");
                    pr.waitFor();
                }

                System.out.println();
            }



        } else {
            System.err.println(args[0]+" appears not to be a directory! Cancelling.");
        }

    }

    private static Tree<GraphNode> buildTree(SGraph graph, GraphNode current, GraphNode parent, Set<GraphNode> seen,
                                             Set<GraphNode> done) {
        List<Tree<GraphNode>> children = new ArrayList<>();
        seen.add(current);
        for (GraphEdge edge : graph.getGraph().edgesOf(current)) {
            GraphNode other = BlobUtils.otherNode(current, edge);
            if (!seen.contains(other)) {
                children.add(buildTree(graph, other, current, seen, done));
            } else {
                if (done.contains(other) && !other.equals(parent)) {//TODO I think the first check subsumes the second
                    children.add(Tree.create(other));
                }
            }
        }
        done.add(current);
        return Tree.create(current, children);
    }

}
