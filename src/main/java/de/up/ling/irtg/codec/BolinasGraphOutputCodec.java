/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.ParseException;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.induction.IrtgInducer;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.FloydWarshallShortestPaths;

/**
 * An output codec for storing an s-graph in a format that
 * the Bolinas tool can read.
 * 
 * @author koller
 */
@CodecMetadata(name = "bolinas-sgraph", description = "bolinas-sgraph", type = SGraph.class)
public class BolinasGraphOutputCodec extends OutputCodec<SGraph> {

    @Override
    public void write(SGraph object, OutputStream ostream) throws IOException {
        DirectedGraph<GraphNode, GraphEdge> graph = object.getGraph();
        Writer w = new BufferedWriter(new OutputStreamWriter(ostream));
        
        List<GraphNode> roots = findRoots(graph);
        
        if( roots.isEmpty() ) {
            // graph has no root from which all other nodes are reachable, can't encode to Bolinas
            w.write("()\n");
        } else {
            GraphNode root = roots.get(0);
            Set<GraphNode> visited = new HashSet<>();
            encode(root, graph, visited, w);
            w.write("\n");
        }
        
        w.flush();
    }

    private List<GraphNode> findRoots(DirectedGraph<GraphNode, GraphEdge> graph) {
        List<GraphNode> ret = new ArrayList<>();
        FloydWarshallShortestPaths<GraphNode, GraphEdge> fwsp = new FloydWarshallShortestPaths<>(graph);

        for (GraphNode u : graph.vertexSet()) {
            boolean isRoot = true;

            for (GraphNode v : graph.vertexSet()) {
                if (fwsp.shortestDistance(u, v) == Double.POSITIVE_INFINITY) {
                    isRoot = false;
                    break;
                }
            }
            
            if( isRoot ) {
                ret.add(u);
            }
        }

        return ret;
    }
    
    
    private void encode(GraphNode node, DirectedGraph<GraphNode, GraphEdge> graph, Set<GraphNode> visited, Writer w) throws IOException {
        if( visited.contains(node)) {
            w.write(node.getName() + ".");
        } else {
            visited.add(node);
            
            w.write("(" + node.getName() + ".");
            
            if( node.getLabel() != null ) {
                w.write(" :" + node.getLabel());
            }
            
            for( GraphEdge e : graph.outgoingEdgesOf(node) ) {
                w.write(" :" + e.getLabel() + " ");
                encode(e.getTarget(), graph, visited, w);
            }
            
            w.write(")");
        }
    }

    /**
     * Reads an AMR-Bank from a file and outputs it in Bolinas format.
     * 
     * @param args
     * @throws ParseException
     * @throws IOException 
     */
    public static void main(String[] args) throws ParseException, IOException {
        Reader corpusReader = new FileReader("corpora-and-grammars/corpora/amr-bank-v1.3.txt");
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        List<IrtgInducer.TrainingInstance> corpus = inducer.getCorpus();
        
        BolinasGraphOutputCodec oc = new BolinasGraphOutputCodec();
        
        OutputStream o = new FileOutputStream("corpora-and-grammars/corpora/bolinas-amr-bank-v1.3.txt");
        
        for( IrtgInducer.TrainingInstance inst : corpus ) {
            oc.write(inst.graph, o);
        }
        
        o.flush();
        o.close();
    }
    
    
        
        
//        String amr = "(p / picture\n"
//                + "  :domain (i / it)\n"
//                + "  :topic (b2 / boa\n"
//                + "           :mod (c2 / constrictor)\n"
//                + "           :ARG0-of (s / swallow-01\n"
//                + "                      :ARG1 (a / animal))))";
//        
//        String amr2 = "(s2 / say-01\n" +
//"      :ARG0 (b2 / book)\n" +
//"      :ARG1 (s / swallow-01\n" +
//"            :ARG0 (b / boa\n" +
//"                  :mod (c / constrictor))\n" +
//"            :ARG1 (p / prey\n" +
//"                  :mod (w / whole)\n" +
//"                  :poss b)\n" +
//"            :manner (c2 / chew-01 :polarity -\n" +
//"                  :ARG0 b\n" +
//"                  :ARG1 p)))";
//        
//        SGraph sgraph = IsiAmrParser.parse(new StringReader(amr2));
//        
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        new BolinasGraphOutputCodec().write(sgraph, bos);
//        
//        bos.flush();
//        System.err.println("result: " + bos.toString());


}
