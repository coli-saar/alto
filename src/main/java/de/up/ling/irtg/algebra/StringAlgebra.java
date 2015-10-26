/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.Lists;
import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.condensed.ConcreteCondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Evaluator;
import de.up.ling.irtg.util.LambdaStopwatch;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileInputStream;
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

    /**
     * A special symbol in the algebra that represents a terminal "*" which we
     * cannot represent directly, it is interpreted as [*].
     */
    public static final String SPECIAL_STAR = "__*__";

    /**
     * The number that is used to represent the special star symbol.
     */
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
        switch (label) {
            case CONCAT: // combine children
                List<String> ret = new ArrayList<>();
                ret.addAll(childrenValues.get(0));
                ret.addAll(childrenValues.get(1));
                return ret;
            case SPECIAL_STAR: // turn this into a "*"
                return Lists.newArrayList("*");
            default: //interpret as itself
                return Lists.newArrayList(label);
        }
    }

    @Override
    public List<String> evaluate(Tree<String> t) {
        List<String> ret = new ArrayList<>();

        t.dfs(new TreeVisitor<String, Void, Void>() {
            @Override
            public Void combine(Tree<String> node, List<Void> childrenValues) {

                if (node.getChildren().isEmpty()) {
                    String label = node.getLabel();

                    switch (label) {
                        case SPECIAL_STAR: // we need to turn this into a "*"
                            ret.add("*");
                            return null;
                        default:
                            ret.add(node.getLabel());
                            return null;
                    }

                } else {
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
    public List<Evaluator> getEvaluationMethods() {
        List<Evaluator> ret = new ArrayList<>();
        ret.add(new Evaluator<List<String>>("Equals") {
            
            @Override
            public Pair<Double, Double> evaluate(List<String> result, List<String> gold) {
                double score = (result.equals(gold)) ? 1 : 0;
                return new Pair(score, 1);
            }
        });
        return ret;
    }

    
    
    @Override
    public Class getClassOfValues() {
        return List.class;
    }

    @Override
    public List<String> parseString(String representation) {
        final List<String> symbols = Arrays.asList(representation.split("\\s+"));

        for (String word : symbols) {
            // here we check whether we need to tranform the word because it is "*"
            if (!CONCAT.equals(word)) {
                signature.addSymbol(word, 0);
            }
        }

        return symbols;
    }

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

                switch (word) {// we write the words into the array, if there is a "*" we turn it into specialStarId
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
                Set<Rule> ret = new HashSet<>();

                if (label == concatSymbolId) {
                    if (childStates.length != 2) {
                        return new HashSet<>();
                    }

                    if (getStateForId(childStates[0]).end != getStateForId(childStates[1]).start) {
                        return new HashSet<>();
                    }

                    Span span = new Span(getStateForId(childStates[0]).start, getStateForId(childStates[1]).end);
                    int spanState = addState(span);
                    Rule rule = createRule(spanState, label, Arrays.copyOf(childStates, childStates.length), 1);  // contents of childStates may change in the future, clone it to be on the safe side
                    ret.add(rule);
                    storeRuleBottomUp(rule);

                    return ret;
                } else {
                    if (childStates.length > 0) {
                        return new HashSet<>();
                    }

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
                        storeRuleTopDown(rule);
                    }
                } else if ((parentSpan.length() == 1) && label == words[parentSpan.start]) {
                    Rule rule = createRule(parentState, label, new int[0], 1);
                    storeRuleTopDown(rule);
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

    private static class InvhomDecompFactory {

        private static final int UNDEF = -1;
        private IntSet binaryLabelSetIds = new IntOpenHashSet();
        private IntSet unaryLabelSetIds = new IntOpenHashSet();
        private Object2IntMap<IntList> wordIdsToLabelSetIds;
//        private Int2IntMap wordIdToLabelSetId;
        private Homomorphism hom;

        public InvhomDecompFactory(Homomorphism hom) {
            this.hom = hom;

            Signature srcSignature = hom.getSourceSignature();
            wordIdsToLabelSetIds = new Object2IntOpenHashMap<>();
            wordIdsToLabelSetIds.defaultReturnValue(UNDEF);

            for (int i = 1; i <= hom.getMaxLabelSetID(); i++) {
                IntSet labelSet = hom.getLabelSetByLabelSetID(i);
                int someLabel = labelSet.intIterator().nextInt();
                int arity = srcSignature.getArity(someLabel);

                switch (arity) {
                    case 2:
                        binaryLabelSetIds.add(i);
                        break;

                    case 1:
                        unaryLabelSetIds.add(i);
                        break;

                    default:
                        Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(i);
                        List<HomomorphismSymbol> leafLabels = rhs.getLeafLabels();
                        IntList words = Util.mapToIntList(leafLabels, ll -> ll.getValue());

                        wordIdsToLabelSetIds.put(words, i);

//                        assert rhs != null;
//                        assert rhs.getChildren().isEmpty();
//                        wordIdToLabelSetId.put(rhs.getLabel().getValue(), i);
                }
            }

            System.err.println("binary: " + binaryLabelSetIds.size());
            System.err.println("unary: " + unaryLabelSetIds.size());
            System.err.println("constants: " + wordIdsToLabelSetIds.size());
        }

        public CondensedTreeAutomaton<Span> getInvDecomp(List<String> sentence) {
            ConcreteCondensedTreeAutomaton<Span> ret = new ConcreteCondensedTreeAutomaton<>(hom.getSourceSignature());
            int n = sentence.size();

            // constants
            for (int i = 0; i < n; i++) {
                IntList stringFromHere = new IntArrayList();

                for (int j = i; j < n; j++) {
                    stringFromHere.add(hom.getTargetSignature().getIdForSymbol(sentence.get(j)));
                    int labelSetId = wordIdsToLabelSetIds.getInt(stringFromHere);

                    if (labelSetId != UNDEF) {
                        String[] labels = getLabels(hom.getLabelSetByLabelSetID(labelSetId), hom.getSourceSignature());
                        CondensedRule rule = ret.createRule(new Span(i, j + 1), labels, new Span[0]);
                        ret.addRule(rule);
                    }
                }

            }

            // unary
            for (int unary : unaryLabelSetIds) {
                String[] unaryLabels = getLabels(unary);
                int labelSetId = ret.addLabelSetID(unaryLabels, 1);

                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j <= n; j++) {
                        Span x = new Span(i, j);
                        int q = ret.addState(x);
                        CondensedRule rule = ret.createRuleRaw(q, labelSetId, new int[]{q}, 1.0);
                        ret.addRule(rule);
                    }
                }
            }

            // binary
            for (int binary : binaryLabelSetIds) {
                String[] binaryLabels = getLabels(binary);
                int labelSetId = ret.addLabelSetID(binaryLabels, 2);

                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j <= n; j++) {
                        int left = ret.addState(new Span(i, j));
                        for (int k = j + 1; k <= n; k++) {
                            int right = ret.addState(new Span(j, k));
                            int total = ret.addState(new Span(i, k));

                            CondensedRule rule = ret.createRuleRaw(total, labelSetId, new int[]{left, right}, 1.0);
                            ret.addRule(rule);
                        }
                    }
                }
            }

            ret.addFinalState(ret.addState(new Span(0, n)));

            return ret;
        }

        private String[] getLabels(int labelSetId) {
            return getLabels(hom.getLabelSetByLabelSetID(labelSetId), hom.getSourceSignature());
        }

        private static String[] getLabels(IntSet labelSet, Signature signature) {
            String[] ret = new String[labelSet.size()];
            int i = 0;

            for (int label : labelSet) {
                ret[i] = signature.resolveSymbolId(label);
                i++;
            }

            return ret;
        }
    }

    public static void main(String[] args) throws Exception {
//        String grammar = "a.irtg";
//        String sentence = "Pierre Vinken , 61 years old , will join the board as a nonexecutive director Nov. 29 .";
        String grammar = "gont/wsj0221_bin.irtg";
        String sentence = "DT NN NN NN JJ NN VBD CD NNS IN DT NNP";

        LambdaStopwatch w = new LambdaStopwatch(System.err);
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammar));
        Homomorphism hom = irtg.getInterpretation("string").getHomomorphism();
        StringAlgebra alg = (StringAlgebra) irtg.getInterpretation("string").getAlgebra();
        InvhomDecompFactory fact = w.t("init", () -> new InvhomDecompFactory(hom));

        List<String> words = alg.parseString(sentence);
        CondensedTreeAutomaton<Span> auto = w.t("getinv", () -> fact.getInvDecomp(words));
        TreeAutomaton chart = w.t("intersect", () -> irtg.getAutomaton().intersectCondensed(auto));

        Tree t = chart.viterbi();
        System.err.println(t);
        System.err.println(irtg.interpret(t));
    }
}
