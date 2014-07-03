/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.Sets;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.FirstOrderModel;
import de.up.ling.irtg.util.Lazy;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An algebra of sets. The elements of this algebra are relations (of arbitrary
 * arity) over some given universe. The algebra interprets the following
 * operations:
 *
 * <ul>
 * <li> project_i(R) projects its argument R (a k-place relation with k >= i) to
 * the set of i-th elements of this relation.</li>
 * <li>intersect_i(R,F) returns the set of all tuples of R (a k-place relation
 * with k >= i) whose i-th element is a member of F (a subset of the
 * universe).</li>
 * <li>uniq_a(A) returns the set A (a subset of the universe) itself if A = {a};
 * otherwise it returns the empty set.</li>
 * <li>member_a(A) returns the set {{a}} if {a} belongs to A;
 * otherwise it returns the empty set.</li> 
 * <li>T returns the universe.</li>
 * </ul>
 *
 * Importantly, the decomposition automata for this algebra only implement
 * {@link TreeAutomaton#getRulesBottomUp(int, int[])}, not
 * {@link TreeAutomaton#getRulesTopDown(int, int)}. This means that you need to
 * take care to only ever call methods on them that look at rules bottom-up.<p>
 *
 * @author koller
 */

public class SetAlgebra extends EvaluatingAlgebra<Set<List<String>>> {

    private static final String PROJECT = "project_";
    private static final String INTERSECT = "intersect_";
    private static final String UNIQ = "uniq_";
    private static final String MEMBER = "member_";
    private static final String TOP = "T";
    private static final String[] SPECIAL_STRINGS = {PROJECT, INTERSECT, UNIQ, MEMBER};
    
    private FirstOrderModel model;
    
//    private Map<String, Set<List<String>>> atomicInterpretations;
//    private int maxArity;
//    private Set<String> allIndividuals;
//    private Set<List<String>> allIndividualsAsTuples;
    private Lazy<IntSet> allStates = null;

    public SetAlgebra() {
        this.model = new FirstOrderModel();
//        this.atomicInterpretations = null;
//        maxArity = 0;
//
//        allIndividuals = new HashSet<String>();
//        allIndividualsAsTuples = new HashSet<List<String>>();
        allStates = null;
    }

    public SetAlgebra(Map<String, Set<List<String>>> atomicInterpretations) {
        this();

        setAtomicInterpretations(atomicInterpretations);
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    /**
     * Writes the interpretations of the atomic concepts to the given Writer.
     * This method should encode into a Json string, but is not yet implemented.
     *
     * @see #readOptions(java.io.Reader)
     * @param optionWriter
     * @throws Exception
     */
    @Override
    public void writeOptions(Writer optionWriter) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public TreeAutomaton decompose(Set<List<String>> value) {
        return new EvaluatingDecompositionAutomaton(value) {
            @Override
            public IntSet getAllStates() {
                if (allStates == null) {
                    allStates = new IntOpenHashSet();

                    // states = are all subsets of U^0, U^1, ..., U^maxarity
                    for (int i = 0; i < model.getMaxArity(); i++) {
                        List<Set<String>> manySets = new ArrayList<>();

                        for (int j = 0; j < i; j++) {
                            manySets.add(model.getUniverse());
                        }

                        Set<List<String>> cartesian = Sets.cartesianProduct(manySets);
                        Set<Set<List<String>>> powerSet = Sets.powerSet(cartesian);

                        for (Set<List<String>> set : powerSet) {
                            int state = addState(set);
                            allStates.add(state);
                        }
                    }
                }

                return allStates;
            }
        };
    }

    /**
     * Reads the options from a Json string representation. The options for the
     * SetAlgebra consist in a specification of the universe and the
     * interpretations of the atomic concepts. For instance, the following
     * string says that "sleep" is a binary relation with the single element
     * (e,r1), whereas "rabbit" is a unary relation containing the elements r1
     * and r2.<p>
     *
     * {"sleep": [["e", "r1"]], "rabbit": [["r1"], ["r2"]], "white": [["r1"],
     * ["b"]], "in": [["r1","h"], ["f","h2"]], "hat": [["h"], ["h2"]] }
     *
     *
     * @param optionReader
     * @throws Exception
     */
    @Override
    public void readOptions(Reader optionReader) throws Exception {
        model = FirstOrderModel.read(optionReader);
    }

    public final void setAtomicInterpretations(Map<String, Set<List<String>>> atomicInterpretations) {
        model.setAtomicInterpretations(atomicInterpretations);
        allStates = null;
    }

    /**
     * @return the atomicInterpretations
     */
    public Map<String, Set<List<String>>> getAtomicInterpretations() {
        return model.getAtomicInterpretations();
    }

    protected Set<List<String>> evaluate(String label, List<Set<List<String>>> childrenValues) throws NoModelException {
        Set<List<String>> ret = null;

        if (model == null) {
            throw new NoModelException("SetAlgebra has no model yet.");
        }

        if (label.startsWith(PROJECT)) {
            ret = project(childrenValues.get(0), Integer.parseInt(arg(label)) - 1);
        } else if (label.startsWith(INTERSECT)) {
            ret = intersect(childrenValues.get(0), childrenValues.get(1), Integer.parseInt(arg(label)) - 1);
        } else if (label.startsWith(UNIQ)) {
            ret = uniq(childrenValues.get(0), arg(label));
        } else if (label.startsWith(MEMBER)) {
            ret = member(childrenValues.get(0), arg(label));
        } else if (label.equals(TOP)) {
            ret = model.getUniverseAsTuples();
        } else {
            ret = model.getInterpretation(label);
        }

        return ret;
    }

    private Set<List<String>> project(Set<List<String>> tupleSet, int pos) {
        Set<List<String>> ret = new HashSet<List<String>>();

        for (List<String> tuple : tupleSet) {
            List<String> l = new ArrayList<String>();

            if (pos < tuple.size()) {
                l.add(tuple.get(pos));
                ret.add(l);
            }
        }

        return ret;
    }

    private Set<List<String>> intersect(Set<List<String>> tupleSet, Set<List<String>> filterSet, int pos) {
        Set<String> filter = new HashSet<String>();
        Set<List<String>> ret = new HashSet<List<String>>();

        for (List<String> f : filterSet) {
            filter.add(f.get(0));
        }

        for (List<String> tuple : tupleSet) {
            if (pos < tuple.size()) {
                if (filter.contains(tuple.get(pos))) {
                    ret.add(tuple);
                }
            }
        }

        return ret;
    }

    /**
     * returns the smallSet, if it's a subset of the bigSet. Otherwise returns
     * the empty set.
     *
     * @param bigSet
     * @param smallSet
     * @param pos
     * @return
     */
    private Set<List<String>> member(Set<List<String>> tupleSet, String value) {
        List<String> memberValue = new ArrayList<String>();
        
        memberValue.add(value);

        Set<List<String>> ret = new HashSet<List<String>>();

        if (tupleSet.contains(memberValue)) {
            ret.add(memberValue);
        }

        return ret;
    }

    private Set<List<String>> uniq(Set<List<String>> tupleSet, String value) {
        List<String> uniqArg = new ArrayList<String>();

        uniqArg.add(value);

        if (tupleSet.size() == 1 && tupleSet.iterator().next().equals(uniqArg)) {
            return tupleSet;
        } else {
            return new HashSet<List<String>>();
        }
    }

    private static String arg(String stringWithArg) {
        for (String s : SPECIAL_STRINGS) {
            if (stringWithArg.startsWith(s)) {
                return stringWithArg.substring(s.length());
            }
        }

        return null;
    }

    @Override
    protected boolean isValidValue(Set<List<String>> value) {
        return !value.isEmpty();
    }

    @Override
    public Set<List<String>> parseString(String representation) throws ParserException {
        try {
            return SetParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }

    }

    public static class NoModelException extends RuntimeException {

        public NoModelException() {
        }

        public NoModelException(String message) {
            super(message);
        }

        public NoModelException(String message, Throwable cause) {
            super(message, cause);
        }

        public NoModelException(Throwable cause) {
            super(cause);
        }

//        public NoModelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
//            super(message, cause, enableSuppression, writableStackTrace);
//        }
        @Override
        public String toString() {
            return "NoModelException: " + getMessage();
        }
    }
}
