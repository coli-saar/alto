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
 * A feature structure.
 *
 * @author koller
 */
public abstract class FeatureStructure {
    private String index = null;

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
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
    
    protected String getIndexMarker() {
        return "#" + getIndex();
    }
    
    
    

    /**
     * Returns all feature paths that are defined in this FS.
     * This method is pretty inefficient.
     * 
     * @return 
     */
    abstract public List<List<String>> getAllPaths();
    

    /**
     * Returns the FS at the endpoint of a given path.
     * Returns null if no such path exists.
     * 
     * @param path
     * @return 
     */
    public FeatureStructure get(List<String> path) {
        return get(path, 0); // dereference().getD(path, 0);
    }

    /**
     * Returns the FS at the endpoint of a given path.
     * Returns null if no such path exists.
     * 
     * @param path
     * @return 
     */
    public FeatureStructure get(String... path) {
        return FeatureStructure.this.get(Arrays.asList(path));
    }
    
    abstract protected FeatureStructure get(List<String> path, int pos);
    
    

    /**
     * Returns the value stored in this feature structure.
     * This returns the primitive value if the FS is primitive,
     * and null otherwise.
     * 
     * @return 
     */
    abstract public Object getValue();

    
    
    
    
    /***************************************************************************
     * Fields and methods for the unification algorithm of Tomabechi (1991).
     * This algorithm performs non-destructive unification of feature
     * structures. It proceeds in two passes: First, the method unify1
     * traverses the two FSs, checking whether the two can be unified
     * and setting temporary pointers to the other FS. If this succeeds,
     * the method copyWithCompArcs traverses the FSs again and copies
     * FSs as needed, creating the FS for the result of the unification.
     * 
     **************************************************************************/
    
    private FeatureStructure forward = null; // called "pointer" in Jurafsky & Martin
    private long forwardTimestamp = -1;      // unify timestamp at which "forward" was set (for Tomabechi; there called "forward-mark")

    private FeatureStructure copy = null;    // for Tomabechi
    private long copyTimestamp = -1;         // for Tomabechi; there called "copy-mark"

    protected static long globalCopyTimestamp = 0; // global unification operation ID, will be counted up for each call to unify()

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
            dg1.setForward(dg2, currentTimestamp);
            return true;
        } else if (dg2 instanceof PlaceholderFeatureStructure) {
            dg2.setForward(dg1, currentTimestamp);
            return true;
        } else if (dg1 instanceof PrimitiveFeatureStructure && dg2 instanceof PrimitiveFeatureStructure) {
            PrimitiveFeatureStructure pdg1 = (PrimitiveFeatureStructure) dg1;
            PrimitiveFeatureStructure pdg2 = (PrimitiveFeatureStructure) dg2;

            if (pdg1.getValue().equals(pdg2.getValue())) {
                dg2.setForward(dg1, currentTimestamp);
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
     * }. If a copy was made previously, this copy is returned.
     * This is Tomabechi's function "copy-dg-with-comp-arcs".
     *
     * @param currentTimestamp
     * @return
     */
    protected FeatureStructure copyWithCompArcs(long currentTimestamp) {
        FeatureStructure dg = dereference(copyTimestamp);

        if (dg.copy != null && dg.copyTimestamp == currentTimestamp) {
            return dg.copy;
        } else {
            return dg.makeCopyWithCompArcs(currentTimestamp);
        }
    }

    /**
     * Implementation of Tomabechi's function "copy-dg-with-comp-arcs"
     * for the different types of feature structures.
     * 
     * @param currentTimestamp
     * @return 
     */
    abstract protected FeatureStructure makeCopyWithCompArcs(long currentTimestamp);
    
    /**
     * Follows forward pointers that are still valid for the current timestamp.
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
    
    /**
     * Sets a forward pointer.
     * 
     * @param other
     * @param currentTimestamp 
     */
    protected void setForward(FeatureStructure other, long currentTimestamp) {
        forward = other;
        forwardTimestamp = currentTimestamp;
    }
    
    /**
     * Stores a copy.
     * 
     * @param fs
     * @param timestamp 
     */
    protected void setCopy(FeatureStructure fs, long timestamp) {
        copy = fs;
        copyTimestamp = timestamp;
    }

    
    
    
    
    /***************************************************************************
     * Equality checking
     **************************************************************************/
    
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

            List<List<String>> paths = getAllPaths(); //dereference().getAllPathsD();
            Set<List<String>> otherPaths = new HashSet<>(other.getAllPaths());

            // FSs must have same set of paths
            if (!new HashSet(paths).equals(otherPaths)) {
                return false;
            }

            List<FeatureStructure> fsUnderPath = new ArrayList<>();         // dereferenced FSs in this, sorted as in "paths"
            List<FeatureStructure> fsUnderPathInOther = new ArrayList<>();  // dereferenced FSs in other, sorted as in "paths"

            // values must be the same
            for (List<String> path : paths) {
                FeatureStructure fs1 = FeatureStructure.this.get(path); //.dereference();
                FeatureStructure fs2 = other.get(path); //.dereference();

                fsUnderPath.add(fs1);
                fsUnderPathInOther.add(fs2);

                if (!fs1.localEquals(fs2)) {
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

    /**
     * Checks whether the two feature structures are locally equal.
     * This should return true if both FSs are AVMs or both placeholders.
     * If both FSs are primitives, returns true iff the values are equal.
     * In all other cases (in particular, if the two FSs have different types),
     * returns false.
     * 
     * @param other
     * @return 
     */
    abstract protected boolean localEquals(FeatureStructure other);
    
    
    
    
    
    
    /***************************************************************************
     * Printing
     **************************************************************************/
    
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
    
    protected static String n(Object e) {
        if (e == null) {
            return "<null>";
        } else {
            return e.toString();
        }
    }
    
    @Override
    public String toString() {
        Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
        StringBuilder buf = new StringBuilder();

        appendWithIndex(visitedIndexedFs, buf);
        return buf.toString();
    }

    protected void appendWithIndex(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
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
    
    
    
    
    
    /***************************************************************************
     * Management of global FS IDs for pretty-printing. It is often useful
     * to abbreviate the identity of an FS by just a numeric identifier. These
     * identifiers are managed here.
     **************************************************************************/
    
    private static Map<FeatureStructure, Integer> previouslyPrinted = new IdentityHashMap<>();
    private static MutableInteger nextPpId = new MutableInteger(1);
    
    protected int findPreviousId() {
        Integer id = previouslyPrinted.get(this);

        if (id == null) {
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
        if (id > -1) {
            return id;
        } else {
            return makeId();
        }
    }
    
    
    
    
    
    
    
    
/*
    public static void main(String[] args) throws FsParsingException {
        FeatureStructure fs1 = FeatureStructure.parse("[a: [b: c], d: [e: f]]");
        FeatureStructure fs2 = FeatureStructure.parse("[a: #1 [b: c], d: #1, g: [h: j]]");
        FeatureStructure ret = fs1.unify(fs2);

        System.err.println(ret);
        System.err.println(ret.rawToString());
    }
*/
}
