/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.algebra.EvaluatingAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 *
 * @author koller
 */
public class GraphAlgebra extends EvaluatingAlgebra<LambdaGraph> {
    private Map<String, GraphCombiningOperation> operations;

    public GraphAlgebra() {
        operations = new HashMap<String, GraphCombiningOperation>();
    }

    protected GraphCombiningOperation op(String opDescription, int arity) throws ParseException {
        GraphCombiningOperation ret = operations.get(opDescription);

        if (ret == null) {
            ret = IsiAmrParser.parseOperation(new StringReader(opDescription));
            getSignature().addSymbol(opDescription, arity);
            operations.put(opDescription, ret);
        }

        return ret;
    }

    @Override
    protected LambdaGraph evaluate(String label, List<LambdaGraph> childrenValues) {
        try {
            GraphCombiningOperation op = op(label, childrenValues.size());
            return op.evaluate(childrenValues);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }

    @Override
    protected boolean isValidValue(LambdaGraph value) {
        return true;
    }

    @Override
    public LambdaGraph parseString(String representation) throws ParserException {
        try {
            return IsiAmrParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
    }

    @Override
    public JComponent visualize(LambdaGraph graph) {
        return graph.makeComponent();
    }
}
