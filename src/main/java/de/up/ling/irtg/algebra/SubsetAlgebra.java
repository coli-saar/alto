// Copyright 2017 Alexander Koller
// Copyright 2019-2020 Arne Köhn
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

import de.saar.basic.StringTools;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.util.Logging;
import java.io.Reader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple algebra performing union and disjoint union on sets.
 *
 * Sets are represented as elem1+elem2+elem3 and can be combined
 * using eunion (extending union) dunion (disjoint) or union (non-disjoint).
 * Combining two non-disjoint sets using dunion results in the empty set.
 * Combining a set with a subset of itself with eunion results in an empty set.
 */
public class SubsetAlgebra extends Algebra<BitSet> {

    private boolean emptyStatesAllowed = false;
    private Interner<String> universeInterner;
    private static final BitSet EMPTY_BITSET = new BitSet();

    public static final String DISJOINT_UNION = "dunion";
	public static final String UNION = "union";
    /** Extending Union.  returns the union if both sets contribute to the result
     * (i.e. none is a superset of the other)
     */
	public static final String EUNION = "eunion";
    /**
     * Evaluates to the first argument and ignores the rest
     * if any argument is null, evaluates to null instead.
     */
	public static final String FIRST_ARG = "firstarg";
    /** returns first argument if it contains the second argument. */
	public static final String CONTAINS = "contains";
    /** returns first argument if it does not contain the second argument. */
    public static final String NOT_CONTAINS = "not_contains";
    public static final String EMPTYSET = "EMPTYSET";
    private static final String SEPARATOR_RE = "\\s*\\+\\s*";
    public static final String SEPARATOR = " + ";


    /**
     * Don't call this constructor directly; it is only needed so
     * an IRTG with this algebra can be constructed by a codec.
     */
    public SubsetAlgebra() {
        this(Collections.emptySet());
    }

    public SubsetAlgebra(Set<String> universe) {
        setUniverse(universe);
    }

    private void setUniverse(Set<String> universe) {
        universeInterner = new Interner<>();

        for (String element : universe) {
            universeInterner.addObject(element);
        }
    }

    @Override
    public void readOptions(Reader optionReader) throws Exception {
      setUniverse(parseStringSet(StringTools.slurp(optionReader)));
    }

    private static boolean disjoint(BitSet s1, BitSet s2) {
        return !s1.intersects(s2);
    }

    /**
     * Checks if s1 is a subset of s2.
     */
    private static boolean subset(BitSet s1, BitSet s2) {
        int currIndex = s1.nextSetBit(0);
        while (currIndex >= 0) {
            if (!s2.get(currIndex)) {
                return false;
            }
            currIndex = s1.nextSetBit(currIndex+1);
        }
        return true;
    }

