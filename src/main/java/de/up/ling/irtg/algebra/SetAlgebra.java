// Copyright 2012-2017 Alexander Koller
// Copyright 2019 Arne Köhn
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.up.ling.irtg.algebra;

import com.google.common.collect.Sets;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.FirstOrderModel;
import de.up.ling.irtg.util.Lazy;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
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
 * <li> project_i(R) projects its argument R (a k-place relation with k ≥ i) to
 * the set of i-th elements of this relation.</li>
 * <li>intersect_i(R,F) returns the set of all tuples of R (a k-place relation
 * with k ≥ i) whose i-th element is a member of F (a subset of the
 * universe).</li>
 * <li>intersect(A,B) returns the intersections of the sets A and B.</li>
 * <li>union(A,B) returns the union of the sets A and B.</li>
 * <li>uniq_a(A) returns the set A (a subset of the universe) itself if A = {a};
 * otherwise it returns the empty set.</li>
 * <li>size_i(A) returns the set A (a subset of the universe) itself if size(A) = i;
 * otherwise it returns the empty set.</li>
 * <li>member_a(A) returns the set {{a}} if {a} belongs to A;
 * otherwise it returns the empty set.</li> 
 * <li>T returns the universe.</li>
 * </ul>
 *
 * Importantly, the decomposition automata for this algebra only implement
 * {@link TreeAutomaton#getRulesBottomUp(int, int[])}, not
 * {@link TreeAutomaton#getRulesTopDown(int, int)}. This means that you need to
 * take care to only ever call methods on them that look at rules bottom-up.
 */

public class SetAlgebra extends Algebra<Set<List<String>>> {

    private static final String PROJECT = "project_";
    private static final String INTERSECT = "intersect_";
    private static final String INTERSECTSETS = "intersect";
    private static final String UNIQ = "uniq_";
    private static final String SIZE = "size_";
    private static final String UNION = "union";
    private static final String MEMBER = "member_";
    private static final String DIFFERENCE = "diff";
    private static final String TOP = "T";
    private static final String[] SPECIAL_STRINGS = {PROJECT, INTERSECT, INTERSECTSETS, UNIQ, UNION, SIZE, MEMBER};
    
    private FirstOrderModel model;
    
    private Lazy<IntSet> allStates = null;

    /**
     * Creates a new instance with a new model and signature.
     */
    public SetAlgebra() {
        this.model = new FirstOrderModel();
        allStates = null;
    }

    /**
     * Creates a new instance with a new signature and model, which adds the
     * given atomicInterpretations to the model.
     * 
     * @param atomicInterpretations 
     */
    public SetAlgebra(Map<String, Set<List<String>>> atomicInterpretations) {
        this();

        setAtomicInterpretations(atomicInterpretations);
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    /**
     * Writes the interpretations of the atomic concepts to the given Writer as a Json string.
     * @throws IOException
     *
     * @see #readOptions(java.io.Reader)
     */
    @Override
    public void writeOptions(Writer optionWriter) throws IOException {
        optionWriter.append(model.toString());
    }

    @Override
    public TreeAutomaton<Set<List<String>>> decompose(Set<List<String>> value) {
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

    public FirstOrderModel getModel() {
        return model;
    }

    public void setModel(FirstOrderModel model) {
        this.model = model;
    }
    
    /**
     * @return the atomicInterpretations
     */
    public Map<String, Set<List<String>>> getAtomicInterpretations() {
        return model.getAtomicInterpretations();
    }

    /**
     * Evaluates the operation given by the label on the child values.
     * Most of the operations are parametrized using an underscore.
     * E.g., project_1 projects all tuples of the first child value
     * to one-element tuples.
     */
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
        } else if (label.equals(UNION)) {
            ret = new HashSet<List<String>>();
            for (Set<List<String>> i: childrenValues) {
                ret.addAll(i);
            }
		} else if (label.equals(INTERSECTSETS)) {
            ret = childrenValues.get(0);
            for (Set<List<String>> i: childrenValues) {
                ret = Sets.intersection(ret, i);
            }
        } else if (label.startsWith(SIZE)) {
            ret = size(childrenValues.get(0), arg(label));
        } else if (label.startsWith(MEMBER)) {
            ret = member(childrenValues.get(0), arg(label));
        } else if (label.equals(DIFFERENCE)) {
            ret = childrenValues.get(0);
            for (int i=1; i < childrenValues.size(); i++) {
                ret = Sets.difference(ret, childrenValues.get(i));
            }
        } else if (label.equals(TOP)) {
            ret = model.getUniverseAsTuples();
        } else {
            ret = model.getInterpretation(label);
        }

        return ret;
    }

    private Set<List<String>> project(Set<List<String>> tupleSet, int pos) {
        Set<List<String>> ret = new HashSet<>();

        for (List<String> tuple : tupleSet) {
            List<String> l = new ArrayList<>();

            if (pos < tuple.size()) {
                l.add(tuple.get(pos));
                ret.add(l);
            }
        }

        return ret;
    }

    private Set<List<String>> intersect(Set<List<String>> tupleSet, Set<List<String>> filterSet, int pos) {
        Set<String> filter = new HashSet<>();
        Set<List<String>> ret = new HashSet<>();

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

    private Set<List<String>> member(Set<List<String>> tupleSet, String value) {
        List<String> memberValue = new ArrayList<>();
        
        memberValue.add(value);

        Set<List<String>> ret = new HashSet<>();

        if (tupleSet.contains(memberValue)) {
            ret.add(memberValue);
        }

        return ret;
    }

    private Set<List<String>> uniq(Set<List<String>> tupleSet, String value) {
        List<String> uniqArg = new ArrayList<>();

        uniqArg.add(value);

        if (tupleSet.size() == 1 && tupleSet.iterator().next().equals(uniqArg)) {
            return tupleSet;
        } else {
            return new HashSet<>();
        }
    }

    private Set<List<String>> size(Set<List<String>> tupleSet, String value) {
        if (tupleSet.size() == Integer.parseInt(value)) {
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

        @Override
        public String toString() {
            return "NoModelException: " + getMessage();
        }
    }
}
