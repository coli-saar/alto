/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.StringJoiner;

/**
 * Output codec for visualizing s-graphs with the GraphViz tool.
 * @author Jonas
 */
@CodecMetadata(name = "GraphViz-dot", description = "GraphViz-dot", type = SGraph.class)
public class GraphVizDotOutputCodec extends OutputCodec<SGraph> {

    @Override
    public void write(SGraph graph, OutputStream ostream) throws IOException, UnsupportedOperationException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write("digraph G {\n");
        
        for (GraphNode node : graph.getGraph().vertexSet()) {
            String label;
            Collection<String> sources = graph.getSourcesAtNode(node.getName());
            if (sources != null && !sources.isEmpty()) {
                StringJoiner sb = new StringJoiner(", ");
                for (String source : sources) {
                    sb.add(source);
                }
                label = "<"+node.getLabel()+"<BR/><FONT COLOR=\"red\">"+sb.toString()+"</FONT>"+">";
            } else {
                label = "\""+node.getLabel()+"\"";
            }
            w.write("  "+node.getName()+" [label="+label+"];\n");
        }
        
        for (GraphEdge edge : graph.getGraph().edgeSet()) {
            w.write("  "+edge.getSource().getName()+" -> "+edge.getTarget().getName()+" [label=\""+edge.getLabel()+"\"];\n");
        }
        w.write("}");
        w.close();
    }
    
}
