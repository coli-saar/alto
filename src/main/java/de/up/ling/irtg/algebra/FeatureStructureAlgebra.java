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
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.Logging;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class FeatureStructureAlgebra extends Algebra<FeatureStructure> implements NullFilterAlgebra {
    public static final String UNIFY = "unify";
    public static final String PROJ = "proj_";
    public static final String EMBED = "emb_";

    private Map<String, FeatureStructure> parsedAtoms = new HashMap<>();

    public FeatureStructureAlgebra() {
        signature.addSymbol(UNIFY, 2);
    }

    @Override
    protected FeatureStructure evaluate(String label, List<FeatureStructure> childrenValues) {
        if (label == null) {
            Logger.getLogger(FeatureStructureAlgebra.class.getName()).log(Level.SEVERE, null, "Cannot evaluate null label");
            return null;
        } else if (UNIFY.equals(label)) {
            assert childrenValues.size() == 2;
            return childrenValues.get(0).unify(childrenValues.get(1));
        } else if (label.startsWith(PROJ)) {
            assert childrenValues.size() == 1;
            
            String attr = withoutPrefix(label, PROJ);
            FeatureStructure arg = childrenValues.get(0);
//            System.err.println(arg.rawToString());
//            System.err.printf("%s -> proj(%s) / of %s / is %s\n", label, attr, arg, arg.get(attr));
            
            return arg.get(attr);
        } else if (label.startsWith(EMBED)) {
            assert childrenValues.size() == 1;
            AvmFeatureStructure ret = new AvmFeatureStructure();
            ret.put(withoutPrefix(label, EMBED), childrenValues.get(0));
            return ret;
        } else {
            assert childrenValues.isEmpty();

            try {
                FeatureStructure cached = parsedAtoms.get(label);

                if (cached == null) {
                    cached = FeatureStructure.parse(label);
                    parsedAtoms.put(label, cached);
                }

                return cached;
            } catch (FsParsingException ex) {
                Logger.getLogger(FeatureStructureAlgebra.class.getName()).log(Level.SEVERE, null, ex);
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
    public TreeAutomaton nullFilter() {
        return new TreeAutomaton<FeatureStructure>(signature) {
            @Override
            public Iterable getRulesBottomUp(int labelId, int[] childStates) {
                String op = getSignature().resolveSymbolId(labelId);

                List<FeatureStructure> children = new ArrayList<>();
                Arrays.stream(childStates).forEach(cs -> children.add(getStateForId(cs)));

                FeatureStructure parent = evaluate(op, children);
                if (parent != null) {
                    int parentState = addState(parent);
                    addFinalState(parentState);

                    Rule rule = createRule(parentState, labelId, childStates, 1);
                    return Collections.singletonList(rule);
                } else {
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

    public static void main(String[] args) throws FileNotFoundException, IOException, ParserException {
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
