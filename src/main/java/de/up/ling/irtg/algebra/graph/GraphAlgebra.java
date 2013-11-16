/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 *
 * @author koller
 */
public class GraphAlgebra extends Algebra<LambdaGraph> {
    private Map<String,GraphCombiningOperation> operations;
    private Signature signature;

    public GraphAlgebra() {
        operations = new HashMap<String, GraphCombiningOperation>();
        signature = new Signature();
    }
    
    protected GraphCombiningOperation op(String opDescription, int arity) throws ParseException {
        GraphCombiningOperation ret = operations.get(opDescription);
        
        if( ret == null ) {
            ret = IsiAmrParser.parseOperation(new StringReader(opDescription));
            getSignature().addSymbol(opDescription, arity);
            operations.put(opDescription, ret);
        }
        
        return ret;
    }
    
    @Override
    public LambdaGraph evaluate(Tree<String> t) {
        return t.dfs(new TreeVisitor<String, Void, LambdaGraph>() {
            @Override
            public LambdaGraph combine(Tree<String> node, List<LambdaGraph> childrenValues) {
                try {
                    GraphCombiningOperation op = op(node.getLabel(), childrenValues.size());
                    return op.evaluate(childrenValues);
                } catch (ParseException ex) {
                    throw new IllegalArgumentException("Could not parse operation \"" + node.getLabel() + "\": " + ex.getMessage());
                }
            }           
        });
    }

    @Override
    public TreeAutomaton decompose(LambdaGraph value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public Signature getSignature() {
        return signature;
    }

    @Override
    public JComponent visualize(LambdaGraph graph) {
        return graph.makeComponent();
    }
}
