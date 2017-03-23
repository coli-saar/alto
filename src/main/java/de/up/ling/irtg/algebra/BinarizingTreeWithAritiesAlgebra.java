/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import javax.swing.JComponent;

/**
 * A {@link BinarizingAlgebra} that interprets its values
 * over a {@link TreeWithAritiesAlgebra}.
 * 
 * This gives the same result as creating a binarizing algebra with a TreeWithAritiesAlgebra
 * as the underlying algebra.
 * 
 * @author koller
 */
public class BinarizingTreeWithAritiesAlgebra extends BinarizingAlgebra<Tree<String>> {
    
    /**
     * Creates a new instance with the default concatenation symbol _@_ and
     * a new signature.
     */
    public BinarizingTreeWithAritiesAlgebra() {
        super(new TreeWithAritiesAlgebra());
    }
    
    /**
     * Creates a new instance with a use specified concatenation symbol and
     * a new signature.
     * 
     * @param appendSymbol 
     */
    public BinarizingTreeWithAritiesAlgebra(String appendSymbol) {
        super(new TreeWithAritiesAlgebra(), appendSymbol);
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }
    
}
