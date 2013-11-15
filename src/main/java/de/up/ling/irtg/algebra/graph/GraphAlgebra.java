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
import java.io.StringReader;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 *
 * @author koller
 */
public class GraphAlgebra extends Algebra<LambdaGraph> {
    public static void drawGraph(LambdaGraph graph) {
        JComponent jgraph = graph.makeComponent();

        JFrame frame = new JFrame();
        frame.getContentPane().add(jgraph);
        frame.setTitle("JGraphT Adapter to JGraph Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public LambdaGraph evaluate(Tree<String> t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JComponent visualize(LambdaGraph graph) {
        return graph.makeComponent();
    }
}
