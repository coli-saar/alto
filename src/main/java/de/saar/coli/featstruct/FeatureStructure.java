/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.saar.basic.IdentityHashSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    protected String index = null;
    protected FeatureStructure forward = null; // called "pointer" in Jurafsky & Martin

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
        return dereference().index;
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
    protected FeatureStructure dereference() {
        if (forward == null) {
            return this;
        } else {
            return forward.dereference();
        }
    }

    @Override
    public String toString() {
        Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
        StringBuilder buf = new StringBuilder();
        dereference().appendWithIndex(visitedIndexedFs, buf);
        return buf.toString();
    }

    protected String getIndexMarker() {
        return "#" + getIndex();
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

    /**
     * Unifies this FS destructively with another FS, using the Pereira (1985)
     * algorithm. Returns the unification of the two FSs if it exists, null
     * otherwise.
     *
     * @param other
     * @return
     */
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
            FeatureStructure other = ((FeatureStructure) obj).dereference();

            List<List<String>> paths = dereference().getAllPathsD();
            Set<List<String>> otherPaths = new HashSet<>(other.getAllPathsD());

            // FSs must have same set of paths
            if (!new HashSet(paths).equals(otherPaths)) {
                return false;
            }

            List<FeatureStructure> fsUnderPath = new ArrayList<>();         // dereferenced FSs in this, sorted as in "paths"
            List<FeatureStructure> fsUnderPathInOther = new ArrayList<>();  // dereferenced FSs in other, sorted as in "paths"

            // values must be the same
            for (List<String> path : paths) {
                FeatureStructure fs1 = get(path).dereference();
                FeatureStructure fs2 = other.get(path).dereference();

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
        return dereference().getAllPathsD();
    }

    public FeatureStructure get(List<String> path) {
        return dereference().getD(path, 0);
    }

    public FeatureStructure get(String... path) {
        return get(Arrays.asList(path));
    }

    public Object getValue() {
        return dereference().getValueD();
    }

    abstract protected Object getValueD();

    abstract protected FeatureStructure getD(List<String> path, int pos);

    abstract protected boolean localEqualsD(FeatureStructure other);

    public static void main(String[] args) throws FsParsingException {
        FeatureStructure fs1 = FeatureStructure.parse("[num: sg]");
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc]");        
        FeatureStructure ret = fs1.destructiveUnify(fs2);

        System.err.println(ret);
    }
}
