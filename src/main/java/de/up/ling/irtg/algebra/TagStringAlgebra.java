/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.*;

/**
 * A string algebra for TAG. The elements of this algebra are strings and string
 * pairs, represented as pairs of lists of strings. If the second element is
 * null, the first element represents a string of tokens as in the
 * {@link StringAlgebra}. If the second element is non-null, then the pair
 * represents a non-contiguous pair of substrings.
 * <p>
 *
 * This algebra defines the string-combining operations described in Koller and
 * Kuhlmann 2012, <a href="http://www.ling.uni-potsdam.de/~koller/showpaper.php?id=tag-irtg">"Decomposing TAG Algorithms Using Simple Algebraizations"</a>,
 * In particular:
 * <ul>
 * <li>*CONC11*(v,w) concatenates two strings into the string vw.</li>
 * <li>*CONC12*(v,(w1,w2)) concatenates a string and a string pair into the
 * string pair (vw1, w2).</li>
 * <li>*CONC21*((v1,v2),w) concatenates a string pair and a string into the
 * string pair (v1, v2w).</li>
 * <li>*WRAP21*((v1,v2),w) wraps a string pair around a string, yielding the
 * string v1wv2.</li>
 * <li>*WRAP22*((v1,v2), (w1,w2)) wraps one string pair around another, yielding
 * the string pair (v1w1, w2v2).</li>
 * <li>*E* evaluates to the empty string.</li>
 * <li>*EE* evaluates to the string pair (e,e), where e is the empty
 * string.</li>
 * <li>All other strings evaluate to themselves, as strings.</li>
 * </ul>
 *
 * @author koller
 */
public class TagStringAlgebra extends Algebra<Pair<List<String>, List<String>>> {

//    private Signature signature;
    private Int2ObjectMap<Operation> namesToOperations = new Int2ObjectOpenHashMap<Operation>();

    public static enum Operation {

        //
        //
        // string + string -> string

