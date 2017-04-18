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
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A feature structure.
 *
 * @author koller
 */
public abstract class FeatureStructure {
    private String index = null;
    private int cachedHashCode = -1;

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

    protected static long globalCopyTimestamp = 1; // global unification operation ID, will be counted up for each call to unify() and checkSubsumption()

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
        FeatureStructure dg = dereference(currentTimestamp);
        FeatureStructure cp = dg.getCopy(currentTimestamp);

        if (cp != null) {
            return cp;
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
    
    protected FeatureStructure getCopy(long timestamp) {
        if( copy == null ) {
            return null;
        } else if( copyTimestamp < timestamp ) {
            return null;
        } else {
            return copy;
        }
    }

    
    
    
    /***************************************************************************
     * Subsumption checking
     **************************************************************************/
    
    public static int SUBSUMES_FORWARD = 2;
    public static int SUBSUMES_BACKWARD = 1;
    
    /**
     * Checks whether this FS subsumes another, i.e. whether all information
     * in this FS is also present in the other.
     * 
     * @param other
     * @return 
     */
    public boolean subsumes(FeatureStructure other) {
        return (checkSubsumptionBothWays(other) & SUBSUMES_FORWARD) != 0;
    }
    
    /**
     * Checks whether this FS subsumes another and vice versa. If this
     * FS subsumes the other, the resulting int will have the bit {@link #SUBSUMES_FORWARD}
     * set. If the other FS subsumes this one, the bit {@link #SUBSUMES_BACKWARD} will
     * be set. If the two FSs are equal, then both bits will be set.<p>
     * 
     * Subsumption checking is performed in linear time in the size of the
     * two FSs, using the algorithm from Malouf/Carroll/Copestake, "Efficient
     * feature structure operations without compilation", JNLE 2000.
     * 
     * @param other
     * @return 
     */
    public int checkSubsumptionBothWays(FeatureStructure other) {
        return checkSubsumptionBothWays(other, globalCopyTimestamp++, SUBSUMES_FORWARD | SUBSUMES_BACKWARD);
    }
    
    protected int checkSubsumptionBothWays(FeatureStructure other, long timestamp, int resultSoFar) {
        FeatureStructure cp1 = getCopy(timestamp);
        FeatureStructure cp2 = other.getCopy(timestamp);
        
        // check reentrancies
        if( cp1 == null ) {
            setCopy(other, timestamp);
        } else if( cp1 != other ) {
            resultSoFar &= ~SUBSUMES_FORWARD;
        }
        
        if( cp2 == null ) {
            other.setCopy(this, timestamp);
        } else if( cp2 != this ) {
            resultSoFar &= ~SUBSUMES_BACKWARD;
        }
        
        if( resultSoFar == 0 ) {
            return 0;
        }
        
        // check same node types
        if( getClass() != other.getClass() ) {
            return 0;
        }
        
        // check contents
        return checkSubsumptionValues(other, timestamp, resultSoFar);
    }
    
    abstract protected int checkSubsumptionValues(FeatureStructure other, long timestamp, int resultSoFar);

    
    
    
    
    
    /***************************************************************************
     * Equality checking
     **************************************************************************/
    
    /**
     * Checks two FSs for equality. This method is implemented in terms of
     * {@link #checkSubsumptionBothWays(de.saar.coli.featstruct.FeatureStructure) },
     * and runs in linear time in the size of the two FSs.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FeatureStructure) {
            FeatureStructure other = (FeatureStructure) obj;
            return checkSubsumptionBothWays(other) == (SUBSUMES_BACKWARD | SUBSUMES_FORWARD);
        } else {
            return false;
        }
    }

    /**
     * A simple implementation of hashCode that is guaranteed to assign
     * two FSs the same hash code if they have the same feature paths and
     * the same values under these feature paths. Reentrancies are ignored.
     * This may lead to slightly more hash collisions, but is easier
     * to implement.<p>
     * 
     * The hash code is cached after it is calculated, for efficiency
     * reasons. If you make destructive changes to the FS (beyond the
     * temporary changes to forward and copy), the hash code
     * may become invalid. If you need this, you will need to change
     * the caching mechanism.
     * 
     * @return 
     */
    @Override
    public int hashCode() {
        if( cachedHashCode == -1 ) {
            cachedHashCode = calculateHashCode();
        }
        
        return cachedHashCode;
    }
    
    abstract protected int calculateHashCode();
    
    
    
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
    
    
    abstract protected void forAllChildren(Consumer<FeatureStructure> fn);
    
    
    
    static final String FIRST_VISIT = "";
    
    void computeReentrancies(Map<FeatureStructure,String> reentrantFsToIndex, MutableInteger nextIndex) {
        if( ! reentrantFsToIndex.containsKey(this) ) {
            // first time we're visiting this FS
            if( this instanceof PlaceholderFeatureStructure ) {
                // placeholder FS => always mark with index
                reentrantFsToIndex.put(this, "#" + nextIndex.incValue());
            } else {
                // otherwise, only mark as a visited for the first time
                reentrantFsToIndex.put(this, FIRST_VISIT);
            }
            
            // do the same for all my children
            forAllChildren(ch -> ch.computeReentrancies(reentrantFsToIndex, nextIndex));
        } else {
            if( reentrantFsToIndex.get(this) == FIRST_VISIT ) {
                // second visit to this FS => generate new index for it
                reentrantFsToIndex.put(this, "#" + nextIndex.incValue());
            }
            
            // all further visits ignored, nothing else to do
        }
    }
    
    
    
    @Override
    public String toString() {
        Map<FeatureStructure,String> reentrantFsToIndex = new IdentityHashMap<>();
        computeReentrancies(reentrantFsToIndex, new MutableInteger(1));
        reentrantFsToIndex.values().removeAll(Collections.singleton(FIRST_VISIT)); // remove all non-reentrant nodes
        
        Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
        StringBuilder buf = new StringBuilder();

        appendWithIndex(visitedIndexedFs, reentrantFsToIndex, buf);
        return buf.toString();
    }

    protected void appendWithIndex(Set<FeatureStructure> previouslyVisitedFs, Map<FeatureStructure,String> reentrantFsToIndex, StringBuilder buf) {
        boolean printedIndexMarker = false;
        // reentrant node => print reentrancy marker
        if( reentrantFsToIndex.containsKey(this)) {
            buf.append(reentrantFsToIndex.get(this));
            printedIndexMarker = true;
        }
        
        // print value only for nodes that were not previously visited
        if( ! previouslyVisitedFs.contains(this) ) {
            previouslyVisitedFs.add(this);
            appendValue(previouslyVisitedFs, printedIndexMarker, reentrantFsToIndex, buf);
        }
    }

    protected abstract void appendValue(Set<FeatureStructure> visitedIndexedFs, boolean printedIndexMarker, Map<FeatureStructure,String> reentrantFsToIndex, StringBuilder buf);
    
    
    
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
    
    
    
    
    
    
    
    public static void main(String[] args) throws FsParsingException {
//        FeatureStructure fs1 = FeatureStructure.parse("[a: [b: c], d: [e: f]]");
//        FeatureStructure fs2 = FeatureStructure.parse("[a: #1 [b: c], d: #1, g: [h: j]]");
//        FeatureStructure ret = fs1.unify(fs2);

        FeatureStructure fs1 = FeatureStructure.parse("[a: s, b: #1]");
        FeatureStructure fs2 = FeatureStructure.parse("[a: #2, b: #2, c: t]");
        FeatureStructure expected = FeatureStructure.parse("[a: #3 s, b: #3, c: t]");
        FeatureStructure unif = fs1.unify(fs2);
        
        System.err.println(fs1);
        System.err.println(fs2);
        System.err.println(expected);
    }

}
