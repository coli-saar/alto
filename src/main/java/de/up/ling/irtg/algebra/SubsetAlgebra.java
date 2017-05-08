/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ImmutableMap;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class SubsetAlgebra<E> extends Algebra<BitSet> {
    private Interner<E> universeInterner;
    private static final BitSet EMPTY_BITSET = new BitSet();

//    private Set<E> universe;
    public static final String DISJOINT_UNION = "dunion";
    public static final String EMPTYSET = "EMPTYSET";
    public static final String SEPARATOR = "\\s*\\|\\s*";

    // Don't call this directly; it is only needed so
    // an IRTG with this algebra can be constructed by a codec.
    public SubsetAlgebra() {
        this(Collections.emptySet());
    }

    public SubsetAlgebra(Set<E> universe) {
        setUniverse(universe);
    }

    private void setUniverse(Set<E> universe) {
        universeInterner = new Interner<>();

        for (E element : universe) {
            universeInterner.addObject(element);
        }
    }

    @Override
    public void readOptions(Reader optionReader) throws Exception {
        setUniverse((Set<E>) parseStringSet(StringTools.slurp(optionReader)));
    }

    private static boolean disjoint(BitSet s1, BitSet s2) {
        return !s1.intersects(s2);
    }

    /**
     * Checks if s1 is a subset of s2.
     *
     * @param s1
     * @param s2
     * @return
     */
    private static boolean subset(BitSet s1, BitSet s2) {
        BitSet copy = new BitSet();
        copy.or(s1);
        copy.andNot(s2); // -> copy = s1 & ~s2
        return copy.isEmpty();
    }

    @Override
    protected BitSet evaluate(String label, List<BitSet> childrenValues) {
        if (DISJOINT_UNION.equals(label)) {
            assert childrenValues.size() == 2;

            BitSet s1 = childrenValues.get(0);
            BitSet s2 = childrenValues.get(1);

            if (s1 == null || s2 == null) {
                return null;
            } else if (disjoint(s1, s2)) {
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
                return parseString(label);
            } catch (ParserException ex) {
                Logging.get().severe(() -> "Could not parse set constant: " + label);
                return null;
            }
        }
    }

    public BitSet toBitset(Set<E> s) {
        if (s == null) {
            return null;
        } else {
            BitSet ret = new BitSet();

            for (E element : s) {
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

    @Override
    public BitSet parseString(String representation) throws ParserException {
        Set<E> s = (Set<E>) parseStringSet(representation);
        return toBitset(s);
        // TODO - figure out a way to return actual set of E's
    }

    public static Set<String> parseStringSet(String representation) {
        String s = representation.trim();

        if (s.length() == 0) {
            return Collections.emptySet();
        } else {
            String[] parts = s.split(SEPARATOR);
            return new HashSet<>(Arrays.asList(parts));
        }
    }

    @Override
    public TreeAutomaton decompose(BitSet set) {
        return new DecompositionAuto(set);
    }

    private class DecompositionAuto extends EvaluatingDecompositionAutomaton {
        private final BitSet finalSet;

        public DecompositionAuto(BitSet finalSet) {
            super(SubsetAlgebra.this.getSignature());

            this.finalSet = finalSet;

        }
        
        @Override
        protected int addState(BitSet state) {
            if (state == null) {
                return 0;
            } else {
                int ret = stateInterner.resolveObject(state);

                if (ret == 0) {
                    ret = stateInterner.addObject(state);
                    if (subset(finalSet, state)) {
                        finalStates.add(ret);
                    }
                }

                return ret;
            }
        }
    }

    public Set<E> toSet(BitSet bitset) {
        if (bitset == null) {
            return null;
        } else {
            Set<E> ret = new HashSet<>();

            for (int i = bitset.nextSetBit(0); i != -1; i = bitset.nextSetBit(i + 1)) {
                ret.add(universeInterner.resolveId(i + 1)); // add one because interner IDs start at 1
            }

            return ret;
        }
    }

    @Override
    public String representAsString(BitSet object) {
        return StringTools.join(toSet(object), SEPARATOR);
    }

    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, Exception {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(args[0]));

        SubsetAlgebra sem = (SubsetAlgebra) irtg.getInterpretation("sem").getAlgebra();
        SetAlgebra ref = (SetAlgebra) irtg.getInterpretation("ref").getAlgebra();
        StringAlgebra str = (StringAlgebra) irtg.getInterpretation("string").getAlgebra();

        // put true facts here
        ref.readOptions(new FileReader(args[1]));
        List<String> trueAtoms = Util.mapToList(ref.getModel().getTrueAtoms(), t -> t.toString());
        sem.setOptions(StringTools.join(trueAtoms, " | "));

        // put inputs here
        Object refInput = ref.parseString("{e}");
        Object semInput = sem.parseString("sleep(e,r1) | rabbit(r1)");

        TreeAutomaton<?> chart = null;

        long start = System.nanoTime();
        int N = 1;
        
        for (int i = 0; i < N; i++) {
            chart = irtg.parseInputObjects(ImmutableMap.of("ref", refInput, "sem", semInput));
        }
        
        System.err.println(chart);

        System.err.printf("%dx chart construction: %s\n", N, Util.formatTimeSince(start));

        for (Tree<String> dt : chart.languageIterable()) {
            System.err.printf("\nderivation: %s\n", dt);
            System.err.printf("output: %s\n", str.representAsString((List<String>) irtg.interpret(dt, "string")));
        }
    }

}
