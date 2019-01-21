/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A string algebra for encoding Regular Dependency Grammars (RDGs). RDG is a
 * mildly context-sensitive grammar formalism developed by Marco Kuhlmann in his
 * PhD thesis, and is equivalent to LCFRS.
 * <p>
 *
 * The operations of this algebra are of the form "word:oa", where "word" is a
 * word, and "oa" is an order annotation. Order annotations are strings
 * consisting of the digits 0 (indicating the position of the word itself), 1-9
 * (indicating the first to ninth child, respectively), and commas (indicating a
 * "gap" between two contiguous substrings). Each order annotation combines the
 * string tuples of its children into a larger string tuple, with as many
 * entries as the <i>block degree</i>
 * of the order annotation, i.e. the number of commas plus one. If the string
 * tuple from the k-th child has block degree b, then the digit k is to be used
 * exactly b times in the order annotation.<p>
 *
 * Unlike in Kuhlmann's work, we explicitly allow for the comma to be the first
 * or last character. This indicates a string tuple which has the empty string
 * as its first or last element, respectively. For instance, the order
 * annotation "word:0," will evaluate to the string pair (word,epsilon), where
 * epsilon is the empty string. This can be useful in modeling TAG auxiliary
 * trees where the foot note is the leftmost or rightmost leaf. It is still
 * illegal, however, to have two commas that follow each other directly.<p>
 *
 * The decomposition automaton for this algebra supports bottom-up queries, but
 * no top-down queries. Its states are tuples of spans in the string, and the
 * bottom-up rule queries simply apply the order annotation in the rule to these
 * spans. This yields a parsing algorithm that is exponential in the block
 * degree and exponential in the maximum rule rank, as in Kuhlmann's thesis.
 * There is no special treatment for well-nested order annotations, which in
 * principle support faster parsing.
 *
 * @author koller
 */
public class RdgStringAlgebra extends Algebra<List<List<String>>> {

    public static final Pattern OA_PATTERN = Pattern.compile("\\s*([^ \\t\\n:]+)\\s*:\\s*([0-9,]+)\\s*");
    private static final boolean DEBUG = false;

    public static final String COMMA = ",";
    private static final String COMMA_COMMA = COMMA + COMMA;

    private static final Logger logger = Logger.getLogger(RdgStringAlgebra.class.getName());

    @Override
    protected List<List<String>> evaluate(String label, List<List<List<String>>> childrenValues) {
        Matcher m = OA_PATTERN.matcher(label);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid order annotation: " + label);
        }

        String word = m.group(1);
        String orderAnnotation = m.group(2);
        int[] posInOA = new int[10]; // initially, all zero

        if (orderAnnotation.contains(COMMA_COMMA)) {
            throw new IllegalArgumentException("Order annotation may not have two subsequent commas: '" + orderAnnotation + "'");
        }

        List<List<String>> ret = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();

        for (int i = 0; i < orderAnnotation.length(); i++) {
            char o = orderAnnotation.charAt(i);

            if (o == COMMA.charAt(0)) {
                ret.add(currentBlock);
                currentBlock = new ArrayList<>();
            } else if (o == '0') {
                currentBlock.add(word);
            } else {
                int child = o - '1';                 // index of child from which next substring comes
                int posInChild = posInOA[child]++; // block in this child from which next substring comes

                if (child >= childrenValues.size() || posInChild >= childrenValues.get(child).size()) {
                    return null; // OA does not fit child values
                } else {
                    List<String> childBlock = childrenValues.get(child).get(posInChild); // words in child block
                    currentBlock.addAll(childBlock);
                }
            }
        }

        ret.add(currentBlock);

