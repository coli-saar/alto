/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.SGraph;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "tikz-sgraph", description = "tikz-sgraph", type = SGraph.class)
public class TikzSgraphOutputCodec extends OutputCodec<SGraph> {

    @Override
    public void write(SGraph object, OutputStream ostream) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
