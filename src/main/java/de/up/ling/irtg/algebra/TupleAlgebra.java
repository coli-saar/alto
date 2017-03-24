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
 * An experimental algebra for tuples of objects.
 * 
 * This algebra is based on an underlying algebra and defines tree types of
 * operations:
 * 
 * - proj_i where I may be any integer greater than or equal to 0; this unary operation
 * takes a tuple and returns a new tuple containing only the ith element in the
 * list.
 * - ** is used to concatenate two tuples into a single tuple so e.g. **([a,b,c],[d,e])
 * will evaluate to [a,b,c,d,e]. This operation is binary.
 * - Based on every operation of the underlying algebra, an operation with the
 * same name and arity is defined. This new operation op in the algebra will take the
 * first elements of all the tuples that are arguments to op and apply the underlying
 * operation to them to create a new element e. The resulting tuple will then be
 * [e].
 * 
 * Expect bugs when using this. The algebra defines a decomposition automaton
 * only via the evaluating decomposition automaton. This means that decomposition
 * may be very inefficient.
 * 
 * @author koller
 */
class TupleAlgebra<E> extends Algebra<List<E>>{
    private final Algebra<E> underlyingAlgebra;
    
    /**
     * The prefix used to identify projection operations.
     */
    public static final String PROJ = "proj_";
    
    /**
     * The name of the tuple concatenation operation.
     */
    public static final String TUP = "**";

    /**
     * Create a new instance with a new signature and based on the given underlying
     * algebra.
     * 
     * @param underlyingAlgebra 
     */
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
