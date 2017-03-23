/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;

/**
 * An experimental algebra for MBOTs.
 * 
 * This algebra represents instances of the TupleAlgebra, which are defined
 * over tuples of trees with string labels. Expect bugs when using this.
 * 
 * @author koller
 */
class RightMbotAlgebra extends TupleAlgebra<Tree<String>> {
    public RightMbotAlgebra() {
        super(new TreeAlgebra());
    }
    
}
