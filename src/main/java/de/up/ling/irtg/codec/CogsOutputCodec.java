package de.up.ling.irtg.codec;

import de.saar.basic.Pair;
import de.saar.coli.algebra.OrderedFeatureTreeAlgebra;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An output codec for printing OrderedFeatureTrees in the variable-free COGS format.
 *
 */
@CodecMetadata(name = "cogs", description = "Encodes an OrderedFeatureTree in the COGS format", type = OrderedFeatureTreeAlgebra.OrderedFeatureTree.class)
public class CogsOutputCodec extends OutputCodec<OrderedFeatureTreeAlgebra.OrderedFeatureTree> {
    @Override
    public void write(OrderedFeatureTreeAlgebra.OrderedFeatureTree ft, OutputStream ostream) throws IOException, UnsupportedOperationException {
        Writer w = new BufferedWriter(new OutputStreamWriter(ostream));
        w.write(ft.toString(true));
        w.flush();
    }
}
