/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import java.util.Set;

/**
 *
 * @author koller
 */
public class PlaceholderFeatureStructure extends FeatureStructure {
    public PlaceholderFeatureStructure(String index) {
        setIndex(index);
    }    

    @Override
    protected void appendWithIndex(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        buf.append(getIndexMarker());
    }

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Object getValue(String[] path, int pos) {
        return null;
    }
}
