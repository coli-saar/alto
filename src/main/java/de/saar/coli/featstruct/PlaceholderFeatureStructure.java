/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    protected FeatureStructure destructiveUnifyLocalD(FeatureStructure other) {
        forward = other;
        return other;
    }

    private static final List<String> EMPTY_PATH = new ArrayList<>();
    
    @Override
    protected List<List<String>> getAllPathsD() {
        return Arrays.asList(EMPTY_PATH);
    }

    @Override
    protected Object getValueD() {
        return null;
    }

    @Override
    protected FeatureStructure getD(List<String> path, int pos) {
        if (pos == path.size()) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    protected boolean localEqualsD(FeatureStructure other) {
        FeatureStructure d = other.dereference();
        return other instanceof PlaceholderFeatureStructure;
    }
}
