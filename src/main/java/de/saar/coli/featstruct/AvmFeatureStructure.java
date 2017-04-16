/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.up.ling.irtg.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A feature structure representing an attribute-value matrix.
 * Each value is another feature structure.
 * 
 * @author koller
 */
public class AvmFeatureStructure extends FeatureStructure {
    private final Map<String, FeatureStructure> avm = new HashMap<>();

    /**
     * Adds an attribute-value pair to this AVM.
     * 
     * @param attribute
     * @param value 
     */
    public void put(String attribute, FeatureStructure value) {
        avm.put(attribute, value);
    }

    /**
     * Returns the set of attributes defined in this AVM.
     * @return 
     */
    public Set<String> getAttributes() {
        return avm.keySet();
    }

    /**
     * Returns the value stored under the given attribute.
     * @param attribute
     * @return 
     */
    public FeatureStructure get(String attribute) {
        return avm.get(attribute);
    }
    
    @Override
    public List<List<String>> getAllPaths() {
        List<List<String>> ret = new ArrayList<>();

        for (Map.Entry<String, FeatureStructure> arc : avm.entrySet()) {
            List<List<String>> chPaths = arc.getValue().getAllPaths();
            for (List<String> chPath : chPaths) {
                List<String> extended = new ArrayList<>();
                extended.add(arc.getKey());
                extended.addAll(chPath);
                ret.add(extended);
            }
        }

        return ret;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    protected FeatureStructure get(List<String> path, int pos) {
        if (pos < path.size()) {
            FeatureStructure ch = get(path.get(pos));

            if (ch == null) {
                return null;
            } else {
                return ch.get(path, pos + 1);
            }
        } else {
            return null;
        }
    }
    
    
    
    
    
    /***************************************************************************
     * Tomabechi unification
     **************************************************************************/
    
    private List<Map.Entry<String, FeatureStructure>> compArcList; // for Tomabechi
    private long compArcListTimestamp;                             // for Tomabechi; called comp-arc-mark there

    boolean unify1AvmD(AvmFeatureStructure adg2, long currentTimestamp) {
        // split arcs of this into complement and intersect arcs
        List<Map.Entry<String, FeatureStructure>> complementArcs = new ArrayList<>();  // arcs in d2 that are not in d1
        List<Map.Entry<String, FeatureStructure>> intersectArcs = new ArrayList<>();   // arcs that are in both d1 and d2

        for (Map.Entry<String, FeatureStructure> arc : adg2.avm.entrySet()) {
            if (avm.containsKey(arc.getKey())) {
                intersectArcs.add(arc);
            } else {
                complementArcs.add(arc);
            }
        }

        // unify shared arcs
        for (Map.Entry<String, FeatureStructure> arc : intersectArcs) {
            FeatureStructure dest1 = avm.get(arc.getKey());
            FeatureStructure dest2 = adg2.avm.get(arc.getKey());

            boolean success = dest1.unify1(dest2, currentTimestamp);

            if (!success) {
                return false;
            }
        }

        // set forwarding pointer
        adg2.setForward(this, currentTimestamp);

        compArcListTimestamp = currentTimestamp;
        compArcList = complementArcs;

        return true;
    }

    @Override
    protected FeatureStructure makeCopyWithCompArcs(long currentTimestamp) {
        AvmFeatureStructure ret = new AvmFeatureStructure();

        // copy permanent arcs
        for (Map.Entry<String, FeatureStructure> arc : avm.entrySet()) {
            FeatureStructure arcCopy = arc.getValue().copyWithCompArcs(currentTimestamp);
            ret.put(arc.getKey(), arcCopy);
        }

        // copy temporary arcs
        if (compArcList != null && compArcListTimestamp == currentTimestamp) {
            for (Map.Entry<String, FeatureStructure> arc : compArcList) {
                FeatureStructure arcCopy = arc.getValue().copyWithCompArcs(currentTimestamp);
                ret.put(arc.getKey(), arcCopy);
            }

            // will never be looked at again, can release memory
            compArcList = null;
        }

        // set copy pointer
        setCopy(ret, currentTimestamp);

        return ret;
    }

    
    
    
    
    /***************************************************************************
     * Equality checking
     **************************************************************************/
    
    @Override
    protected boolean localEquals(FeatureStructure other) {
        return true;
    }
    
    
    
    
    /***************************************************************************
     * Printing
     **************************************************************************/

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

            avm.get(key).appendWithIndex(visitedIndexedFs, buf);
        }

        buf.append("]");
    }
    
    @Override
    protected void appendRawToString(StringBuilder buf, int indent) {
        String prefix = Util.repeat(" ", indent);
        int id = findPreviousId();

        if (id > -1) {
            buf.append(String.format("%savm #%d\n", prefix, id));
        } else {
            id = makeId();
            buf.append(String.format("%savm #%d\n", prefix, id));

            avm.entrySet().forEach(arc -> {
                buf.append(String.format("%s%s:\n", prefix, arc.getKey()));
                arc.getValue().appendRawToString(buf, indent + 2);
            });

            appendForwardAndCopy(buf, indent);

            if (compArcList != null) {
                buf.append(String.format("%stemporary arcs (timestamp %d):\n", prefix, compArcListTimestamp));
                compArcList.forEach(arc -> {
                    buf.append(String.format("%s%s:\n", prefix, arc.getKey()));
                    arc.getValue().appendRawToString(buf, indent + 2);
                });
            }
        }
    }
}
