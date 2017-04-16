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
 * An empty feature structure that has no data in it.
 * Placeholder feature structures are useful when multiple
 * placeholders with the same index are used in different places,
 * enforcing reentrancy.
 * 
 * @author koller
 */
public class PlaceholderFeatureStructure extends FeatureStructure {
    public PlaceholderFeatureStructure(String index) {
        setIndex(index);
    }
    
    
    
    private static final List<String> EMPTY_PATH = new ArrayList<>();
    
    @Override
    public List<List<String>> getAllPaths() {
        return Arrays.asList(EMPTY_PATH);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    protected FeatureStructure get(List<String> path, int pos) {
        if (pos == path.size()) {
            return this;
        } else {
            return null;
        }
    }

    
    /***************************************************************************
     * Tomabechi unification
     **************************************************************************/
    
    @Override
    protected FeatureStructure makeCopyWithCompArcs(long currentTimestamp) {
        FeatureStructure ret = new PlaceholderFeatureStructure(getIndex());
        setCopy(ret, currentTimestamp);
        return ret;
    }
    
    
    
    /***************************************************************************
     * Equality checking
     **************************************************************************/
    
    @Override
    protected boolean localEquals(FeatureStructure other) {
        FeatureStructure d = other; //.dereference();
        return other instanceof PlaceholderFeatureStructure;
    }
    
    
    /***************************************************************************
     * Printing
     **************************************************************************/
    

    @Override
    protected void appendWithIndex(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        buf.append(getIndexMarker());
    }

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        buf.append("PH" + getIndexMarker());
    }

    @Override
    protected void appendRawToString(StringBuilder buf, int indent) {
        String prefix = Util.repeat(" ", indent);
        
        int id = findPreviousId();
        if( id > -1 ) {
            buf.append(String.format("%splaceholder #%d\n", prefix, id));
        } else {
            id = makeId();
            buf.append(String.format("%splaceholder #%d, index=%s\n", prefix, id, getIndex()));
            appendForwardAndCopy(buf, indent);
        }
    }
}