        CONC11(new BinaryOperation("*CONC11*", 1, 1, 1) {
            @Override
            protected Pair<List<String>, List<String>> evaluate(Pair<List<String>, List<String>> first, Pair<List<String>, List<String>> second) {
                return new Pair(concat(first.left, second.left), null);
            }

            @Override
            public Set<Rule> makeTopDownRules(Pair<Span, Span> parent, TreeAutomaton<Pair<Span, Span>> auto) {
                Set<Rule> ret = new HashSet<Rule>();

                if (_arity(parent) == 1) {
                    // string + string
                    for (int split = parent.left.start; split <= parent.left.end; split++) {
                        ret.add(auto.createRule(parent, getName(),
                                l2(p(s(parent.left.start, split), null), p(s(split, parent.left.end), null))));
                    }
                }

                return ret;
            }

            @Override
            protected Pair<Span, Span> makeBottomUpRule(Pair<Span, Span> first, Pair<Span, Span> second, TreeAutomaton<Pair<Span, Span>> auto) {
                if (first.left.end == second.left.start) {
                    return new Pair(new Span(first.left.start, second.left.end), null);
                } else {
                    return null;
                }
            }
        }),
        //
        //
        // string + string pair -> string pair
        CONC12(new BinaryOperation("*CONC12*", 1, 2, 2) {
            @Override
            protected Pair<List<String>, List<String>> evaluate(Pair<List<String>, List<String>> first, Pair<List<String>, List<String>> second) {
                return new Pair(concat(first.left, second.left), second.right);
            }

            @Override
            public Set<Rule> makeTopDownRules(Pair<Span, Span> parent, TreeAutomaton<Pair<Span, Span>> auto) {
                Set<Rule> ret = new HashSet<Rule>();
                if (_arity(parent) == 2) {
                    for (int split = parent.left.start; split <= parent.left.end; split++) {
                        ret.add(auto.createRule(parent, getName(),
                                l2(p(s(parent.left.start, split), null), p(s(split, parent.left.end), parent.right))));
                    }
                }
                return ret;
            }

            @Override
            protected Pair<Span, Span> makeBottomUpRule(Pair<Span, Span> first, Pair<Span, Span> second, TreeAutomaton<Pair<Span, Span>> auto) {
                if (first.right == null && second.right != null && first.left.end == second.left.start) {
                    return new Pair(new Span(first.left.start, second.left.end), second.right);
                } else {
                    return null;
                }
            }
        }),
        //
        //
        // string pair + string -> string pair
        CONC21(new BinaryOperation("*CONC21*", 2, 1, 2) {
            @Override
            protected Pair<List<String>, List<String>> evaluate(Pair<List<String>, List<String>> first, Pair<List<String>, List<String>> second) {
                return new Pair(first.left, concat(first.right, second.left));
            }

            @Override
            public Set<Rule> makeTopDownRules(Pair<Span, Span> parent, TreeAutomaton<Pair<Span, Span>> auto) {
                Set<Rule> ret = new HashSet<Rule>();
                if (_arity(parent) == 2) {

                    for (int split = parent.right.start; split <= parent.right.end; split++) {
                        ret.add(auto.createRule(parent, getName(),
                                l2(p(parent.left, s(parent.right.start, split)), p(s(split, parent.right.end), null))));
                    }
                }
                return ret;
            }

            @Override
            protected Pair<Span, Span> makeBottomUpRule(Pair<Span, Span> first, Pair<Span, Span> second, TreeAutomaton<Pair<Span, Span>> auto) {
                if (first.right != null && second.right == null && first.right.end == second.left.start) {
                    Pair<Span,Span> ret = new Pair(first.left, new Span(first.right.start, second.left.end));
                    return ret;
                } else {
                    return null;
                }
            }
        }),
        //
        //
        // string pair + string -> string
        WRAP21(new BinaryOperation("*WRAP21*", 2, 1, 1) {
            @Override
            protected Pair<List<String>, List<String>> evaluate(Pair<List<String>, List<String>> first, Pair<List<String>, List<String>> second) {
                return new Pair(concat(first.left, concat(second.left, first.right)), null);
            }

            @Override
            public Set<Rule> makeTopDownRules(Pair<Span, Span> parent, TreeAutomaton<Pair<Span, Span>> auto) {
                Set<Rule> ret = new HashSet<Rule>();

                if (_arity(parent) == 1) {
                    for (int start = parent.left.start; start <= parent.left.end; start++) {
                        for (int end = start; end <= parent.left.end; end++) {
                            ret.add(auto.createRule(parent, getName(),
                                    l2(p(s(parent.left.start, start), s(end, parent.left.end)),
                                            p(s(start, end), null))));
                        }
                    }
                }
                return ret;
            }

            @Override
            protected Pair<Span, Span> makeBottomUpRule(Pair<Span, Span> first, Pair<Span, Span> second, TreeAutomaton<Pair<Span, Span>> auto) {
                if (first.left.end == second.left.start && first.right.start == second.left.end) {
                    return new Pair(new Span(first.left.start, first.right.end), null);
                } else {
                    return null;
                }
            }
        }),
        //
        //
        // string pair + string pair -> string pair
        WRAP22(new BinaryOperation("*WRAP22*", 2, 2, 2) {
            @Override
            protected Pair<List<String>, List<String>> evaluate(Pair<List<String>, List<String>> first, Pair<List<String>, List<String>> second) {
                return new Pair(concat(first.left, second.left), concat(second.right, first.right));
            }

            @Override
            public Set<Rule> makeTopDownRules(Pair<Span, Span> parent, TreeAutomaton<Pair<Span, Span>> auto) {
                Set<Rule> ret = new HashSet<Rule>();

                if (_arity(parent) == 2) {
                    for (int split1 = parent.left.start; split1 <= parent.left.end; split1++) {
                        for (int split2 = parent.right.start; split2 <= parent.right.end; split2++) {
                            ret.add(auto.createRule(parent, getName(),
                                    l2(p(s(parent.left.start, split1), s(split2, parent.right.end)),
                                            p(s(split1, parent.left.end), s(parent.right.start, split2)))));
                        }
                    }
                }
                return ret;
            }

            @Override
            protected Pair<Span, Span> makeBottomUpRule(Pair<Span, Span> first, Pair<Span, Span> second, TreeAutomaton<Pair<Span, Span>> auto) {
                if (first.left.end == second.left.start && second.right.end == first.right.start) {
                    return new Pair(new Span(first.left.start, second.left.end), new Span(second.right.start, first.right.end));
                } else {
                    return null;
                }
            }
        }),
        //
        //
        // the string pair (epsilon, epsilon)
        EPSILON_EPSILON(null) {
                    @Override
                    Pair<List<String>, List<String>> evaluate(List<Pair<List<String>, List<String>>> children) {
                        if (!children.isEmpty()) {
                            throw new UnsupportedOperationException("*EE* is a constant");
                        }

                        return new Pair(new ArrayList<String>(), new ArrayList<String>());
                    }

                    @Override
                    Set<Rule> makeBottomUpRules(int[] children, int n, TreeAutomaton<Pair<Span, Span>> auto) {
                        Set<Rule> ret = new HashSet<Rule>();

                        if (children.length > 0) {
                            return ret;
//                    throw new UnsupportedOperationException("*EE* is a constant");
                        }

                        for (int i = 0; i <= n; i++) {
                            for (int j = i; j <= n; j++) {
                                Pair parent = new Pair(new Span(i, i), new Span(j, j));
                                ret.add(auto.createRule(parent, label(), getChildStates(children, auto)));
                            }
                        }

                        return ret;
                    }

                    @Override
                    Set<Rule> makeTopDownRules(int parentId, TreeAutomaton<Pair<Span, Span>> auto) {
                        Set<Rule> ret = new HashSet<Rule>();
                        Pair<Span, Span> parent = auto.getStateForId(parentId);

                        if (_arity(parent) == 2) {
                            if (parent.left.start == parent.left.end && parent.right.start == parent.right.end) {
                                ret.add(auto.createRule(parent, label(), new Pair[]{}));
                            }
                        }

                        return ret;
                    }

                    @Override
                    public String label() {
                        return "*EE*";
                    }

                    @Override
                    public int arity() {
                        return 2;
                    }
                },
        //
        //
        // the empty string epsilon
        EPSILON(null) {
                    @Override
                    Pair<List<String>, List<String>> evaluate(List<Pair<List<String>, List<String>>> children) {
                        if (!children.isEmpty()) {
                            throw new UnsupportedOperationException("*EE* is a constant");
                        }

                        return new Pair(new ArrayList<String>(), null);
                    }

                    @Override
                    Set<Rule> makeBottomUpRules(int[] children, int n, TreeAutomaton<Pair<Span, Span>> auto) {
                        Set<Rule> ret = new HashSet<Rule>();

                        if (children.length > 0) {
                            return ret;
                        }

                        for (int i = 0; i <= n; i++) {
                            Pair parent = new Pair(new Span(i, i), null);
                            ret.add(auto.createRule(parent, label(), new Pair[]{}));
                        }

                        return ret;
                    }

                    @Override
                    Set<Rule> makeTopDownRules(int parentId, TreeAutomaton<Pair<Span, Span>> auto) {
                        Set<Rule> ret = new HashSet<Rule>();
                        Pair<Span, Span> parent = auto.getStateForId(parentId);

                        if (_arity(parent) == 1) {
                            if (parent.left.start == parent.left.end) {
                                ret.add(auto.createRule(parent, label(), new Pair[]{}));
                            }
                        }

                        return ret;
                    }

                    @Override
                    public String label() {
                        return "*E*";
                    }

                    @Override
                    public int arity() {
                        return 1;
                    }
                };
        private final BinaryOperation binop;

