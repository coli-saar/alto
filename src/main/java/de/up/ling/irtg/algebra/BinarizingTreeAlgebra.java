/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import javax.swing.JComponent;

/**
 * A {@link BinarizingAlgebra} that interprets values
 * over a {@link TreeAlgebra}.
 * 
 * This is a BinarizingAlgebra where the underlying algebra is a TreeAlgebra.
 * 
 * @author koller
 */
public class BinarizingTreeAlgebra extends BinarizingAlgebra<Tree<String>> {
    
    /**
     * Creates a new instance with its own signature.
     */
    public BinarizingTreeAlgebra() {
        super(new TreeAlgebra());
    }
    
    /**
     * Creates a new instance with its own signature and a user specified concatenation symbol.
     * @param appendSymbol 
     */
    public BinarizingTreeAlgebra(String appendSymbol) {
        super(new TreeAlgebra(), appendSymbol);
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }

}
