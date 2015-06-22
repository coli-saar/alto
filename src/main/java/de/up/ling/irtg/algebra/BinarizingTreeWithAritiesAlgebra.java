/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import javax.swing.JComponent;

/**
 *
 * @author koller
 */
public class BinarizingTreeWithAritiesAlgebra extends BinarizingAlgebra<Tree<String>> {
     public BinarizingTreeWithAritiesAlgebra() {
        super(new TreeWithAritiesAlgebra());
    }
    
    public BinarizingTreeWithAritiesAlgebra(String appendSymbol) {
        super(new TreeWithAritiesAlgebra(), appendSymbol);
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }
}
