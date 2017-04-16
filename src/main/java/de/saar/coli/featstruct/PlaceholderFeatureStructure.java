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
 *
 * @author koller
 */
public class PlaceholderFeatureStructure extends FeatureStructure {
    public PlaceholderFeatureStructure(String index) {
        setIndex(index);
    }    

    @Override
    protected void appendWithIndexD(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        buf.append(getIndexMarker());
    }

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*
    @Override
    protected FeatureStructure destructiveUnifyLocalD(FeatureStructure other) {
        setForwardD(other, -1);
        return other;
    }
*/

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
        FeatureStructure d = other; //.dereference();
        return other instanceof PlaceholderFeatureStructure;
    }

    @Override
    protected FeatureStructure copyWithCompArcsD(long currentTimestamp) {
        FeatureStructure ret = new PlaceholderFeatureStructure(getIndexD());
        setCopyD(ret, currentTimestamp);
        return ret;
    }

    @Override
    protected void appendRawToString(StringBuilder buf, int indent) {
        String prefix = Util.repeat(" ", indent);
        
        int id = findPreviousId();
        if( id > -1 ) {
            buf.append(String.format("%splaceholder #%d\n", prefix, id));
        } else {
            id = makeId();
            buf.append(String.format("%splaceholder #%d, index=%s\n", prefix, id, getIndexD()));
            appendForwardAndCopy(buf, indent);
        }
    }
}
