/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;

/**
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
}
