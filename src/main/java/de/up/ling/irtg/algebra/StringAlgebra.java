/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * The signature of the string algebra consists of a binary concatenation
 * operation. It also contains a nullary constant for each word that has ever
 * been read using the parseString method of this particular StringAlgebra
 * object. Notice that the contents of the Signature object may change if
 * further strings are parsed with the same algebra object.
 *
 * @author koller
 */
public class StringAlgebra implements Algebra<List<String>> {
    public static final String CONCAT = "*";
    private int concatSymbolId;
    private Set<Integer> concatSet;
    private Signature signature = new Signature();

    public StringAlgebra() {
        concatSymbolId = signature.addSymbol(CONCAT, 2);
        
        concatSet = new HashSet<Integer>();
        concatSet.add(concatSymbolId);
    }
    
    public int getConcatSymbolId() {
        return concatSymbolId;
    }

    @Override
    public List<String> evaluate(Tree<Integer> t) {
        final List<String> ret = new ArrayList<String>();

        t.dfs(new TreeVisitor<Integer, Void, Void>() {
            @Override
            public Void combine(Tree<Integer> node, List<Void> childrenValues) {
                if (childrenValues.isEmpty()) {
                    ret.add(signature.resolveSymbolId(node.getLabel()));
                }

                return null;
            }
        });

        return ret;
    }

    @Override
    public TreeAutomaton decompose(List<String> words) {
        return new CkyAutomaton(words);
    }

    @Override
    public List<String> parseString(String representation) {
        final List<String> symbols = Arrays.asList(representation.split("\\s+"));
        
        for( String word : symbols ) {
            signature.addSymbol(word, 0);
        }

        return symbols;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    private class CkyAutomaton extends TreeAutomaton<Span> {
        private int[] words;
        private Set<String> allLabels;
        private boolean isBottomUpDeterministic;

        public CkyAutomaton(List<String> words) {
            super(StringAlgebra.this.getSignature());
            
            this.words = new int[words.size()];
            for( int i = 0; i < words.size(); i++ ) {
                this.words[i] = StringAlgebra.this.getSignature().getIdForSymbol(words.get(i));
            }

            finalStates.add(new Span(0, words.size()));

            allLabels = new HashSet<String>();
            allLabels.add(CONCAT);
            allLabels.addAll(words);

            // automaton becomes nondeterministic if the same word
            // occurs twice in the string
            isBottomUpDeterministic = new HashSet<String>(words).size() == words.size();
        }

        @Override
        public Set<Span> getAllStates() {
            Set<Span> ret = new HashSet<Span>();

            for (int i = 0; i < words.length; i++) {
                for (int k = i + 1; k <= words.length; k++) {
                    ret.add(new Span(i, k));
                }
            }

            return ret;
        }

        @Override
        public Set<Rule<Span>> getRulesBottomUp(int label, List<Span> childStates) {
            if (useCachedRuleBottomUp(label, childStates)) {
                return getRulesBottomUpFromExplicit(label, childStates);
            } else {
                Set<Rule<Span>> ret = new HashSet<Rule<Span>>();

                if (label == concatSymbolId) {
                    if (childStates.size() != 2) {
                        return new HashSet<Rule<Span>>();
                    }

                    if (childStates.get(0).end != childStates.get(1).start) {
                        return new HashSet<Rule<Span>>();
                    }

                    Span span = new Span(childStates.get(0).start, childStates.get(1).end);
                    Rule<Span> rule = createRule(span, label, childStates);
                    ret.add(rule);
                    storeRule(rule);

                    return ret;
                } else {
                    for (int i = 0; i < words.length; i++) {
                        if (words[i] == label) {
                            ret.add(createRule(new Span(i, i + 1), label, new Span[]{}, 1));
                        }
                    }

                    return ret;
                }
            }
        }

        @Override
        public Set<Rule<Span>> getRulesTopDown(int label, Span parentState) {
            if (!useCachedRuleTopDown(label, parentState)) {
                if (label == concatSymbolId) {
                    for (int i = parentState.start + 1; i < parentState.end; i++) {
                        List<Span> childStates = new ArrayList<Span>();
                        childStates.add(new Span(parentState.start, i));
                        childStates.add(new Span(i, parentState.end));
                        Rule<Span> rule = createRule(parentState, label, childStates);
                        storeRule(rule);
                    }
                } else if ((parentState.length() == 1) && label == words[parentState.start]) {
                    Rule<Span> rule = createRule(parentState, label, new ArrayList<Span>());
                    storeRule(rule);
                }
            }

            return getRulesTopDownFromExplicit(label, parentState);
        }

        @Override
        public Set<Integer> getLabelsTopDown(Span parentState) {
            if (parentState.end == parentState.start + 1) {
                Set<Integer> ret = new HashSet<Integer>();
                ret.add(words[parentState.start]);
                return ret;
            } else {
                return concatSet;
            }
        }

        @Override
        public boolean hasRuleWithPrefix(int label, List<Span> prefixOfChildren) {
            if (label == concatSymbolId) {
                switch (prefixOfChildren.size()) {
                    case 0:
                    case 1:
                        return true;

                    case 2:
                        return prefixOfChildren.get(0).end == prefixOfChildren.get(1).start;

                    default:
                        throw new RuntimeException("checking rule prefix for CONCAT with arity > 2");
                }
            } else {
                for( int i = 0; i < words.length; i++ ) {
                    if( words[i] == label ) {
                        return true;
                    }
                }
                
                return false;
            }
        }

        @Override
        public Set<Span> getFinalStates() {
            return finalStates;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return isBottomUpDeterministic;
        }
    }

    public static class Span implements Serializable {
        public int start, end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int length() {
            return end - start;
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
            int hash = 3;
            hash = 23 * hash + this.start;
            hash = 23 * hash + this.end;
            return hash;
        }
    }
}
