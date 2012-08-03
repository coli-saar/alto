/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class SetAlgebra implements Algebra<Set<List<String>>> {
    private static final String PROJECT = "project_";
    private static final String INTERSECT = "intersect_";
    private static final String UNIQ = "uniq_";
    private static final String TOP = "T";
    private static final String[] SPECIAL_STRINGS = {PROJECT, INTERSECT, UNIQ};
    private static final int MAX_TUPLE_LENGTH = 3;
    private final Map<String, Set<List<String>>> atomicInterpretations;
    private final Set<String> allIndividuals;
    private final Set<String> allLabels;
    private final Set<List<String>> allIndividualsAsTuples;

    public SetAlgebra() {
        this(new HashMap<String, Set<List<String>>>());
    }

    public SetAlgebra(Map<String, Set<List<String>>> atomicInterpretations) {
        this.atomicInterpretations = atomicInterpretations;

        allIndividuals = new HashSet<String>();
        allIndividualsAsTuples = new HashSet<List<String>>();
        for (Set<List<String>> sls : atomicInterpretations.values()) {
            for (List<String> ls : sls) {
                allIndividuals.addAll(ls);

                for (String x : ls) {
                    List<String> tuple = new ArrayList<String>();
                    tuple.add(x);
                    allIndividualsAsTuples.add(tuple);
                }
            }
        }

        allLabels = new HashSet<String>();
        allLabels.addAll(atomicInterpretations.keySet());
        for (int i = 1; i <= MAX_TUPLE_LENGTH; i++) {
            allLabels.add(PROJECT + i);
            allLabels.add(INTERSECT + i);
        }
        for (String individual : allIndividuals) {
            allLabels.add(UNIQ + individual);
        }
    }

    @Override
    public Set<List<String>> evaluate(final Tree<String> t) {
        return (Set<List<String>>) t.dfs(new TreeVisitor<String, Void, Set<List<String>>>() {
            @Override
            public Set<List<String>> combine(Tree<String> node, List<Set<List<String>>> childrenValues) {
                return evaluate(node.getLabel(), childrenValues);
            }
        });
    }

    private Set<List<String>> evaluate(String label, List<Set<List<String>>> childrenValues) {
        if (label.startsWith(PROJECT)) {
            return project(childrenValues.get(0), Integer.parseInt(arg(label)) - 1);
        } else if (label.startsWith(INTERSECT)) {
            return intersect(childrenValues.get(0), childrenValues.get(1), Integer.parseInt(arg(label)) - 1);
        } else if (label.startsWith(UNIQ)) {
            return uniq(childrenValues.get(0), arg(label));
        } else if (label.equals(TOP)) {
            return allIndividualsAsTuples;
        } else {
            return atomicInterpretations.get(label);
        }
    }

    private Set<List<String>> project(Set<List<String>> tupleSet, int pos) {
        Set<List<String>> ret = new HashSet<List<String>>();
        for (List<String> tuple : tupleSet) {
            List<String> l = new ArrayList<String>();
            l.add(tuple.get(pos));
            ret.add(l);
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
            if (filter.contains(tuple.get(pos))) {
                ret.add(tuple);
            }
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

//    private String getLabel(Tree t, String node) {
//        return t.getLabel(node).toString();
//    }

    @Override
    public TreeAutomaton decompose(Set<List<String>> value) {
        return new SetDecompositionAutomaton(value);
    }

    @Override
    public Signature getSignature() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class SetDecompositionAutomaton extends TreeAutomaton<Set<List<String>>> {
        public SetDecompositionAutomaton(Set<List<String>> finalElement) {
            super(SetAlgebra.this.getSignature());
            finalStates = new HashSet<Set<List<String>>>();
            finalStates.add(finalElement);
            
            // this is theoretically correct, but WAY too slow,
            // and anyway the Guava powerset function only allows
            // up to 30 elements in the base set
            
//            for( int arity = 1; arity <= MAX_TUPLE_LENGTH; arity++ ) {
//                List<Set<String>> tupleLists = new ArrayList<Set<String>>();
//                for( int i = 0; i < arity; i++ ) {
//                    tupleLists.add(allIndividuals);
//                }
//                
//                CartesianIterator<String> it = new CartesianIterator<String>(tupleLists);
//                Set<List<String>> tuples = new HashSet<List<String>>();
//                while( it.hasNext() ) {
//                    tuples.add(it.next());
//                }
//                
//                Set<Set<List<String>>> powerset = Sets.powerSet(tuples);
//                allStates.addAll(powerset);
//            }
        }

        @Override
        public Set<Rule<Set<List<String>>>> getRulesBottomUp(String label, List<Set<List<String>>> childStates) {
            if (useCachedRuleBottomUp(label, childStates)) {
                return getRulesBottomUpFromExplicit(label, childStates);
            } else {
                Set<Rule<Set<List<String>>>> ret = new HashSet<Rule<Set<List<String>>>>();
                Set<List<String>> parents = evaluate(label, childStates);
                
                // require that set in parent state must be non-empty; otherwise there is simply no rule
                if (parents != null && !parents.isEmpty()) {
                    Rule<Set<List<String>>> rule = new Rule<Set<List<String>>>(parents, label, childStates);
                    ret.add(rule);
                    storeRule(rule);
                }

                return ret;
            }
        }

        @Override
        public Set<Rule<Set<List<String>>>> getRulesTopDown(String label, Set<List<String>> parentState) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

//        @Override
//        public int getArity(String label) {
//            if (label.startsWith(UNIQ)) {
//                return 1;
//            } else if (label.startsWith(INTERSECT)) {
//                return 2;
//            } else if (label.startsWith(PROJECT)) {
//                return 1;
//            } else {
//                return 0;
//            }
//        }


        @Override
        public Set<Set<List<String>>> getFinalStates() {
            return finalStates;
        }

        @Override
        // ultimately, only needed for EM training -- and can we get rid of it there?
        // (and for correct computation of 
        public Set<Set<List<String>>> getAllStates() {
            return new HashSet<Set<List<String>>>();
        }
    }

    @Override
    public Set<List<String>> parseString(String representation) throws ParserException {
        try {
            return SetParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
        
    }

    private static List<String> l(String x) {
        List<String> ret = new ArrayList<String>();
        ret.add(x);
        return ret;
    }
}
