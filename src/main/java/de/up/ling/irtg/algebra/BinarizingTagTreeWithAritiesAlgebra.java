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
 * Warning: this uses TagTreeWithAritiesAlgebra, which is a bit hacky (see comment there).
 * @author Jonas
 */
public class BinarizingTagTreeWithAritiesAlgebra extends BinarizingAlgebra<Tree<String>> {
     public BinarizingTagTreeWithAritiesAlgebra() {
        super(new TagTreeWithAritiesAlgebra());
    }
    
    public BinarizingTagTreeWithAritiesAlgebra(String appendSymbol) {
        super(new TagTreeWithAritiesAlgebra(), appendSymbol);
    }
    
    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }
    
}
