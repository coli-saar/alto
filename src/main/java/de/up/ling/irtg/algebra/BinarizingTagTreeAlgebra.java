/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import de.up.ling.irtg.util.Evaluator;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import java.util.List;
import javax.swing.JComponent;

/**
 * A {@link BinarizingAlgebra} whose underlying algebra is a {@link TagTreeAlgebra}.
 * 
 * @author koller
 */
public class BinarizingTagTreeAlgebra extends BinarizingAlgebra<Tree<String>> {
    public BinarizingTagTreeAlgebra() {
        super(new TagTreeAlgebra());
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }

    @Override
    public List<Evaluator> getEvaluationMethods() {
        return new TagTreeAlgebra().getEvaluationMethods(); //To change body of generated methods, choose Tools | Templates.
    }
    
}
