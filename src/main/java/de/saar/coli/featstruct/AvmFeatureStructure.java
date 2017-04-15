/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class AvmFeatureStructure extends FeatureStructure {
    private Map<String, FeatureStructure> avm;

    public AvmFeatureStructure() {
        avm = new HashMap<>();
    }

    private AvmFeatureStructure adereference() {
        return (AvmFeatureStructure) dereference();
    }

    public void put(String attribute, FeatureStructure value) {
        adereference().avm.put(attribute, value);
    }

    public Set<String> getAttributes() {
        return adereference().avm.keySet();
    }

    public FeatureStructure get(String attribute) {
        return adereference().avm.get(attribute);
    }

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        boolean first = true;

        buf.append("[");

        for (String key : avm.keySet()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }

            buf.append(key);
            buf.append(": ");

            avm.get(key).dereference().appendWithIndex(visitedIndexedFs, buf);
        }

        buf.append("]");
    }


    @Override
    protected FeatureStructure destructiveUnifyLocalD(FeatureStructure d2) {
        // unification with non-AVM FSs
        if( d2 instanceof PlaceholderFeatureStructure ) {
            d2.forward = this;
            return this;
        } else if( d2 instanceof PrimitiveFeatureStructure ) {
            return null;
        }
        
//        System.err.printf("avm unify %s with %s", this, d2);
        
        // split arcs of this into complement and intersect arcs
        AvmFeatureStructure ad2 = (AvmFeatureStructure) d2;
        Set<String> ad2Attr = ad2.getAttributes();
        List<Map.Entry<String, FeatureStructure>> complementArcs = new ArrayList<>();  // arcs in d1 that are not in d2
        List<Map.Entry<String, FeatureStructure>> intersectArcs = new ArrayList<>();   // arcs that are in both d1 and d2

        for( Map.Entry<String,FeatureStructure> arc : avm.entrySet() ) {
            if( ad2Attr.contains(arc.getKey()) ) {
                intersectArcs.add(arc);
            } else {
                complementArcs.add(arc);
            }
        }
        
//        System.err.printf("intersect arcs: %s", intersectArcs);
//        System.err.printf("complement arcs: %s", complementArcs);
        
        // forward this to d2
        forward = d2;
        
        // unify shared arcs into d2
        for(Map.Entry<String,FeatureStructure> arc : intersectArcs ) {
            FeatureStructure d2Child = ad2.get(arc.getKey());
            FeatureStructure result = arc.getValue().destructiveUnify(d2Child);
            
            if( result == null ) {
                return null;
            } else {
                ad2.put(arc.getKey(), result);
            }
        }
        
        // add complement arcs to d2
        for(Map.Entry<String,FeatureStructure> arc : complementArcs ) {
            ad2.put(arc.getKey(), arc.getValue());
        }
        
        // clean up, we will never look at this again
        avm.clear();
        
        return d2;
    }

    @Override
    protected List<List<String>> getAllPathsD() {
        List<List<String>> ret = new ArrayList<>();
        
        for( Map.Entry<String,FeatureStructure> arc : avm.entrySet() ) {
            List<List<String>> chPaths = arc.getValue().dereference().getAllPathsD();
            for( List<String> chPath : chPaths ) {
                List<String> extended = new ArrayList<>();
                extended.add(arc.getKey());
                extended.addAll(chPath);
                ret.add(extended);
            }
        }
        
        return ret;
    }

    @Override
    protected Object getValueD() {
        return null;
    }

    @Override
    protected FeatureStructure getD(List<String> path, int pos) {
        if (pos < path.size()) {
            FeatureStructure ch = get(path.get(pos));

            if (ch == null) {
                return null;
            } else {
                return ch.dereference().getD(path, pos + 1);
            }
        } else {
            return null;
        }
    }

    @Override
    protected boolean localEqualsD(FeatureStructure other) {
        return true;
    }
}
