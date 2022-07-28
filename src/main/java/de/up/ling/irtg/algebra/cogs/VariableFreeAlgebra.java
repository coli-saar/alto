package de.up.ling.irtg.algebra.cogs;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;

import java.util.ArrayList;
import java.util.List;

public class VariableFreeAlgebra extends Algebra<FeatureTree> {
    @Override
    protected FeatureTree evaluate(String label, List<FeatureTree> childrenValues) {
        if( childrenValues.isEmpty() ) {
            // constants
            return new FeatureTree(label, new ArrayList<>());
        } else {
            // binary operations: add edge to feature tree
            assert childrenValues.size() == 2;
            FeatureTree ret = childrenValues.get(0).deepCopy();
            ret.getChildren().add(new Pair(label, childrenValues.get(1)));
            return ret;
        }
    }

    @Override
    public FeatureTree parseString(String representation) throws ParserException {
        return FeatureTree.parse(representation);
    }
}