        Operation(BinaryOperation binop) {
            this.binop = binop;
        }

        public String label() {
            return binop.getName();
        }

        public int arity() {
            return binop.getArity();
        }

        Pair<List<String>, List<String>> evaluate(List<Pair<List<String>, List<String>>> children) {
            return binop.evaluate(children);
        }

        Set<Rule> makeBottomUpRules(int[] children, int n, TreeAutomaton<Pair<Span, Span>> auto) {
            return binop.makeBottomUpRules(children, n, auto);
        }

        Set<Rule> makeTopDownRules(int parent, TreeAutomaton<Pair<Span, Span>> auto) {
            return binop.makeTopDownRules(auto.getStateForId(parent), auto);
        }
    }

    public static String WRAP21() {
        return Operation.WRAP21.label();
    }

    public static String WRAP22() {
        return Operation.WRAP22.label();
    }

    public static String WRAP(int i, int j) {
        if (j == 1) {
            return WRAP21();
        } else {
            return WRAP22();
        }
    }

    public static String CONCAT11() {
        return Operation.CONC11.label();
    }

    public static String CONCAT21() {
        return Operation.CONC21.label();
    }

    public static String CONCAT12() {
        return Operation.CONC12.label();
    }

    public static String CONCAT(int i, int j) {
        if (i == 1) {
            if (j == 1) {
                return CONCAT11();
            } else {
                return CONCAT12();
            }
        } else {
            if (j == 1) {
                return CONCAT21();
            }
        }

        return null;
    }

    public static String EE() {
        return Operation.EPSILON_EPSILON.label();
    }

    public static String E() {
        return Operation.EPSILON.label();
    }

//    static {
//        for (Operation op : Operation.values()) {
//            namesToOperations.put(op.label(), op);
//        }
//    }
    private static Span s(int start, int end) {
        return new Span(start, end);
    }