    @Override
    protected BitSet evaluate(String label, List<BitSet> childrenValues) {
        if (childrenValues.contains(null)) {
            return null;
        }
        if (NOT_CONTAINS.equals(label)) {
            assert childrenValues.size() == 2;
            if (! subset(childrenValues.get(1), childrenValues.get(0))) {
                return childrenValues.get(1);
            }
            return null;
        }
        if (CONTAINS.equals(label)) {
            assert childrenValues.size() == 2;
            if (subset(childrenValues.get(1), childrenValues.get(0))) {
                return childrenValues.get(1);
            }
            return null;
        }
        if (FIRST_ARG.equals(label)) {
            return childrenValues.get(0);
        }
        if (UNION.equals(label)) {
            BitSet ret = new BitSet();
            for (BitSet bs: childrenValues) {
                ret.or(bs);
            }
            return ret;
        }

        if (EUNION.equals(label)) {
            assert childrenValues.size() == 2;
            BitSet s1 = childrenValues.get(0);
            BitSet s2 = childrenValues.get(1);
            BitSet result = new BitSet();
            result.or(s1);
            result.or(s2);
            if (result.cardinality() == s1.cardinality() || result.cardinality() == s2.cardinality()) {
                // s1 was a superset of s2 or vice versa
                return null;
            }
            return result;
        }

        if (DISJOINT_UNION.equals(label)) {
            assert childrenValues.size() == 2;

            BitSet s1 = childrenValues.get(0);
            BitSet s2 = childrenValues.get(1);
            if (disjoint(s1, s2)) {
                BitSet ret = new BitSet();
                ret.or(s1);
                ret.or(s2);
                return ret;
            } else {
                return null;
            }
        } else if (EMPTYSET.equals(label)) {
            assert childrenValues.isEmpty();
            return EMPTY_BITSET;
        } else {
            assert childrenValues.isEmpty();
            try {
                BitSet ret = parseString(label);
                
                if( ret == null ) {
                    throw new RuntimeException("Could not parse set constant: " + label);
                }
                return ret;
            } catch (ParserException ex) {
                throw new RuntimeException("Could not parse set constant: " + label, ex);
            }
        }
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    public BitSet toBitset(Set<String> s) {
        if (s == null) {
            return null;
        } else {
            BitSet ret = new BitSet();

            for (String element : s) {
                int x = universeInterner.resolveObject(element);

                if (x > 0) {
                    ret.set(x - 1); // subtract one because interner IDs start at 1
                } else {
                    Logging.get().severe(() -> "Not a subset of universe: " + element.toString());
                    return null;
                }
            }

            return ret;
        }
    }

    /**
     * Set whether states with an empty set should be allowd or pruned from the chart.
     */
    public void setEmptyStatesAllowed(boolean allowed) {
        this.emptyStatesAllowed = allowed;
    }

    @Override
    protected boolean isValidValue(BitSet value) {
        return value != null && (emptyStatesAllowed || !value.isEmpty());
    }

    @Override
    public BitSet parseString(String representation) throws ParserException {
        Set<String> s = parseStringSet(representation);
        return toBitset(s);
    }

    public static Set<String> parseStringSet(String representation) {
        String s = representation.trim();

        if (s.length() == 0) {
            return Collections.emptySet();
        } else {
            String[] parts = s.split(SEPARATOR_RE);
            return new HashSet<>(Arrays.asList(parts));
        }
    }

    @Override
    public TreeAutomaton<BitSet> decompose(BitSet set) {
        return new DecompositionAuto(set);
    }

    /**
     * Perform decomposition with several possible target states.
     */
    public TreeAutomaton<BitSet> decompose(Set<BitSet> sets) {
        return new DecompositionAuto(sets);
    }

    private class DecompositionAuto extends EvaluatingDecompositionAutomaton {

        private final Set<BitSet> finalSets;

        public DecompositionAuto(BitSet finalSet) {
            super(SubsetAlgebra.this.getSignature());

            this.finalSets = new HashSet<>();
            finalSets.add(finalSet);
        }

        /**
         * Construct a decomposition automaton with several possible
         * final states.
         */
        public DecompositionAuto(Set<BitSet> finalSets) {
            super(SubsetAlgebra.this.getSignature());
            this.finalSets = finalSets;
        }

        @Override
        protected int addState(BitSet state) {
            int ret = 0;
            if (state != null) {
                ret = stateInterner.resolveObject(state);
                if (ret == 0) {
                    ret = stateInterner.addObject(state);
                    for (BitSet finalSet: finalSets) {
                        if (subset(finalSet, state)) {
                            finalStates.add(ret);
                        }
                    }
                }
            }
            return ret;
        }
    }

    public Set<String> toSet(BitSet bitset) {
        if (bitset == null) {
            return null;
        } else {
            Set<String> ret = new HashSet<>();

            for (int i = bitset.nextSetBit(0); i != -1; i = bitset.nextSetBit(i + 1)) {
                ret.add(universeInterner.resolveId(i + 1)); // add one because interner IDs start at 1
            }

            return ret;
        }
    }

    @Override
    public String representAsString(BitSet object) {
        if (object == null) {
            return "<null>";
        } else {
            return StringTools.join(toSet(object), SEPARATOR);
        }
    }
}
