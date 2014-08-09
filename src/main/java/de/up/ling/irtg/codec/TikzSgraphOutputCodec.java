/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.saar.basic.StringTools;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "tikz-sgraph", description = "tikz-sgraph", type = SGraph.class)
public class TikzSgraphOutputCodec extends OutputCodec<SGraph> {

    @Override
    public void write(SGraph sgraph, OutputStream ostream) throws IOException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.println("\\begin{tikzpicture}");
        w.println(" \\begin{scope} [layered layout, level sep=1.5cm, sibling sep=1cm, rounded corners]");
        w.println(" \\graph {");
        
        for( GraphNode u : sgraph.getGraph().vertexSet() ) {
            //  u1/{want foo} [ssrc];
            w.print("   ");
            writeNodeRepr(u, sgraph, w);
            w.println(";");
        }
        
        w.println();

        for (GraphNode u : sgraph.getGraph().vertexSet()) {
            for (GraphEdge e : sgraph.getGraph().outgoingEdgesOf(u)) {
                //    u3     ->[sedge, "ARG0" sedgel]       { u2 };
                w.print("   " + cleanup(u.getName()) + " ");
                writeEdgeRepr(e, sgraph, w);
                w.println(" { " + cleanup(e.getTarget().getName()) + " };");
            }
        }

        w.println("  };");
        w.println(" \\end{scope}");
        
        w.println();
        
        for( String u : sgraph.getAllNodeNames() ) {
            if( sgraph.isSourceNode(u) ) {
                // \node[sanno,above right=of u1] {\src{root}};
                String srcString = StringTools.join(sgraph.getSourcesAtNode(u), ",");
                w.println(" \\node[sanno, above right=of " + cleanup(u) + "] {\\src{" + srcString + "}};");
            }
        }

        w.println("\\end{tikzpicture}");
        w.flush();
    }

    private void writeNodeRepr(GraphNode u, SGraph sgraph, PrintWriter w) {
        w.print(cleanup(u.getName()) + "/{" + u.getLabel() + "} ");
        if( sgraph.isSourceNode(u.getName()) ) {
            w.print("[ssrc]");
        } else {
            w.print("[snode]");
        }
    }
    
    private void writeEdgeRepr(GraphEdge e, SGraph sgraph, PrintWriter w) {
        w.print("->[sedge, \"" + e.getLabel() + "\" sedgel]");
    }
    
    private String cleanup(String s) {
        return s.replaceAll("_", "").replaceAll("-", "");
    }
}
