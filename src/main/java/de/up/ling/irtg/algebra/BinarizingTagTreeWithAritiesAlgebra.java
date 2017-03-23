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
 * This class behaves like a BinarizingTagTreeAlgebra, but an underlying TagTreeWithAritiesAlgebra.
 * 
 * Warning: this uses TagTreeWithAritiesAlgebra, which is a bit hacky (see comment there).
 * @author Jonas
 */
public class BinarizingTagTreeWithAritiesAlgebra extends BinarizingAlgebra<Tree<String>> {
    
    /**
     * Creates a new instance with it's own signature.
     * 
     * This will use the default concatenation symbol.
     */
    public BinarizingTagTreeWithAritiesAlgebra() {
        super(new TagTreeWithAritiesAlgebra());
    }
    
    /**
     * This creates a new instance with its own signature and a user specified
     * concatenation symbol.
     * 
     * @param appendSymbol 
     */
    public BinarizingTagTreeWithAritiesAlgebra(String appendSymbol) {
        super(new TagTreeWithAritiesAlgebra(), appendSymbol);
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }
    
}
