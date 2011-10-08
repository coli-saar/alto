/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.io.StringReader;
import java.util.ArrayList;
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
    private static final String[] SPECIAL_STRINGS = {PROJECT, INTERSECT, UNIQ};
    private Map<String, Set<List<String>>> atomicInterpretations;

    public SetAlgebra(Map<String, Set<List<String>>> atomicInterpretations) {
        this.atomicInterpretations = atomicInterpretations;
    }

    public Set<List<String>> evaluate(final Tree t) {
        return (Set<List<String>>) t.dfs(new TreeVisitor<Void, Set<List<String>>>() {
            @Override
            public Set<List<String>> combine(String node, List<Set<List<String>>> childrenValues) {
//                System.err.println("evaluate: " + t.subtree(node));
                Set<List<String>> ret = null;
                String label = getLabel(t, node);

                if (label.startsWith(PROJECT)) {
                    Set<List<String>> child = childrenValues.get(0);
                    int pos = Integer.parseInt(arg(label)) - 1;
                    ret = new HashSet<List<String>>();
                    for (List<String> tuple : child) {
                        List<String> l = new ArrayList<String>();
                        l.add(tuple.get(pos));
                        ret.add(l);
                    }
                } else if (label.startsWith(INTERSECT)) {
                    Set<List<String>> tupleSet = childrenValues.get(0);
                    int pos = Integer.parseInt(arg(label)) - 1;
                    Set<List<String>> filterSet = childrenValues.get(1);
                    Set<String> filter = new HashSet<String>();
                    ret = new HashSet<List<String>>();

                    for (List<String> f : filterSet) {
                        filter.add(f.get(0));
                    }

                    System.err.println("filter set: " + filter);

                    for (List<String> tuple : tupleSet) {
                        if (filter.contains(tuple.get(pos))) {
                            ret.add(tuple);
                        } 
                    }
                } else if (label.startsWith(UNIQ)) {
                    String arg = arg(label);
                    List<String> uniqArg = new ArrayList<String>();
                    Set<List<String>> child = childrenValues.get(0);

                    uniqArg.add(arg);
                    
                    if (child.size() == 1 && child.iterator().next().equals(uniqArg)) {
                        ret = child;
                    } else {
                        ret = new HashSet<List<String>>();
                    }
                } else {
                    ret = atomicInterpretations.get(label);
                }

//                System.err.println("  -> " + ret);
                return ret;
            }
        });


    }

    private static String arg(String stringWithArg) {
        for (String s : SPECIAL_STRINGS) {
            if (stringWithArg.startsWith(s)) {
                return stringWithArg.substring(s.length());
            }
        }

        return null;
    }

    private String getLabel(Tree t, String node) {
        return t.getLabel(node).toString();
    }

    public BottomUpAutomaton decompose(Set<List<String>> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Set<List<String>> parseString(String representation) throws ParseException {
        return SetParser.parse(new StringReader(representation));
    }

    private static List<String> l(String x) {
        List<String> ret = new ArrayList<String>();
        ret.add(x);
        return ret;
    }
}