    private static Pair<Span, Span> p(Span first, Span second) {
        return new Pair<Span, Span>(first, second);
    }

    private static Pair[] l2(Pair p1, Pair p2) {
        return new Pair[]{p1, p2};
    }

    public TagStringAlgebra() {
//        signature = new Signature();

        createSymbol(Operation.CONC11, 2);
        createSymbol(Operation.CONC12, 2);
        createSymbol(Operation.CONC21, 2);
        createSymbol(Operation.WRAP21, 2);
        createSymbol(Operation.WRAP22, 2);
        createSymbol(Operation.EPSILON, 0);
        createSymbol(Operation.EPSILON_EPSILON, 0);

        // plus every word that we see as a nullary symbol
    }

    private void createSymbol(Operation op, int arity) {
        String name = op.label();
        int id = signature.addSymbol(name, arity);
        namesToOperations.put(id, op);
    }

    @Override
    protected Pair<List<String>, List<String>> evaluate(String label, List<Pair<List<String>, List<String>>> childrenValues) {
        int labelId = signature.getIdForSymbol(label);

        if (namesToOperations.containsKey(labelId)) {
            return namesToOperations.get(labelId).evaluate(childrenValues);
        } else {
            List<String> l = new ArrayList<String>();
            l.add(label);
            return new Pair<List<String>, List<String>>(l, null);
        }
    }

//    @Override
//    public Pair<List<String>, List<String>> evaluate(Tree<String> t) {
//        return t.dfs(new TreeVisitor<String, Void, Pair<List<String>, List<String>>>() {
//            @Override
//            public Pair<List<String>, List<String>> combine(Tree<String> node, List<Pair<List<String>, List<String>>> childrenValues) {
//                String label = node.getLabel();
//                int labelId = signature.getIdForSymbol(label);
//
//                if (namesToOperations.containsKey(labelId)) {
//                    return namesToOperations.get(labelId).evaluate(childrenValues);
//                } else {
//                    List<String> l = new ArrayList<String>();
//                    l.add(label);
//                    return new Pair<List<String>, List<String>>(l, null);
//                }
//            }
//        });
//
//    }

    @Override
    public TreeAutomaton decompose(Pair<List<String>, List<String>> value) {
        // TODO - so far this only works if "value" is of arity 1
        return new TagDecompositionAutomaton(value.left);
    }

    @Override
    public Pair<List<String>, List<String>> parseString(String representation) throws ParserException {
        List<String> words = Arrays.asList(representation.split("\\s+"));

        for (String word : words) {
            signature.addSymbol(word, 0);
        }

        return new Pair<List<String>, List<String>>(words, null);
    }

//    @Override
//    public JComponent visualize(Pair<List<String>, List<String>> object) {
//        if (object.right == null) {
//            return new JLabel(StringTools.join(object.left, " "));
//        } else {
//            return new JLabel("[" + StringTools.join(object.left, " ") + " / " + StringTools.join(object.right, " ") + "]");
//        }
//    }

    @Override
    public String representAsString(Pair<List<String>, List<String>> object) {
        if (object.right == null) {
            return StringTools.join(object.left, " ");
        } else {
            return "[" + StringTools.join(object.left, " ") + " / " + StringTools.join(object.right, " ") + "]";
        }
    }
    
    

    public static <E, F> int _arity(Pair<E, F> pair) {
        return pair.right == null ? 1 : 2;
    }

    public class TagDecompositionAutomaton extends TreeAutomaton<Pair<Span, Span>> {

        private int[] words;

        public TagDecompositionAutomaton(List<String> words) {
            super(TagStringAlgebra.this.getSignature());

            this.words = new int[words.size()];
            for (int i = 0; i < words.size(); i++) {
                this.words[i] = getSignature().addSymbol(words.get(i), 0);
            }

            finalStates.add(addState(new Pair(new Span(0, words.size()), null)));

            // states of arity 1
            for (int i = 0; i < words.size(); i++) {
                for (int k = i + 1; k <= words.size(); k++) {
                    Pair p = new Pair(new Span(i, k), null);
                    addState(p);
                }
            }

            // states of arity 2
            for (int i = 0; i <= words.size(); i++) {
                for (int j = i; j <= words.size(); j++) {
                    for (int k = j; k <= words.size(); k++) {
                        for (int l = k; l <= words.size(); l++) {
                            Pair p = new Pair(new Span(i, j), new Span(k, l));
                            addState(p);
                        }
                    }
                }
            }
        }

