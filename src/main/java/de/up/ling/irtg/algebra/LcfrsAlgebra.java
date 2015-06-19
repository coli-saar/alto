/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import java.util.List;

/**
 * An experimental algebra for LCFRS. Expect bugs when using this.
 * 
 * @author koller
 */
class LcfrsAlgebra extends TupleAlgebra<List<String>> {
    public LcfrsAlgebra() {
        super(new StringAlgebra());
    }
}
