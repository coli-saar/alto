/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.SGraph;

/**
 * Encodes an s-graph as an AMR in the style of the ISI AMR-banks,
 * and also prints source names.
 * An example representation looks as follows:<p>
 * 
 * <code>(u_1 / boy  :ARG0-of (u_2&lt;root&gt; / want  :ARG1 (u_3 / go  :ARG0 u_1)))</code>
 * <p>
 * 
 * The codec annotates each source node with its source names. In the example,
 * the node named u_2 is identified as a root-source.<p>
 * 
 * See the documentation of {@link SgraphAmrOutputCodec} for details
 * on this codec.
 * 
 * @author koller
 */
@CodecMetadata(name = "amr-sgraph-src", description = "ISI-style AMR (with sources)", type = SGraph.class)
public class SgraphAmrWithSourcesOutputCodec extends SgraphAmrOutputCodec {

    public SgraphAmrWithSourcesOutputCodec() {
        printSources = true;
    }
    
}
