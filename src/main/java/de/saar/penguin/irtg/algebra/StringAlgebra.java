/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.StringTools;
import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
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
        public Set<Span> getParentStates(String label, List<Span> childStates) {
            if (contains(label, childStates)) {
                return getParentStatesFromExplicitRules(label, childStates);
            } else {
                Set<Span> ret = new HashSet<Span>();

                if (label.equals(CONCAT)) {
                    if (childStates.size() != 2) {
                        return new HashSet<Span>();
                    }

                    if (childStates.get(0).end != childStates.get(1).start) {
                        return new HashSet<Span>();
                    }

                    Span span = new Span(childStates.get(0).start, childStates.get(1).end);
                    ret.add(span);
                    storeRule(label, childStates, span);

                    return ret;
                } else {
                    for (int i = 0; i < words.length; i++) {
                        if (words[i].equals(label)) {
                            ret.add(new Span(i, i + 1));
                        }
                    }

                    return ret;
                }
            }
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