        @Override
        public Set<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            if (namesToOperations.containsKey(labelId)) {
                return namesToOperations.get(labelId).makeBottomUpRules(childStates, words.length, this);
            } else {
                Set<Rule> ret = new HashSet<Rule>();
                for (int i = 0; i < words.length; i++) {
                    if (words[i] == labelId) {
                        Pair parent = new Pair(new Span(i, i + 1), null);
                        List<Pair<Span, Span>> children = new ArrayList<Pair<Span, Span>>();
                        ret.add(createRule(parent, getSignature().resolveSymbolId(labelId), children));
                    }
                }

                return ret;
            }
        }

        @Override
        public Set<Rule> getRulesTopDown(int label, int parentStateId) {
            if (namesToOperations.containsKey(label)) {
                return namesToOperations.get(label).makeTopDownRules(parentStateId, this);
            } else {
                Set<Rule> ret = new HashSet<Rule>();
                Pair<Span, Span> parentState = getStateForId(parentStateId);

                if (_arity(parentState) == 1) {
                    if (parentState.left.length() == 1) {
                        if (words[parentState.left.start] == label) {
                            ret.add(createRule(parentStateId, label, new int[]{}, 1));
                        }
                    }
                }

                return ret;
            }
        }

        public Pair<Span, Span> parseState(String state) {
            String[] parts = state.split("[-,]");

            if (parts.length == 4) {
                return p(s(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])),
                        s(Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
            } else {
                return p(s(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])), null);
            }
        }

        @Override
        public boolean isBottomUpDeterministic() {
            // TAG decomposition automata are always nondeterministic,
            // because *EE* can be inserted at any point
            return false;
        }
    }

//    public Signature getSignature() {
//        return signature;
//    }

    public int getSort(int name) {
        if (namesToOperations.containsKey(name)) {
            return namesToOperations.get(name).arity();
        } else {
            // unknown symbols are assumed to be constants (of sort 1)
            return 1;
        }

    }

    private abstract static class BinaryOperation {

        private String name;
        private int arity1, arity2;
        private int arity;

        public BinaryOperation(String name, int arity1, int arity2, int arity) {
            this.name = name;
            this.arity1 = arity1;
            this.arity2 = arity2;
            this.arity = arity;
        }

        public int getArity1() {
            return arity1;
        }

        public int getArity2() {
            return arity2;
        }

        public int getArity() {
            return arity;
        }

        abstract protected Pair<List<String>, List<String>> evaluate(Pair<List<String>, List<String>> first, Pair<List<String>, List<String>> second);

        abstract public Set<Rule> makeTopDownRules(Pair<Span, Span> parent, TreeAutomaton<Pair<Span, Span>> auto);

        abstract protected Pair<Span, Span> makeBottomUpRule(Pair<Span, Span> first, Pair<Span, Span> second, TreeAutomaton<Pair<Span, Span>> auto);

        public String getName() {
            return name;
        }

        public Pair<List<String>, List<String>> evaluate(List<Pair<List<String>, List<String>>> children) {
            if (children.size() != 2) {
                throw new UnsupportedOperationException("Wrong number of arguments for concatenation: " + children);
            }

            Pair<List<String>, List<String>> first = children.get(0);
            Pair<List<String>, List<String>> second = children.get(1);

            if (_arity(first) == arity1 && _arity(second) == arity2) {
                return evaluate(first, second);
            }

            throw new UnsupportedOperationException("Illegal concatenation: " + name + " of " + first + " + " + second);
        }

        public Set<Rule> makeBottomUpRules(int[] children, int n, TreeAutomaton<Pair<Span, Span>> auto) {
            Set<Rule> ret = new HashSet<Rule>();
            Pair parent = null;

            if (children.length != 2) {
                return ret;
            }

            Pair<Span, Span> first = auto.getStateForId(children[0]);
            Pair<Span, Span> second = auto.getStateForId(children[1]);

            if (_arity(first) == arity1 && _arity(second) == arity2) {
                parent = makeBottomUpRule(first, second, auto);
            }

            if (parent != null) {
                ret.add(auto.createRule(parent, name, getChildStates(children, auto)));
            }

            return ret;
        }

        public static List<String> concat(List<String> x, List<String> y) {
            List<String> ret = new ArrayList<String>(x);
            ret.addAll(y);
            return ret;
        }
    }

    private static Pair<Span, Span>[] getChildStates(int[] childStateIds, TreeAutomaton<Pair<Span, Span>> auto) {
        Pair<Span, Span>[] childStates = new Pair[childStateIds.length];

        for (int i = 0; i < childStateIds.length; i++) {
            childStates[i] = auto.getStateForId(childStateIds[i]);
        }

        return childStates;
    }
}
