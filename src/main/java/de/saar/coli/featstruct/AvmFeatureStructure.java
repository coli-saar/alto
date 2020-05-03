/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.up.ling.irtg.util.Util;

import java.util.*;
import java.util.function.Consumer;

/**
 * A feature structure representing an attribute-value matrix. Each value is
 * another feature structure.
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
     *
     * @return
     */
    public Set<String> getAttributes() {
        return avm.keySet();
    }

    /**
     * Returns the value stored under the given attribute.
     *
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
            return this;
        }
    }

    @Override
    protected int calculateHashCode() {
        int ret = 17;

        for (Map.Entry<String, FeatureStructure> arc : avm.entrySet()) {
            ret = 19 * ret + 23 * Objects.hashCode(arc.getKey()) + 31 * Objects.hashCode(arc.getValue());
        }

        return ret;
    }

    @Override
    protected void forAllChildren(Consumer<FeatureStructure> fn) {
        avm.values().forEach(fn);
    }

    /**
     * *************************************************************************
     * Tomabechi unification
     * ************************************************************************
     */
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

    /**
     * *************************************************************************
     * Subsumption checking
     * ************************************************************************
     */
    @Override
    protected int checkSubsumptionValues(FeatureStructure other, long timestamp, int resultSoFar) {
        AvmFeatureStructure aOther = (AvmFeatureStructure) other;

        for (Map.Entry<String, FeatureStructure> arc : avm.entrySet()) {
            if (aOther.avm.containsKey(arc.getKey())) { // iteration over intersect arcs
                FeatureStructure childInThis = arc.getValue();
                FeatureStructure childInOther = aOther.avm.get(arc.getKey());
                resultSoFar = childInThis.checkSubsumptionBothWays(childInOther, timestamp, resultSoFar);

                if (resultSoFar == 0) {
                    return 0;
                }
            } else {
                // "this" has attribute that is not present in "other"
                // => "this" does not subsume "other"
                resultSoFar &= ~SUBSUMES_FORWARD;
            }
        }

        if ((resultSoFar | SUBSUMES_BACKWARD) > 0) {
            for (String key : aOther.avm.keySet()) {
                if (!avm.containsKey(key)) {
                    // "other" has attribute that is not present in "this"
                    // => "other" does not subsume "this"
                    resultSoFar &= ~SUBSUMES_BACKWARD;

                    if (resultSoFar == 0) {
                        return 0;
                    }
                }
            }
        }

        return resultSoFar;
    }

    /**
     * *************************************************************************
     * Printing
     * ************************************************************************
     */
    public List<String> getSortedAttributes() {
        List<String> ret = new ArrayList<>(avm.keySet());
        Collections.sort(ret);
        return ret;
    }
    
    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, boolean printedIndexMarker, Map<FeatureStructure, String> reentrantFsToIndex, StringBuilder buf) {
        boolean first = true;
        
        if( printedIndexMarker ) {
            buf.append(" ");
        }

        buf.append("[");

        for (String key : getSortedAttributes()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }

            buf.append(key);
            buf.append(": ");

            avm.get(key).appendWithIndex(visitedIndexedFs, reentrantFsToIndex, buf);
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
            
            for( String key : getSortedAttributes() ) {
                buf.append(String.format("%s%s:\n", prefix, key));
                avm.get(key).appendRawToString(buf, indent + 2);
            }

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

    /**
     * Makes a deep copy of this feature structure. The copy has no
     * nodes in common with the feature structure itself. This should be a little
     * faster than the generic implementation from the base class.
     *
     * @return
     */
    @Override
    public FeatureStructure deepCopy() {
        Map<AvmFeatureStructure, AvmFeatureStructure> originalNodeToCopyNode = new IdentityHashMap<>();
        Map<String, PlaceholderFeatureStructure> indexToPlaceholder = new HashMap<>();
        return recursiveDeepCopy(originalNodeToCopyNode, indexToPlaceholder);
    }

    private AvmFeatureStructure recursiveDeepCopy(Map<AvmFeatureStructure, AvmFeatureStructure> originalNodeToCopyNode, Map<String, PlaceholderFeatureStructure> indexToPlaceholder) {
        AvmFeatureStructure nodeInCopy = originalNodeToCopyNode.get(this);

        if( nodeInCopy != null ) {
            // previously visited this node
            return nodeInCopy;
        } else {
            nodeInCopy = new AvmFeatureStructure();
            originalNodeToCopyNode.put(this, nodeInCopy);

            for (Map.Entry<String, FeatureStructure> edge : avm.entrySet()) {
                FeatureStructure copyOfChild;
                if (edge.getValue() instanceof AvmFeatureStructure) {
                    AvmFeatureStructure child = (AvmFeatureStructure) edge.getValue();
                    copyOfChild = child.recursiveDeepCopy(originalNodeToCopyNode, indexToPlaceholder);
                } else if (edge.getValue() instanceof PlaceholderFeatureStructure) {
                    // Make exactly one copy per AVM of each placeholder FS, then reuse that copy
                    // throughout the AVM.
                    PlaceholderFeatureStructure child = (PlaceholderFeatureStructure) edge.getValue();
                    PlaceholderFeatureStructure p = indexToPlaceholder.get(child.getIndex());

                    if( p == null ) {
                        p = (PlaceholderFeatureStructure) child.deepCopy();
                        indexToPlaceholder.put(child.getIndex(), p);
                    }

                    copyOfChild = p;
                } else {
                    copyOfChild = edge.getValue().deepCopy();
                }

                nodeInCopy.put(edge.getKey(), copyOfChild);
            }

            return nodeInCopy;
        }
    }
}
