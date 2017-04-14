/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.saar.basic.IdentityHashSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public abstract class FeatureStructure {
    protected String index = null;
    
    public static FeatureStructure parse(String s) throws FsParsingException {
        try {
            return new FsParser().parse(new ByteArrayInputStream(s.getBytes()));
        } catch (IOException ex) {
            // this should not happen
            Logger.getLogger(FeatureStructure.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public Object getValue(String... path) {
        return getValue(path, 0);
    }
    
    abstract protected Object getValue(String[] path, int pos);

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    @Override
    public String toString() {
        Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
        StringBuilder buf = new StringBuilder();
        appendWithIndex(visitedIndexedFs, buf);
        return buf.toString();
    }
    
    protected String getIndexMarker() {
        return "#" + getIndex();
    }
    
    protected void appendWithIndex(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        // prepend index if needed
        if( getIndex() != null ) {
            buf.append(getIndexMarker());
        }
        
        if (! visitedIndexedFs.contains(this)) {
            // mark as visited
            if( getIndex() != null ) {
                visitedIndexedFs.add(this);
                buf.append(" ");
            }
            
            // print actual value
            appendValue(visitedIndexedFs, buf);
        }
    }

    protected abstract void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf);
}
