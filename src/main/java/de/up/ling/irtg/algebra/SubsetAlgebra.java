/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.Sets;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class SubsetAlgebra<E> extends Algebra<Set<E>> {
    private Set<E> universe;
    public static final String DISJOINT_UNION = "dunion";
    public static final String EMPTYSET = "EMPTYSET";
    public static final String SEPARATOR = "\\s*\\|\\s*";

    // Don't call this directly; it is only needed so
    // an IRTG with this algebra can be constructed by a codec.
    public SubsetAlgebra() {
        this(Collections.emptySet());
    }

    public SubsetAlgebra(Set<E> universe) {
        this.universe = universe;
    }

    @Override
    public void readOptions(Reader optionReader) throws Exception {
        universe = (Set<E>) parseStringSet(StringTools.slurp(optionReader));
    }

    @Override
    protected Set<E> evaluate(String label, List<Set<E>> childrenValues) {
        if (DISJOINT_UNION.equals(label)) {
            assert childrenValues.size() == 2;

            Set<E> s1 = childrenValues.get(0);
            Set<E> s2 = childrenValues.get(1);

            if (s1 == null || s2 == null) {
                return null;
            } else if (Collections.disjoint(s1, s2)) {
                // TODO - measure, on larger inputs, whether this is more or less
                // performant than actually computing the set union and putting
                // it in a new object.
                return Sets.union(s1, s2);
            } else {
                return null;
            }
        } else if (EMPTYSET.equals(label)) {
            assert childrenValues.isEmpty();
            return Collections.emptySet();
        } else {
            assert childrenValues.isEmpty();

            try {
                Set<E> s = parseString(label);

                if (universe.containsAll(s)) {
                    return s;
                } else {
                    Logging.get().severe(() -> "Not a subset of universe: " + label);
                    return null;
                }
            } catch (ParserException ex) {
                Logging.get().severe(() -> "Could not parse set constant: " + label);
                return null;
            }
        }
    }

    @Override
    public Set<E> parseString(String representation) throws ParserException {
        return (Set<E>) parseStringSet(representation);
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
    public TreeAutomaton decompose(Set<E> set) {
        return new DecompositionAuto(set);
    }

    private class DecompositionAuto extends EvaluatingDecompositionAutomaton {
        public DecompositionAuto(Set<E> finalSet) {
            super(finalSet);

            finalStates.clear();

            // Add all subsets of the universe as states.
            // Add all supersets of the finalSet as final states.
            for (Set<E> s : Sets.powerSet(universe)) {
                int q = addState(s);
                if (s.containsAll(finalSet)) {
                    finalStates.add(q);
                }
            }
        }

        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
//            System.err.printf("grbu: ? -> %s%s\n", getSignature().resolveSymbolId(labelId), Util.mapToList(new IntArrayList(childStates), q -> getStateForId(q).toString()));
            Iterable<Rule> result = super.getRulesBottomUp(labelId, childStates);
//            System.err.printf("  -> %s\n", Util.mapToList(result, x -> x.toString(this)));
            return result;
        }
    }

    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, Exception {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(args[0]));

        SubsetAlgebra a = (SubsetAlgebra) irtg.getInterpretation("sem").getAlgebra();
        // pass true facts here
        a.setOptions("rabbit(r1) | sleep(e,r1) | takefrom(e2,r1,h) | white(r1) | hat(h)");

        System.err.println(a.getSignature());

        // pass things we want to say here
        TreeAutomaton decomp = a.decompose(parseStringSet("sleep(e,r1)"));
//        decomp.makeAllRulesExplicit();
        System.err.println(decomp.toStringBottomUp());
    }

}
