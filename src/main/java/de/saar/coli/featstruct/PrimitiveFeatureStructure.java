/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.up.ling.irtg.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A feature structure representing a primitive value,
 * such as "sg" or 3.
 * 
 * @author koller
 */
public class PrimitiveFeatureStructure<E> extends FeatureStructure {
    private E value;

    public PrimitiveFeatureStructure(E value) {
        this.value = value;
    }

    @Override
    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        this.value = value;
    }
    
    
    private static final List<String> EMPTY_PATH = new ArrayList<>();

    @Override
    public List<List<String>> getAllPaths() {
        return Arrays.asList(EMPTY_PATH);
    }

    @Override
    protected FeatureStructure get(List<String> path, int pos) {
        if (pos == path.size()) {
            return this;
        } else {
            return null;
        }
    }
    
    
    
    @Override
    protected int calculateHashCode() {
        return 19 * value.hashCode();
    }

    
    
    /***************************************************************************
     * Tomabechi unification
     **************************************************************************/
    
    @Override
    protected FeatureStructure makeCopyWithCompArcs(long currentTimestamp) {
        FeatureStructure ret = new PrimitiveFeatureStructure(value);
        setCopy(ret, currentTimestamp);
        return ret;
    }
    
    
    
    /***************************************************************************
     * Subsumption checking
     **************************************************************************/
    
    @Override
    protected int checkSubsumptionValues(FeatureStructure other, long timestamp, int resultSoFar) {
        if( value.equals(((PrimitiveFeatureStructure) other).value)) {
            return resultSoFar;
        } else {
            return 0;
        }
    }
    
    
    /***************************************************************************
     * Printing
     **************************************************************************/

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        buf.append(value.toString());
    }
    

    @Override
    protected void appendRawToString(StringBuilder buf, int indent) {
        String prefix = Util.repeat(" ", indent);
        int id = findPreviousId();

        if (id > -1) {
            buf.append(String.format("%sprimitive #%d\n", prefix, id));
        } else {
            id = makeId();
            buf.append(String.format("%sprimitive #%d, value=%s\n", prefix, id, n(value)));            
            appendForwardAndCopy(buf, indent);
        }
    }

    
}