        return ret;
    }

    @Override
    public List<List<String>> parseString(String representation) {
        final List<String> words = Arrays.asList(representation.split("\\s+"));
        return Collections.singletonList(words);
    }

    @Override
    public String representAsString(List<List<String>> object) {
        List<String> parts = new ArrayList<>();

        for (List<String> block : object) {
            Iterable<String> nonemptyParts = Iterables.filter(block, x -> !"".equals(x));
            parts.add(StringTools.join(Util.mapToList(nonemptyParts, x -> x), " "));
        }

        return StringTools.join(parts, " _ ");
    }

    @Override
    public TreeAutomaton decompose(List<List<String>> value) {
        return new RdgDecompositionAutomaton(value);
    }

    private class RdgDecompositionAutomaton extends TreeAutomaton<List<Pair<Integer, Integer>>> {

        private int n;
        private boolean isBottomUpDeterministic;
        private ListMultimap<String, Integer> positionsOfWordInString;

        public RdgDecompositionAutomaton(List<List<String>> value) {
            super(RdgStringAlgebra.this.getSignature());

            if (value.size() != 1) {
                throw new IllegalArgumentException("String to be parsed must have block degree 1");
            }

            List<String> w = value.get(0);
            n = w.size();

            int fin = addState(Collections.singletonList(new Pair(0, n)));
            finalStates.add(fin);

            // automaton becomes nondeterministic if the same word
            // occurs twice in the string
            isBottomUpDeterministic = new HashSet<>(value.get(0)).size() == n;

            // find word positions
            Set<String> knownWords = collectKnownWords();
            positionsOfWordInString = ArrayListMultimap.create();
            for (int pos = 0; pos < n; pos++) {
                String word = w.get(pos);
                positionsOfWordInString.put(word, pos);

                if (!knownWords.contains(word)) {
                    logger.warning("Unknown word: '" + word + "'");
                }
            }
        }

        private Set<String> collectKnownWords() {
            Set<String> ret = new HashSet<>();

            for (String label : getSignature().getSymbols()) {
                Matcher m = OA_PATTERN.matcher(label);

                if (!m.matches()) {
                    assert false;
                } else {
                    ret.add(m.group(1));
                }
            }

            return ret;
        }

        @Override
        public Iterable getRulesBottomUp(int labelId, int[] childStates) {
            if (useCachedRuleBottomUp(labelId, childStates)) {
                return getRulesBottomUpFromExplicit(labelId, childStates);
            } else {
                // obtain and parse label
                String label = getSignature().resolveSymbolId(labelId);
                Matcher m = OA_PATTERN.matcher(label);

                if (!m.matches()) {
                    return Collections.EMPTY_LIST;
                }

                String word = m.group(1);
                String orderAnnotation = m.group(2);
                int[] posInOA = new int[10]; // initially, all zero

                // obtain child state list
                List<Pair<Integer, Integer>>[] children = new List[childStates.length];
                for (int i = 0; i < childStates.length; i++) {
                    children[i] = getStateForId(childStates[i]);
                }

                // look up positions of word
                if (!positionsOfWordInString.containsKey(word)) {
                    return Collections.EMPTY_LIST;
                }

                List<Integer> wordPositions = positionsOfWordInString.get(word);
                List<Rule> ret = new ArrayList<>();

                if (DEBUG) {
                    System.err.printf("grbu %s : %s %s\n", word, orderAnnotation, Arrays.toString(children));
                    System.err.printf(" word pos: %s\n", wordPositions);
                }

                WORD_POSITION_LOOP:
                for (int wordPosition : wordPositions) {
                    List<Pair<Integer, Integer>> parent = new ArrayList<>();
                    Pair<Integer, Integer> currentBlock = new Pair(-1, -1);

                    ORDER_ANNOTATION_LOOP:
                    for (int i = 0; i < orderAnnotation.length(); i++) {
                        char o = orderAnnotation.charAt(i);

                        if (o == COMMA.charAt(0)) {
                            parent.add(currentBlock);
                            currentBlock = new Pair(-1, -1);
                        } else if (o == '0') {
                            if (currentBlock.left == -1) {
                                currentBlock.left = wordPosition;
                            } else if (currentBlock.right != wordPosition) {
                                if (DEBUG) {
                                    System.err.printf("   SKIP@%d: cb.left=%d, wordpos=%d\n", i, currentBlock.left, wordPosition);
                                }
                                continue WORD_POSITION_LOOP;
                            }

                            currentBlock.right = wordPosition + 1;
                        } else {
                            int childPos = o - '1';                 // index of child from which next substring comes
                            int posInChild = posInOA[childPos]++; // block in this child from which next substring comes

                            if (childPos >= children.length || posInChild >= children[childPos].size()) {
                                if (DEBUG) {
                                    System.err.printf("   SKIP@%d: child size exceeded\n");
                                }
                                continue WORD_POSITION_LOOP; // OA does not fit child values
                            } else {
                                Pair<Integer, Integer> childBlock = children[childPos].get(posInChild);
                                if (currentBlock.left == -1) {
                                    currentBlock.left = childBlock.left;
                                } else if (childBlock.left == -1) {
                                    assert childBlock.right == -1;

                                    // child block was [-1,-1] => denotes epsilon at arbitrary position, just skip it
                                    continue ORDER_ANNOTATION_LOOP;
                                } else if (currentBlock.right.intValue() != childBlock.left.intValue()) {
                                    assert childBlock.left != -1;
                                    assert childBlock.right != -1;
                                    if (DEBUG) {
                                        System.err.printf("   SKIP@%d: current.left=%d, child.left=%d\n", i, currentBlock.left, childBlock.left);
                                    }
                                    continue WORD_POSITION_LOOP;
                                }

                                currentBlock.right = childBlock.right;
                            }
                        }
                    }

                    parent.add(currentBlock);

                    // managed to combine all the children according to the OA => make rule
                    int parentId = addState(parent);
                    Rule rule = createRule(parentId, labelId, childStates, 1);
                    ret.add(rule);
                    storeRuleBottomUp(rule);

                    if (DEBUG) {
                        System.err.printf(" -> %s\n", rule.toString(this));
                    }
                }

                return ret;
            }
        }

        @Override
        public Iterable getRulesTopDown(int labelId, int parentState) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean supportsTopDownQueries() {
            return false;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return isBottomUpDeterministic;
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
