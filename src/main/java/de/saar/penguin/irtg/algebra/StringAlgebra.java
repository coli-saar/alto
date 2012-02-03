/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class StringAlgebra implements Algebra<List<String>> {
    public static final String CONCAT = "*";

    public List<String> evaluate(Tree<String> t) {
        List<String> children = t.getChildren(t.getRoot());

        if (children.isEmpty()) {
            List<String> ret = new ArrayList<String>();
            ret.add(t.getLabel(t.getRoot()));
            return ret;
        } else {
            List<String> childEval = new ArrayList<String>();

            // append yields of all children
            for (String child : children) {
                childEval.addAll(evaluate(t.subtree(child)));
            }

            return childEval;
        }
    }

    public BottomUpAutomaton decompose(List<String> words) {
        return new CkyAutomaton(words);
    }

    public List<String> parseString(String representation) {
        return Arrays.asList(representation.split("\\s+"));
    }

    private static class CkyAutomaton extends BottomUpAutomaton<Span> {
        private List<String> words;
        private Set<String> allLabels;

        public CkyAutomaton(List<String> words) {
            this.words = words;

            finalStates.add(new Span(0, words.size()));

            allLabels = new HashSet<String>();
            allLabels.add(CONCAT);
            allLabels.addAll(words);
        }

        @Override
        public Set<String> getAllLabels() {
            return allLabels;
        }

        @Override
        public Set<Span> getAllStates() {
            Set<Span> ret = new HashSet<Span>();

            for( int i = 0; i < words.size(); i++ ) {
                for( int k = i+1; k <= words.size(); k++ ) {
                    ret.add(new Span(i,k));
                }
            }

            return ret;
        }



        @Override
        public Set<Rule<Span>> getRulesBottomUp(String label, List<Span> childStates) {
            if (useCachedRuleBottomUp(label, childStates)) {
                return getRulesBottomUpFromExplicit(label, childStates);
            } else {
                Set<Rule<Span>> ret = new HashSet<Rule<Span>>();

                if (label.equals(CONCAT)) {
                    if (childStates.size() != 2) {
                        return new HashSet<Rule<Span>>();
                    }

                    if (childStates.get(0).end != childStates.get(1).start) {
                        return new HashSet<Rule<Span>>();
                    }

                    Span span = new Span(childStates.get(0).start, childStates.get(1).end);
                    Rule<Span> rule = new Rule<Span>(span, label, childStates);
                    ret.add(rule);
                    storeRule(rule);

                    return ret;
                } else {
                    for (int i = 0; i < words.size(); i++) {
                        if (words.get(i).equals(label)) {
                            ret.add(new Rule<Span>(new Span(i, i+1), label, new Span[]{}));
                        }
                    }

                    return ret;
                }
            }
        }



        @Override
        public Set<Rule<Span>> getRulesTopDown(String label, Span parentState) {
            if( ! useCachedRuleTopDown(label, parentState)) {
                if( label.equals(CONCAT)) {
                    for( int i = parentState.start + 1; i < parentState.end; i++ ) {
                        List<Span> childStates = new ArrayList<Span>();
                        childStates.add(new Span(parentState.start, i));
                        childStates.add(new Span(i, parentState.end));
                        Rule<Span> rule = new Rule<Span>(parentState, label, childStates);
                        storeRule(rule);
                    }
                }
            }

            return getRulesTopDownFromExplicit(label, parentState);
        }

//        @Override
//        public int getArity(String label) {
//            if( label.equals(CONCAT)) {
//                return 2;
//            } else {
//                return 0;
//            }
//        }

        @Override
        public Set<Span> getFinalStates() {
            return finalStates;
        }
    }

    static class Span implements Serializable {

        public int start, end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return start + "-" + end;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Span other = (Span) obj;
            if (this.start != other.start) {
                return false;
            }
            if (this.end != other.end) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }
    }
}
