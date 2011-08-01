/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.StringTools;
import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class StringAlgebra implements Algebra<String> {
    public static final String CONCAT = "*";

    public String evaluate(Tree t) {
        List<String> children = t.getChildren(t.getRoot());

        if (children.isEmpty()) {
            return t.getLabel(t.getRoot()).toString();
        } else {
            List<String> childEval = new ArrayList<String>();

            for (String child : children) {
                childEval.add(evaluate(t.subtree(child)));
            }

            return StringTools.join(childEval, " ");
        }
    }

    public BottomUpAutomaton decompose(String value) {
        String[] words = value.split("\\s+");
        return new CkyAutomaton(words);
    }

    private static class CkyAutomaton extends BottomUpAutomaton<Span> {
        private String[] words;
        private Set<String> allLabels;

        public CkyAutomaton(String[] words) {
            this.words = words;

            finalStates.add(new Span(0, words.length));

            allLabels = new HashSet<String>();
            allLabels.add(CONCAT);
            for (int i = 0; i < words.length; i++) {
                allLabels.add(words[i]);
            }
        }

        @Override
        public Set<String> getAllLabels() {
            return allLabels;
        }

        @Override
        public Set<Span> getAllStates() {
            Set<Span> ret = new HashSet<Span>();

            for( int i = 0; i < words.length; i++ ) {
                for( int k = i+1; k <= words.length; k++ ) {
                    ret.add(new Span(i,k));
                }
            }

            return ret;
        }



        @Override
        public Set<Rule<Span>> getRulesBottomUp(String label, List<Span> childStates) {
            if (contains(label, childStates)) {
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
                    for (int i = 0; i < words.length; i++) {
                        if (words[i].equals(label)) {
                            ret.add(new Rule<Span>(new Span(i, i+1), label, new Span[]{}));
                        }
                    }

                    return ret;
                }
            }
        }



        @Override
        public Set<Rule<Span>> getRulesTopDown(String label, Span parentState) {
            if( ! containsTopDown(label, parentState)) {
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

        @Override
        public int getArity(String label) {
            if( label.equals(CONCAT)) {
                return 2;
            } else {
                return 0;
            }
        }

        @Override
        public Set<Span> getFinalStates() {
            return finalStates;
        }
    }

    static class Span {

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
