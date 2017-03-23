/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import javax.swing.JComponent;

/**
 * A {@link BinarizingAlgebra} whose underlying algebra is a {@link TagTreeAlgebra}.
 * 
 * This will behave exactly as a BinarizingAlgebra with the underlying algebra being
 * a TagTreeAlgebra.
 * 
 * @author koller
 */
public class BinarizingTagTreeAlgebra extends BinarizingAlgebra<Tree<String>> {
    
    /**
     * Creates a new instance with a new signature. 
     */
    public BinarizingTagTreeAlgebra() {
        super(new TagTreeAlgebra());
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }

    
}
