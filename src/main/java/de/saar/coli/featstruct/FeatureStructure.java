/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.saar.basic.IdentityHashSet;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.irtg.util.Util;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Convention: (protected) methods that end in D can only be called on
 * dereferenced FSs, i.e. on FSs with this == dereference().
 *
 * @author koller
 */
public abstract class FeatureStructure {
    private String index = null;

    // These fields are only visible to subclasses so implementations of appendRawToString can
    // access them for pretty-printing. Don't use them directly from your code;
    // use dereference(), setForwardD(), and setCopyD() instead.
    private FeatureStructure forward = null; // called "pointer" in Jurafsky & Martin
    private long forwardTimestamp = -1;      // unify timestamp at which "forward" was set (for Tomabechi; there called "forward-mark")

    private FeatureStructure copy = null;    // for Tomabechi
    private long copyTimestamp = -1;         // for Tomabechi; there called "copy-mark"

    protected static long globalCopyTimestamp = 0; // global unification operation ID, will be counted up for each call to unify()
    
    // for pretty-printing
    private static Map<FeatureStructure, Integer> previouslyPrinted = new IdentityHashMap<>();
    private static MutableInteger nextPpId = new MutableInteger(1);
    

    public static FeatureStructure parse(String s) throws FsParsingException {
        try {
            return new FsParser().parse(new ByteArrayInputStream(s.getBytes()));
        } catch (IOException ex) {
            // this should not happen
            Logger.getLogger(FeatureStructure.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public String getIndex() {
//        return dereference().index;
        return getIndexD();
    }

    protected String getIndexD() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    /**
     * Follows the "forward" links and returns the FS at the endpoint of the
     * non-null forward links.
     *
     * @return
     */
    @Deprecated
    protected FeatureStructure dereference() {
        if (forward == null) {
            return this;
        } else {
            return forward.dereference();
        }
    }

    /**
     * Tomabechi-style dereferencing: follow only forward links as long as they
     * are from the current timestamp.
     *
     * @param timestamp
     * @return
     */
    protected FeatureStructure dereference(long timestamp) {
        if (forward == null) {
            return this;
        } else if (forwardTimestamp < timestamp) {
            return this;
        } else {
            return forward.dereference(timestamp);
        }
    }

    @Override
    public String toString() {
        Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
        StringBuilder buf = new StringBuilder();
        
        appendWithIndexD(visitedIndexedFs, buf);
//        dereference().appendWithIndexD(visitedIndexedFs, buf);
        return buf.toString();
    }

    protected String getIndexMarker() {
        return "#" + getIndex();
    }

    protected void appendWithIndexD(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        // prepend index if needed
        if (getIndex() != null) {
            buf.append(getIndexMarker());
        }

        if (!visitedIndexedFs.contains(this)) {
            // mark as visited
            if (getIndex() != null) {
                visitedIndexedFs.add(this);
                buf.append(" ");
            }

            // print actual value
            appendValue(visitedIndexedFs, buf);
        }
    }

    protected abstract void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf);

    /**
     * Unifies this FS destructively with another FS, using the Pereira (1985)
     * algorithm. Returns the unification of the two FSs if it exists, null
     * otherwise.
     *
     * @param other
     * @return
     */
    /*
    public FeatureStructure destructiveUnify(FeatureStructure other) {
        FeatureStructure d1 = dereference();
        FeatureStructure d2 = other.dereference();

        if (d1 == d2) {
            return d1;
        } else {
            return d1.destructiveUnifyLocalD(d2);
        }
    }

    protected abstract FeatureStructure destructiveUnifyLocalD(FeatureStructure other);
    */

    /**
     * Unifies this FS with another FS, using the Tomabechi (1991) algorithm.
     * The two FSs are not modified; instead, copies of FS objects are created
     * by need. Returns the unification of the two FSs if it exists, null
     * otherwise.
     *
     * @param other
     * @return
     */
    public FeatureStructure unify(FeatureStructure other) {
        return unify0(other, globalCopyTimestamp++);
    }

    protected FeatureStructure unify0(FeatureStructure other, long currentTimestamp) {
        boolean unifiable = unify1(other, currentTimestamp);

        if (unifiable) {
//            System.err.println("before copying:\n" + rawToString());
            return copyWithCompArcs(currentTimestamp);
        } else {
            return null;
        }
    }

    /**
     * Traverses an FS and sets temporary forwarding pointers, without making
     * copies of any FS. Returns true iff the two FSs are unifiable. This is
     * Tomabechi's function "unify1".
     *
     * @param other
     * @param currentTimestamp
     * @return
     */
    protected boolean unify1(FeatureStructure other, long currentTimestamp) {
        FeatureStructure dg1 = dereference(currentTimestamp);
        FeatureStructure dg2 = other.dereference(currentTimestamp);

        if (dg1 == dg2) {
            return true;
        } else if (dg1 instanceof PlaceholderFeatureStructure) {
            dg1.setForwardD(dg2, currentTimestamp);
            return true;
        } else if (dg2 instanceof PlaceholderFeatureStructure) {
            dg2.setForwardD(dg1, currentTimestamp);
            return true;
        } else if (dg1 instanceof PrimitiveFeatureStructure && dg2 instanceof PrimitiveFeatureStructure) {
            PrimitiveFeatureStructure pdg1 = (PrimitiveFeatureStructure) dg1;
            PrimitiveFeatureStructure pdg2 = (PrimitiveFeatureStructure) dg2;

            if (pdg1.getValueD().equals(pdg2.getValueD())) {
                dg2.setForwardD(dg1, currentTimestamp);
                return true;
            } else {
                return false;
            }
        } else if (dg1 instanceof PrimitiveFeatureStructure || dg2 instanceof PrimitiveFeatureStructure) {
            return false;
        } else {
            AvmFeatureStructure adg1 = (AvmFeatureStructure) dg1;
            AvmFeatureStructure adg2 = (AvmFeatureStructure) dg2;
            return adg1.unify1AvmD(adg2, currentTimestamp);
        }
    }

    /**
     * Makes a copy of an FS that was previously processed by {@link #unify1(de.saar.coli.featstruct.FeatureStructure, long)
     * }. This is Tomabechi's function "copy-dg-with-comp-arcs".
     *
     * @param currentTimestamp
     * @return
     */
    protected FeatureStructure copyWithCompArcs(long currentTimestamp) {
        FeatureStructure dg = dereference(copyTimestamp);
        
//        System.err.printf("copyWithCompArcs #%d, deref -> #%d\n", findOrMakeId(), dg.findOrMakeId());

        if (dg.copy != null && dg.copyTimestamp == currentTimestamp) {
//            System.err.printf("- has copy #%d, return that\n", dg.copy.findOrMakeId());
            return dg.copy;
        } else {
//            System.err.println("- need to make new copy");
            return dg.copyWithCompArcsD(currentTimestamp);
        }
    }

    abstract protected FeatureStructure copyWithCompArcsD(long currentTimestamp);

    protected void setForwardD(FeatureStructure other, long currentTimestamp) {
        forward = other;
        forwardTimestamp = currentTimestamp;
    }

    /**
     * Checks two FSs for equality. Note that this method is pretty slow, it
     * should only be used in testing and debugging.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FeatureStructure) {
            FeatureStructure other = (FeatureStructure) obj; //((FeatureStructure) obj).dereference();

            List<List<String>> paths = getAllPathsD(); //dereference().getAllPathsD();
            Set<List<String>> otherPaths = new HashSet<>(other.getAllPathsD());

            // FSs must have same set of paths
            if (!new HashSet(paths).equals(otherPaths)) {
                return false;
            }

            List<FeatureStructure> fsUnderPath = new ArrayList<>();         // dereferenced FSs in this, sorted as in "paths"
            List<FeatureStructure> fsUnderPathInOther = new ArrayList<>();  // dereferenced FSs in other, sorted as in "paths"

            // values must be the same
            for (List<String> path : paths) {
                FeatureStructure fs1 = get(path); //.dereference();
                FeatureStructure fs2 = other.get(path); //.dereference();

                fsUnderPath.add(fs1);
                fsUnderPathInOther.add(fs2);

                if (!fs1.localEqualsD(fs2)) {
                    return false;
                }
            }

            // coindexations must match
            for (int i = 0; i < paths.size(); i++) {
                for (int j = i + 1; j < paths.size(); j++) {
                    boolean coindexed = (fsUnderPath.get(i) == fsUnderPath.get(j));
                    boolean coindexedOther = (fsUnderPathInOther.get(i) == fsUnderPathInOther.get(j));

                    if (coindexed != coindexedOther) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    abstract protected List<List<String>> getAllPathsD();

    public List<List<String>> getAllPaths() {
        return getAllPathsD(); //dereference().getAllPathsD();
    }

    public FeatureStructure get(List<String> path) {
        return getD(path, 0); // dereference().getD(path, 0);
    }

    public FeatureStructure get(String... path) {
        return get(Arrays.asList(path));
    }

    public Object getValue() {
        return getValueD(); //dereference().getValueD();
    }

    abstract protected Object getValueD();

    abstract protected FeatureStructure getD(List<String> path, int pos);

    abstract protected boolean localEqualsD(FeatureStructure other);

    public static void main(String[] args) throws FsParsingException {
        FeatureStructure fs1 = FeatureStructure.parse("[a: [b: c], d: [e: f]]");
        FeatureStructure fs2 = FeatureStructure.parse("[a: #1 [b: c], d: #1, g: [h: j]]");
        FeatureStructure ret = fs1.unify(fs2);

        System.err.println(ret);
        System.err.println(ret.rawToString());
    }

    protected void setCopyD(FeatureStructure fs, long timestamp) {
        copy = fs;
        copyTimestamp = timestamp;
    }

    public String rawToString() {
        StringBuilder buf = new StringBuilder();
        Map<FeatureStructure, Integer> previouslyPrinted = new IdentityHashMap<>();
        MutableInteger fsId = new MutableInteger(1);

        buf.append("=== Feature structure, global timestamp=" + globalCopyTimestamp + "\n");
        appendRawToString(buf, 0);

        return buf.toString();
    }

    abstract protected void appendRawToString(StringBuilder buf, int indent);

    protected void appendForwardAndCopy(StringBuilder buf, int indent) {
        String prefix = Util.repeat(" ", indent);
        
        if (forward != null) {
            buf.append(String.format("%s-- forward pointer (timestamp %d) to:\n", prefix, forwardTimestamp));
            forward.appendRawToString(buf, indent + 3);
        }

        if (copy != null) {
            buf.append(String.format("%s-- has copy (timestamp %s):\n", prefix, copyTimestamp));
            copy.appendRawToString(buf, indent + 3);
        }
    }
    
    protected int findPreviousId() {
        Integer id = previouslyPrinted.get(this);
        
        if( id == null ) {
            return -1;
        } else {
            return id;
        }
    }

    protected int makeId() {
        int id = nextPpId.incValue();
        previouslyPrinted.put(this, id);
        return id;
    }
    
    protected int findOrMakeId() {
        int id = findPreviousId();
        if( id > -1 ) {
            return id;
        } else {
            return makeId();
        }
    }

    protected static String n(Object e) {
        if (e == null) {
            return "<null>";
        } else {
            return e.toString();
        }
    }

}
