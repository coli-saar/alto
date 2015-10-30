/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.util.Evaluator;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import java.util.List;
import javax.swing.JComponent;

/**
 * A {@link BinarizingAlgebra} that interprets values
 * over a {@link TreeAlgebra}.
 * 
 * @author koller
 */
public class BinarizingTreeAlgebra extends BinarizingAlgebra<Tree<String>> {
    public BinarizingTreeAlgebra() {
        super(new TreeAlgebra());
    }
    
    public BinarizingTreeAlgebra(String appendSymbol) {
        super(new TreeAlgebra(), appendSymbol);
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }

    @Override
    public List<Evaluator> getEvaluationMethods() {
        return new TreeAlgebra().getEvaluationMethods(); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
