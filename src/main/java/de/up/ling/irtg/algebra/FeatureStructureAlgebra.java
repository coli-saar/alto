/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ImmutableMap;
import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.FeatureStructure;
import de.saar.coli.featstruct.FsParsingException;
import de.saar.coli.featstruct.JFeatureStructurePanel;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.Logging;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 *
 * @author koller
 */
public class FeatureStructureAlgebra extends Algebra<FeatureStructure> implements NullFilterAlgebra {

    public static final String UNIFY = "unify";
    public static final String PROJ = "proj_";
    public static final String EMBED = "emb_";
    public static final String EMBED_AUX = "emba_";

    private Map<String, FeatureStructure> parsedAtoms = new HashMap<>();

    public FeatureStructureAlgebra() {
        signature.addSymbol(UNIFY, 2);
    }

    @Override
    protected FeatureStructure evaluate(String label, List<FeatureStructure> childrenValues) {
        // if any of the children are null, just return null
        if (childrenValues.stream().anyMatch(fs -> fs == null)) {
            return null;
        }


        if (label == null) {
            Logger.getLogger(FeatureStructureAlgebra.class.getName()).log(Level.SEVERE, null, "Cannot evaluate null label");
//            System.err.println("evaluate -> null"); // AKAKAK
            return null;
        } else if (UNIFY.equals(label)) {
            assert childrenValues.size() == 2;
            FeatureStructure ret = childrenValues.get(0).unify(childrenValues.get(1));
//            System.err.println("evaluate1 -> " + ret.rawToString()); // AKAKAK
            return ret;
        } else if (label.startsWith(PROJ)) {
            assert childrenValues.size() == 1;
            String attr = withoutPrefix(label, PROJ);
            FeatureStructure arg = childrenValues.get(0);
//            System.err.println("evaluate2 -> " + arg.get(attr).rawToString()); // AKAKAK
            return arg.get(attr);
        } else if (label.startsWith(EMBED)) {
            assert childrenValues.size() == 1;
            AvmFeatureStructure ret = new AvmFeatureStructure();
            ret.put(withoutPrefix(label, EMBED), childrenValues.get(0));
//            System.err.println("evaluate3 -> " + ret.rawToString()); // AKAKAK
            return ret;
        } else if (label.startsWith(EMBED_AUX)) {
            assert childrenValues.size() == 1;
            String[] parts = label.split("_");
            AvmFeatureStructure ret = new AvmFeatureStructure();
            ret.put(parts[1], childrenValues.get(0).get("root"));
            ret.put(parts[2], childrenValues.get(0).get("foot"));
//            System.err.println("evaluate4 -> " + ret.rawToString()); // AKAKAK
            return ret;
        } else {
            assert childrenValues.isEmpty();

            try {
                // Always return the same FS object for the same literal.
                FeatureStructure cached = parsedAtoms.get(label);

                if (cached == null) {
                    cached = FeatureStructure.parse(label);
                    parsedAtoms.put(label, cached);
                }

//                System.err.println("evaluate5 -> " + cached.rawToString()); // AKAKAK
                return cached;
            } catch (FsParsingException ex) {
                Logger.getLogger(FeatureStructureAlgebra.class.getName()).log(Level.SEVERE, null, ex);
//                System.err.println("evaluate -> null"); // AKAKAK
                return null;
            }
        }
    }

    private static String withoutPrefix(String s, String prefix) {
        return s.substring(prefix.length());
    }

    @Override
    public FeatureStructure parseString(String representation) throws ParserException {
        try {
            return FeatureStructure.parse(representation);
        } catch (FsParsingException ex) {
            throw new ParserException(ex);
        }
    }

    @Override
    public JComponent visualize(FeatureStructure object) {
        if (object == null) {
            return new JLabel("<null>");
        } else {
            return new JFeatureStructurePanel(object);
        }
    }

    /**
     * A tree automaton which accepts all terms over this algebra which do not evaluate
     * to null. Intersect this automaton into a parse chart to filter out derivations with
     * unification failures.<p>
     *
     * The states of this automaton are feature structures, modulo equality; that is, two
     * feature structures which are equal as per {@link FeatureStructure#equals(Object)} are
     * conflated into the same state. This suppresses re-derivations of the same feature
     * structure for the same substring.<p>
     *
     * It is guaranteed that no two
     * of these feature structures have nodes in common. Node sharing between different
     * FSes in the same chart might improve efficiency, but it is really dangerous
     * because the unification algorithm in {@link AvmFeatureStructure#unify(FeatureStructure)}
     * destructively modifies feature structures. In particular, if we first parse the FS
     * #1 [ft: #2 []], there will be a state #2 []. If we subsequently encounter the state
     * #3 [], it will be replaced by #2 [] because of the equality caching described above.
     * Unifying #1 [ft: #2] with #2 [] will fail because it introduced a cycle. This is the
     * cause behind issue #46.
     *
     */
    @Override
    public TreeAutomaton nullFilter() {
        return new TreeAutomaton<FeatureStructure>(signature) {
            @Override
            public Iterable getRulesBottomUp(int labelId, int[] childStates) {
                String op = getSignature().resolveSymbolId(labelId);

                List<FeatureStructure> children = new ArrayList<>();
                Arrays.stream(childStates).forEach(cs -> children.add(getStateForId(cs)));

                FeatureStructure parent = evaluate(op, children);
                if (parent != null) {
                    parent = parent.deepCopy(); // deepCopy -> ensure no nodes in common, see #46
                    int parentState = addState(parent);
                    addFinalState(parentState);

                    Rule rule = createRule(parentState, labelId, childStates, 1);
                    return Collections.singletonList(rule);
                } else {
//                    System.err.printf("unif failure: %s\n  with %s\n\n", children.get(0), children.get(1));
                    return Collections.EMPTY_LIST;
                }
            }

            @Override
            public boolean supportsTopDownQueries() {
                return false;
            }

            @Override
            public Iterable getRulesTopDown(int labelId, int parentState) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isBottomUpDeterministic() {
                return true;
            }
        };
    }

    public static void main(String[] args) throws IOException, ParserException {
        Logging.get().setLevel(Level.INFO);

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/fcfg.irtg"));
        FeatureStructureAlgebra fsa = (FeatureStructureAlgebra) irtg.getInterpretation("ft").getAlgebra();

        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        sw.record();

        TreeAutomaton<?> chart = irtg.parse(ImmutableMap.of("string", "john sleeps"));
        sw.record();

        System.err.println(chart);

        TreeAutomaton<?> filtered = chart.intersect(fsa.nullFilter().inverseHomomorphism(irtg.getInterpretation("ft").getHomomorphism()));
        sw.record();

        sw.printMilliseconds("chart", "filtering");

        System.err.println(filtered);
        System.err.println(filtered.language());
    }
}
