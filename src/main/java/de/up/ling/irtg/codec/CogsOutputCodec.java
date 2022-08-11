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
        ft = postprocess(ft);
        w.write(ft.toString(true));
        w.flush();
    }

    private static OrderedFeatureTreeAlgebra.OrderedFeatureTree postprocess(OrderedFeatureTreeAlgebra.OrderedFeatureTree ft) {
        if( ft.getChildren().isEmpty() ) {
            return ft;
        } else if( ! ft.getLabel().equals("nmod") ) {
            return ft;
        } else {
            // Condense "case" feature of "nmod" nodes into the node label.
            // TODO "nmod" is an edge label, not a node label -> fix it
            List<Pair<String, OrderedFeatureTreeAlgebra.OrderedFeatureTree>> children = new ArrayList<>();
            String label = ft.getLabel();
            for( Pair<String, OrderedFeatureTreeAlgebra.OrderedFeatureTree> child : ft.getChildren() ) {
                if( child.left.equals("case") || child.left.equals("pre_case")) {
                    label = label + "." + child.right.getLabel();
                } else {
                    children.add(child);
                }
            }
            return new OrderedFeatureTreeAlgebra.OrderedFeatureTree(label, children);
        }
    }
}
