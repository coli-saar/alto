/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import com.google.common.collect.Lists;
import de.up.ling.irtg.util.Util;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * An experimental algebra for tuples of objects. Expect bugs when using this.
 * 
 * @author koller
 */
class TupleAlgebra<E> extends Algebra<List<E>>{
    private final Algebra<E> underlyingAlgebra;
    public static final String PROJ = "proj_";
    public static final String TUP = "**";

    public TupleAlgebra(Algebra<E> underlyingAlgebra) {
        this.underlyingAlgebra = underlyingAlgebra;
    }
    
    @Override
    protected List<E> evaluate(String label, List<List<E>> childrenValues) {
        if( label.startsWith(PROJ) ) {
            int pos = Integer.parseInt(label.substring(PROJ.length())) - 1;
            List<E> firstChild = childrenValues.get(0);
            return Lists.newArrayList(firstChild.get(pos));
        } else if( label.equals(TUP)) {
            List<E> ret = new ArrayList<>();
            ret.addAll(childrenValues.get(0));
            ret.addAll(childrenValues.get(1));
            return ret;
        } else {
            List<E> firstComponents = Util.mapToList(childrenValues, l -> l.get(0));
            E value = underlyingAlgebra.evaluate(label, firstComponents);
            return Lists.newArrayList(value);
        }
    }

    @Override
    public List<E> parseString(String representation) throws ParserException {
        E value = underlyingAlgebra.parseString(representation);
        return Lists.newArrayList(value);
    }

    @Override
    public JComponent visualize(List<E> object) {
        if( object.size() == 1 ) {
            return underlyingAlgebra.visualize(object.get(0));
        } else {
            return super.visualize(object);
        }
    }
    
    
}
