/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.Lists;
import de.saar.basic.StringTools;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The binary string algebra. The elements of this algebra are lists of strings,
 * which can be thought of as the words in a sentence. The algebra has a single
 * binary operation symbol, *, which evaluates to string concatenation. All
 * other strings are nullary symbols of this algebra; the string w evaluates to
 * the list [w]
 * .<p>
 *
 * Notice that the algebra's signature is made aware of these nullary symbols
 * only when {@link StringAlgebra#parseString(java.lang.String) }
 * sees these symbols. This means that the contents of the signature may change
 * as more string representations are parsed.
 *
 * @author koller
 */
public class StringAlgebra extends Algebra<List<String>> implements Serializable {
    
    public static final String CONCAT = "*";
    
    public static final String SPECIAL_STAR = "__*__";
    
    protected int specialStarId;
    
    protected final int concatSymbolId;
    
    private IntSet concatSet;

    public StringAlgebra() {
        concatSymbolId = signature.addSymbol(CONCAT, 2);

        specialStarId = signature.addSymbol(SPECIAL_STAR, 0);
        
        concatSet = new IntOpenHashSet();
        concatSet.add(concatSymbolId);
    }

    @Override
    protected List<String> evaluate(String label, List<List<String>> childrenValues) {
       switch(label){
            case CONCAT:
                List<String> ret = new ArrayList<>();
                ret.addAll(childrenValues.get(0));
                ret.addAll(childrenValues.get(1));
                return ret;
            case SPECIAL_STAR:
                return Lists.newArrayList("*");
            default:
                return Lists.newArrayList(label);
       }
    }

    @Override
    public List<String> evaluate(Tree<String> t) {
        List<String> ret = new ArrayList<String>();

        t.dfs(new TreeVisitor<String, Void, Void>() {
            @Override
            public Void combine(Tree<String> node, List<Void> childrenValues) {
                
                if(node.getChildren().isEmpty()){ 
                    String label = node.getLabel();
                    
                    switch(label){
                        case SPECIAL_STAR:
                            ret.add("*");
                            return null;
                        default:
                            ret.add(node.getLabel());
                            return null;
                    }
                    
                }
                else{
                    return null;
                }
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

        for (String word : symbols) {
            signature.addSymbol(word, 0);
        }

        return symbols;
    }

//    @Override
//    public JComponent visualize(List<String> object) {
//        return new JLabel(StringTools.join(object, " "));
//    }

    @Override
    public String representAsString(List<String> object) {
        return StringTools.join(object, " ");
    }
    
    

    private class CkyAutomaton extends TreeAutomaton<Span> {

        private int[] words;
        private IntSet allLabels;
        private boolean isBottomUpDeterministic;

        public CkyAutomaton(List<String> words) {
            super(StringAlgebra.this.getSignature());

            allLabels = new IntOpenHashSet();
            allLabels.add(signature.getIdForSymbol(CONCAT));

            this.words = new int[words.size()];
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                int code;
                
                
                switch(word){
                    case "*":
                        code = specialStarId;
                        break;
                    default:
                        code = StringAlgebra.this.getSignature().getIdForSymbol(words.get(i));
                        break;
                }
                
                this.words[i] = code;
                allLabels.add(code);
            }

            finalStates.add(addState(new Span(0, words.size())));

            // automaton becomes nondeterministic if the same word
            // occurs twice in the string
            isBottomUpDeterministic = new HashSet<String>(words).size() == words.size();
        }

        @Override
        public IntSet getAllLabels() {
            return allLabels;
        }

        @Override
        public IntSet getAllStates() {
            IntSet ret = new IntOpenHashSet();

            for (int i = 0; i < words.length; i++) {
                for (int k = i + 1; k <= words.length; k++) {
                    ret.add(addState(new Span(i, k)));
                }
            }

            return ret;
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
            if (useCachedRuleBottomUp(label, childStates)) {
                return getRulesBottomUpFromExplicit(label, childStates);
            } else {
                Set<Rule> ret = new HashSet<Rule>();

                if (label == concatSymbolId) {
                    if (childStates.length != 2) {
                        return new HashSet<Rule>();
                    }

                    if (getStateForId(childStates[0]).end != getStateForId(childStates[1]).start) {
                        return new HashSet<Rule>();
                    }

                    Span span = new Span(getStateForId(childStates[0]).start, getStateForId(childStates[1]).end);
                    int spanState = addState(span);
                    Rule rule = createRule(spanState, label, Arrays.copyOf(childStates, childStates.length), 1);  // contents of childStates may change in the future, clone it to be on the safe side
                    ret.add(rule);
                    storeRule(rule);

                    return ret;
                } else {
                    for (int i = 0; i < words.length; i++) {
                        if (words[i] == label) {
                            ret.add(createRule(addState(new Span(i, i + 1)), label, new int[0], 1));
                        }
                    }

                    return ret;
                }
            }
        }

        @Override
        public Iterable<Rule> getRulesTopDown(int label, int parentState) {
            if (!useCachedRuleTopDown(label, parentState)) {
                Span parentSpan = getStateForId(parentState);

                if (label == concatSymbolId) {
                    for (int i = parentSpan.start + 1; i < parentSpan.end; i++) {
                        int[] childStates = new int[2];
                        childStates[0] = addState(new Span(parentSpan.start, i));
                        childStates[1] = addState(new Span(i, parentSpan.end));
                        Rule rule = createRule(parentState, label, childStates, 1);
                        storeRule(rule);
                    }
                } else if ((parentSpan.length() == 1) && label == words[parentSpan.start]) {
                    Rule rule = createRule(parentState, label, new int[0], 1);
                    storeRule(rule);
                }
            }

            return getRulesTopDownFromExplicit(label, parentState);
        }

        @Override
        public IntIterable getLabelsTopDown(int parentState) {
            Span parentSpan = getStateForId(parentState);

            if (parentSpan.end == parentSpan.start + 1) {
                IntSet ret = new IntOpenHashSet();
                ret.add(words[parentSpan.start]);
                return ret;
            } else {
                return concatSet;
            }
        }

        @Override
        public boolean hasRuleWithPrefix(int label, List<Integer> prefixOfChildren) {
            if (label == concatSymbolId) {
                switch (prefixOfChildren.size()) {
                    case 0:
                    case 1:
                        return true;

                    case 2:
                        return getStateForId(prefixOfChildren.get(0)).end == getStateForId(prefixOfChildren.get(1)).start;

                    default:
                        throw new RuntimeException("checking rule prefix for CONCAT with arity > 2");
                }
            } else {
                for (int i = 0; i < words.length; i++) {
                    if (words[i] == label) {
                        return true;
                    }
                }

                return false;
            }
        }

//        @Override
//        public Set<Integer> getFinalStates() {
//            return finalStates;
//        }
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

    public String getBinaryConcatenation() {
        return CONCAT;
    }
}
