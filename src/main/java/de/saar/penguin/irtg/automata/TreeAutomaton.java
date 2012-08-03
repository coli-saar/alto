/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.saar.penguin.irtg.semiring.DoubleArithmeticSemiring;
import de.saar.penguin.irtg.semiring.LongArithmeticSemiring;
import de.saar.penguin.irtg.semiring.Semiring;
import de.saar.penguin.irtg.semiring.ViterbiWithBackpointerSemiring;
import de.saar.penguin.irtg.signature.Signature;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class TreeAutomaton<State> implements Serializable {
    protected Map<String, StateListToStateMap> explicitRules; // one for each label
    protected Map<String, SetMultimap<State, Rule<State>>> explicitRulesTopDown;
    protected Set<State> finalStates;
    protected Set<State> allStates;
    private final Map<String, State> dummyLtsSubstitution = new HashMap<String, State>();
    protected boolean isExplicit;
    protected SetMultimap<State, Rule<State>> rulesForRhsState;
    protected Signature signature;

    public TreeAutomaton(Signature signature) {
        explicitRules = new HashMap<String, StateListToStateMap>();
        explicitRulesTopDown = new HashMap<String, SetMultimap<State, Rule<State>>>();
        finalStates = new HashSet<State>();
        allStates = new HashSet<State>();
        isExplicit = false;
        rulesForRhsState = HashMultimap.create();
        this.signature = signature;
    }

    public Signature getSignature() {
        return signature;
    }

    /**
     * Finds automaton rules bottom-up for a given list of child states
     * and a given parent label. The method returns a collection of
     * rules that can be used to assign a state to the parent node.
     * 
     * @param label
     * @param childStates
     * @return 
     */
    abstract public Set<Rule<State>> getRulesBottomUp(String label, List<State> childStates);

    /**
     * Finds automaton rules top-down for a given parent state and label.
     * The method returns a collection of rules that can be used to
     * assign states to the children.
     * 
     * @param label
     * @param parentState
     * @return 
     */
    abstract public Set<Rule<State>> getRulesTopDown(String label, State parentState);

    /**
     * Returns a set that contains all terminal symbols f
     * such that the automaton has top-down
     * transition rules parentState -> f(...). The set returned by this
     * method may contain symbols for which such a transition does not
     * actually exist; it is only guaranteed that all symbols for which
     * transitions exist are also in the set. The default implementation
     * in BottomUpAutomaton returns getAllLabels(). Subclasses
     * (especially lazy automata) may replace this with more specific implementations.
     * 
     * @param parentState
     * @return 
     */
    public Set<String> getLabelsTopDown(State parentState) {
        return getAllLabels();
    }

    /**
     * Returns true whenever the automaton has a bottom-up rule whose first
     * n child states are the n child states that are passed as the prefixOfChildren
     * argument. It is not required that the method returns false if the automaton
     * does _not_ have such a rule; i.e., the method may overestimate the existence
     * of rules. The default implementation always returns true. Derived automaton
     * classes for which an efficient, more precise test is available may override
     * this method appropriately. This may speed up the Earley intersection algorithm.
     * 
     * @param label
     * @param prefixOfChildren
     * @return 
     */
    public boolean hasRuleWithPrefix(String label, List<State> prefixOfChildren) {
        return true;
    }

    /**
     * Returns the arity of a terminal symbol. Apparently this method
     * is not being used anywhere, so I'm deleting it for now.
     * 
     * @param label
     * @return 
     */
//    abstract public int getArity(String label);
    /**
     * Returns all terminal symbols that this automaton knows about.
     * 
     * @return 
     */
    abstract public Set<String> getAllLabels();

    /**
     * Returns the final states of the automaton.
     * 
     * @return 
     */
    public Set<State> getFinalStates() {
        return finalStates;
    }

    /**
     * Returns the set of all states of this automaton.
     * 
     * @return 
     */
    public Set<State> getAllStates() {
        return allStates;
    }

    /**
     * Caches a rule for future use. Once a rule has been cached,
     * it will be found by getRulesBottomUpFromExplicit and getRulesTopDownFromExplicit.
     * 
     * @param rule 
     */
    protected void storeRule(Rule<State> rule) {
        // store as bottom-up rule
        StateListToStateMap smap = getOrCreateStateMap(rule.getLabel());
        smap.put(rule);

        // store as top-down rule
        SetMultimap<State, Rule<State>> topdown = explicitRulesTopDown.get(rule.getLabel());
        if (topdown == null) {
            topdown = HashMultimap.create();
            explicitRulesTopDown.put(rule.getLabel(), topdown);
        }
        topdown.put(rule.getParent(), rule);

        // store pointer from rhs states to rule
        for (State rhs : rule.getChildren()) {
            rulesForRhsState.put(rhs, rule);
        }

        // collect states
        if (allStates != null) {
            allStates.add(rule.getParent());
            for (int i = 0; i < rule.getArity(); i++) {
                allStates.add(rule.getChildren()[i]);
            }
        }
    }

    /**
     * Like getRulesBottomUp, but only looks for rules in the
     * cache of previously discovered rules.
     * 
     * @param label
     * @param childStates
     * @return 
     */
    protected Set<Rule<State>> getRulesBottomUpFromExplicit(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return new HashSet<Rule<State>>();
        } else {
            return smap.get(childStates);
        }
    }

    /**
     * Like getRulesTopDown, but only looks for rules in the
     * cache of previously discovered rules.
     * 
     * @param label
     * @param parentState
     * @return 
     */
    protected Set<Rule<State>> getRulesTopDownFromExplicit(String label, State parentState) {
        if (useCachedRuleTopDown(label, parentState)) {
            if (!explicitRulesTopDown.containsKey(label) || !explicitRulesTopDown.get(label).containsKey(parentState)) {
                return new HashSet<Rule<State>>();
            } else {
                return explicitRulesTopDown.get(label).get(parentState);
            }
        } else {
            return new HashSet<Rule<State>>();
        }
    }

    /**
     * Returns the set of all rules of this automaton. This method
     * is currently implemented rather inefficiently. Note that it
     * necessarily _computes_ the set of all rules, which may be expensive
     * for lazy automata.
     * 
     * @return 
     */
    public Set<Rule<State>> getRuleSet() {
        Set<Rule<State>> ret = new HashSet<Rule<State>>();

        makeAllRulesExplicit();

        for (StateListToStateMap map : explicitRules.values()) {
            for (Set<Rule<State>> set : map.getAllRules().values()) {
                ret.addAll(set);
            }
        }

        return ret;
    }

    /**
     * Returns the set of all rules, indexed by parent label and
     * children states.
     * 
     * @return 
     */
    private Map<String, Map<List<State>, Set<Rule<State>>>> getAllRules() {
        Map<String, Map<List<State>, Set<Rule<State>>>> ret = new HashMap<String, Map<List<State>, Set<Rule<State>>>>();

        makeAllRulesExplicit();

        for (String f : getAllLabels()) {
            ret.put(f, getAllRules(f));
        }

        return ret;
    }

    private Map<List<State>, Set<Rule<State>>> getAllRules(String label) {
        if (explicitRules.containsKey(label)) {
            return explicitRules.get(label).getAllRules();
        } else {
            return new HashMap<List<State>, Set<Rule<State>>>();
        }
    }

    /**
     * Returns the number of trees in the language of this
     * automaton. Note that this is faster than computing the
     * entire language. The method only works if the automaton
     * is acyclic, and only returns correct results if the
     * automaton is bottom-up deterministic.
     * 
     * @return 
     */
    @CallableFromShell
    public long countTrees() {
        Map<State, Long> map = evaluateInSemiring(new LongArithmeticSemiring(), new RuleEvaluator<State, Long>() {
            public Long evaluateRule(Rule<State> rule) {
                return 1L;
            }
        });

        long ret = 0L;
        for (State f : getFinalStates()) {
            ret += map.get(f);
        }
        return ret;
    }

    /**
     * Returns a map representing the inside probability of
     * each state.
     * 
     * @return 
     */
    public Map<State, Double> inside() {
        return evaluateInSemiring(new DoubleArithmeticSemiring(), new RuleEvaluator<State, Double>() {
            public Double evaluateRule(Rule<State> rule) {
                return rule.getWeight();
            }
        });
    }

    /**
     * Returns a map representing the outside probability of
     * each state.
     * 
     * @param inside a map representing the inside probability of each state.
     * @return 
     */
    public Map<State, Double> outside(final Map<State, Double> inside) {
        return evaluateInSemiringTopDown(new DoubleArithmeticSemiring(), new RuleEvaluatorTopDown<State, Double>() {
            public Double initialValue() {
                return 1.0;
            }

            public Double evaluateRule(Rule<State> rule, int i) {
                Double ret = rule.getWeight();
                for (int j = 0; j < rule.getArity(); j++) {
                    if (j != i) {
                        ret = ret * inside.get(rule.getChildren()[j]);
                    }
                }
                return ret;
            }
        });
    }

    /**
     * Computes the highest-weighted tree in the language of this
     * (weighted) automaton, using the Viterbi algorithm.
     * 
     * @return 
     */
    @CallableFromShell
    public Tree viterbi() {
        // run Viterbi algorithm bottom-up, saving rules as backpointers
        Map<State, Pair<Double, Rule<State>>> map =
                evaluateInSemiring(new ViterbiWithBackpointerSemiring<State>(), new RuleEvaluator<State, Pair<Double, Rule<State>>>() {
            public Pair<Double, Rule<State>> evaluateRule(Rule<State> rule) {
                return new Pair<Double, Rule<State>>(rule.getWeight(), rule);
            }
        });

        // find final state with highest weight
        State bestFinalState = null;
        double weightBestFinalState = Double.POSITIVE_INFINITY;
        for (State s : getFinalStates()) {
            if (map.get(s).left < weightBestFinalState) {
                bestFinalState = s;
                weightBestFinalState = map.get(s).left;
            }
        }

        // extract best tree from backpointers
        return extractTreeFromViterbi(bestFinalState, map);
    }

    private Tree<String> extractTreeFromViterbi(State state, Map<State, Pair<Double, Rule<State>>> map) {
        Rule<State> backpointer = map.get(state).right;
        List<Tree<String>> childTrees = new ArrayList<Tree<String>>();

        for (State child : backpointer.getChildren()) {
            childTrees.add(extractTreeFromViterbi(child, map));
        }

        return Tree.create(backpointer.getLabel(), childTrees);
    }

    /**
     * Computes the tree language accepted by this automaton.
     * This only works if the automaton is acyclic (in which case
     * the language is also finite). 
     * 
     * @return 
     */
    @CallableFromShell(joinList = "\n")
    public Set<Tree<String>> language() {
        /*
         * The current implementation is probably not particularly efficient.
         * It could be improved by using CartesianIterators for each rule.
         * Or don't return the Set as a whole, but just an Iterator.
         */
        Map<State, List<Tree<String>>> languagesForStates =
                evaluateInSemiring(new LanguageCollectingSemiring(), new RuleEvaluator<State, List<Tree<String>>>() {
            public List<Tree<String>> evaluateRule(Rule<State> rule) {
                List<Tree<String>> ret = new ArrayList<Tree<String>>();
                ret.add(Tree.create(rule.getLabel()));
                return ret;
            }
        });

        Set<Tree<String>> ret = new HashSet<Tree<String>>();
        for (State finalState : getFinalStates()) {
            ret.addAll(languagesForStates.get(finalState));
        }
        return ret;
    }

    private static class LanguageCollectingSemiring implements Semiring<List<Tree<String>>> {
        // +: concatenate the two languages
        public List<Tree<String>> add(List<Tree<String>> x, List<Tree<String>> y) {
            x.addAll(y);
            return x;
        }

        // *: add each tree in newSubtrees as daughters to the root of
        // each tree in partialTrees
        public List<Tree<String>> multiply(List<Tree<String>> partialTrees, List<Tree<String>> newSubtrees) {
            List<Tree<String>> ret = new ArrayList<Tree<String>>();
            for (Tree<String> partialTree : partialTrees) {
                for (Tree<String> newSubtree : newSubtrees) {
                    ret.add(partialTree.addSubtree(newSubtree));
                }
            }

            return ret;
        }

        public List<Tree<String>> zero() {
            return new ArrayList<Tree<String>>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TreeAutomaton)) {
            return false;
        }

        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();
        Map<String, Map<List<State>, Set<Rule<State>>>> otherRules = ((TreeAutomaton) o).getAllRules();

        if (!rules.keySet().equals(otherRules.keySet())) {
            return false;
        }

        for (String f : rules.keySet()) {
            if (!rules.get(f).keySet().equals(otherRules.get(f).keySet())) {
                return false;
            }

            for (List<State> states : rules.get(f).keySet()) {
                if (!new HashSet<Rule<State>>(rules.get(f).get(states)).equals(new HashSet<Rule<State>>(otherRules.get(f).get(states)))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();

        for (String f : rules.keySet()) {
            for (List<State> children : rules.get(f).keySet()) {
                for (Rule rule : rules.get(f).get(children)) {
                    buf.append(rule.toString() + (getFinalStates().contains(rule.getParent()) ? "!" : "") + "\n");
                }
            }
        }

        return buf.toString();
    }

    /**
     * Computes all rules in this automaton and stores them in the cache.
     * This only makes a difference for lazy automata, in which rules
     * are only computed by need. After calling this function, it is
     * guaranteed that all rules are in the cache.
     */
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Set<State> everAddedStates = new HashSet<State>();
            Queue<State> agenda = new LinkedList<State>();

            agenda.addAll(getFinalStates());
            everAddedStates.addAll(getFinalStates());

            while (!agenda.isEmpty()) {
                State state = agenda.remove();

                for (String label : getAllLabels()) {
                    Set<Rule<State>> rules = getRulesTopDown(label, state);
                    for (Rule<State> rule : rules) {
                        for (State child : rule.getChildren()) {
                            if (!everAddedStates.contains(child)) {
                                everAddedStates.add(child);
                                agenda.offer(child);
                            }
                        }
                    }
                }
            }

            isExplicit = true;
        }
    }

    public ConcreteTreeAutomaton<State> makeConcreteAutomaton() {
        makeAllRulesExplicit();

        ConcreteTreeAutomaton<State> ret = new ConcreteTreeAutomaton<State>();

        ret.explicitRules = explicitRules;
        ret.explicitRulesTopDown = explicitRulesTopDown;
        ret.finalStates = finalStates;
        ret.allStates = allStates;
        ret.isExplicit = isExplicit;
        ret.rulesForRhsState = rulesForRhsState;

        return ret;
    }

    /**
     * Checks whether the cache contains a bottom-up rule for
     * the given parent label and children states.
     * 
     * @param label
     * @param childStates
     * @return 
     */
    protected boolean useCachedRuleBottomUp(String label, List<State> childStates) {
        if (isExplicit) {
            return true;
        }

        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return false;
        } else {
            return smap.contains(childStates);
        }
    }

    /**
     * Checks whether the cache contains a top-down rule for
     * the given parent label and state.
     * @param label
     * @param parent
     * @return 
     */
    protected boolean useCachedRuleTopDown(String label, State parent) {
        // Even when the automaton has been computed explicltly, not all labels
        // that are returned by getAllLabels() may have entries in explicitRulesTopDown.
        // This happens when the automaton doesn't contain any rules for these labels,
        // e.g. for InverseHomAutomata (see getAllLabels of that class).
        SetMultimap<State, Rule<State>> topdown = explicitRulesTopDown.get(label);
        if (isExplicit) {
            return true;
        } else if (topdown == null) {
            return false;
        } else {
            return topdown.containsKey(parent);
        }
    }

    /**
     * Intersects this automaton with another one.
     * 
     * @param <OtherState> the state type of the other automaton.
     * @param other the other automaton.
     * @return an automaton representing the intersected language.
     */
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersect(TreeAutomaton<OtherState> other) {
        return new IntersectionAutomaton<State, OtherState>(this, other);
    }

    /**
     * Computes the pre-image of this automaton under a homomorphism.
     * 
     * @param hom the homomorphism.
     * @return an automaton representing the homomorphic pre-image.
     */
    public TreeAutomaton<String> inverseHomomorphism(Homomorphism hom) {
        return new InverseHomAutomaton<State>(this, hom);
    }

    /**
     * Computes the image of this automaton under a homomorphism.
     * 
     * @param hom
     * @return 
     */
    @CallableFromShell
    public TreeAutomaton<String> homomorphism(Homomorphism hom) {
        return new HomAutomaton(this, hom);
    }

    /**
     * Determines whether the automaton accepts a given tree.
     * 
     * @param tree
     * @return 
     */
    public boolean accepts(final Tree tree) {
        Set<State> resultStates = run(tree);
        resultStates.retainAll(getFinalStates());
        return !resultStates.isEmpty();
    }

    /**
     * Runs the automaton bottom-up on a given tree and returns the set
     * of possible states for the root. See also #run(Tree, Function).
     * 
     * @param tree
     * @return 
     */
    public Set<State> run(final Tree tree) {
        return run(tree, new Function<Tree<String>, State>() {
            @Override
            public State apply(Tree<String> f) {
                return null;
            }
        });
    }

    /**
     * Runs the automaton bottom-up on a given tree, assuming a certain
     * assignment of states to nodes.  This assignment is specified in the
     * second argument.  If this function returns a non-null value for a given
     * subtree, this value is used as the (unique) state for this subtree.
     * 
     * This method can run on trees with arbitrary label classes, but the
     * tree automaton itself can only have terminal symbols that are strings.
     * When running the automaton, the labels of the tree are therefore
     * converted to strings in order to look up the rules.
     * 
     * @param tree
     * @param subst
     * @return 
     */
    public <TreeLabels> Set<State> run(final Tree<TreeLabels> tree, final Function<Tree<TreeLabels>, State> subst) {
//        final Set<State> ret = new HashSet<State>();

        final Set<State> ret = (Set<State>) tree.dfs(new TreeVisitor<TreeLabels, Void, Set<State>>() {
            @Override
            public Set<State> combine(Tree<TreeLabels> node, List<Set<State>> childrenValues) {
                TreeLabels f = node.getLabel();
                Set<State> states = new HashSet<State>();
                State substState = subst.apply(node);

                if (substState != null) {
                    states.add(substState);
                } else if (childrenValues.isEmpty()) {
                    for (Rule<State> rule : getRulesBottomUp(f.toString(), new ArrayList<State>())) {
                        states.add(rule.getParent());
                    }
                } else {
                    CartesianIterator<State> it = new CartesianIterator<State>(childrenValues);

                    while (it.hasNext()) {
                        for (Rule<State> rule : getRulesBottomUp(f.toString(), it.next())) {
                            states.add(rule.getParent());
                        }
                    }
                }

//                if (node.equals(tree.getRoot())) {
//                    ret.addAll(states);
//                }

                return states;
            }
        });

        return ret;
    }

    /**
     * Reduces the automaton. This means that all states and rules that
     * are not reachable bottom-up are removed.
     * 
     * @return 
     */
    public TreeAutomaton<State> reduceBottomUp() {
        Set<State> productiveStates = getProductiveStates();
        ConcreteTreeAutomaton<State> ret = new ConcreteTreeAutomaton<State>();

        // copy all rules that only contain productive states
        for (Rule<State> rule : getRuleSet()) {
            boolean allProductive = productiveStates.contains(rule.getParent());

            for (State child : rule.getChildren()) {
                if (!productiveStates.contains(child)) {
                    allProductive = false;
                }
            }

            if (allProductive) {
                ret.addRule(rule);
            }
        }

        // copy all productive final states
        for (State state : getFinalStates()) {
            if (productiveStates.contains(state)) {
                ret.addFinalState(state);
            }
        }

        return ret;
    }

    public Set<State> getProductiveStates() {
        return new HashSet<State>(getStatesInBottomUpOrder());
    }

    /**
     * Evaluates all states of the automaton bottom-up
     * in a semiring. The evaluation of a state is the semiring sum
     * of semiring zero plus the evaluations of all rules in which it is the parent.
     * The evaluation of a rule is the semiring product of the evaluations
     * of its child states, times the evaluation of the rule itself.
     * The evaluation of a rule is determined by the RuleEvaluator argument.
     * This method only works if the automaton is acyclic, so states can be
     * processed in a well-defined bottom-up order.
     * 
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return a map assigning values in the semiring to all reachable states.
     */
    public <E> Map<State, E> evaluateInSemiring(Semiring<E> semiring, RuleEvaluator<State, E> evaluator) {
        Map<State, E> ret = new HashMap<State, E>();

        for (State s : getStatesInBottomUpOrder()) {
            E accu = semiring.zero();

            for (String label : getAllLabels()) {
                Set<Rule<State>> rules = getRulesTopDown(label, s);

                for (Rule<State> rule : rules) {
                    E valueThisRule = evaluator.evaluateRule(rule);
                    for (State child : rule.getChildren()) {
                        if (valueThisRule != null) {
                            if (ret.containsKey(child)) {
                                valueThisRule = semiring.multiply(valueThisRule, ret.get(child));
                            } else {
                                // if a child state hasn't been evaluated yet, this means that it
                                // is not reachable bottom-up, and therefore shouldn't be counted here
                                valueThisRule = null;
                            }
                        }
                    }

                    if (valueThisRule != null) {
                        accu = semiring.add(accu, valueThisRule);
                    }
                }
            }

            ret.put(s, accu);
        }

        return ret;
    }

    /**
     * Like evaluateInSemiring, but proceeds in top-down order.
     * 
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return 
     */
    public <E> Map<State, E> evaluateInSemiringTopDown(Semiring<E> semiring, RuleEvaluatorTopDown<State, E> evaluator) {
        Map<State, E> ret = new HashMap<State, E>();
        List<State> statesInOrder = getStatesInBottomUpOrder();
        Collections.reverse(statesInOrder);

        for (State s : statesInOrder) {
            E accu = semiring.zero();

            if (rulesForRhsState.containsKey(s)) {
                for (Rule<State> rule : rulesForRhsState.get(s)) {
                    E parentValue = ret.get(rule.getParent());
                    for (int i = 0; i < rule.getArity(); i++) {
                        if (rule.getChildren()[i].equals(s)) {
                            accu = semiring.add(accu, semiring.multiply(parentValue, evaluator.evaluateRule(rule, i)));
                        }
                    }
                }
            } else {
                accu = evaluator.initialValue();
            }

            ret.put(s, accu);
        }

        return ret;
    }

    /**
     * Returns a topological ordering of the states, such that
     * later nodes always occur above earlier nodes in any run
     * of the automaton on a tree. Note that only states that are
     * reachable top-down from the final states are included in the
     * list that is returned.
     * 
     * @return 
     */
    public List<State> getStatesInBottomUpOrder() {
        List<State> ret = new ArrayList<State>();
        SetMultimap<State, State> children = HashMultimap.create(); // children(q) = {q1,...,qn} means that q1,...,qn occur as child states of rules of which q is parent state
        Set<State> visited = new HashSet<State>();

        // traverse all rules to compute graph
        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();
        for (Map<List<State>, Set<Rule<State>>> rulesPerLabel : rules.values()) {
            for (List<State> lhs : rulesPerLabel.keySet()) {
                Set<Rule<State>> rhsStates = rulesPerLabel.get(lhs);

                for (Rule<State> rule : rhsStates) {
                    children.putAll(rule.getParent(), lhs);
                }
            }
        }

        // perform topological sort
        for (State q : getFinalStates()) {
            dfsForStatesInBottomUpOrder(q, children, visited, ret);
        }

        return ret;
    }

    private void dfsForStatesInBottomUpOrder(State q, SetMultimap<State, State> children, Set<State> visited, List<State> ret) {
        if (!visited.contains(q)) {
            visited.add(q);

            for (State parent : children.get(q)) {
                dfsForStatesInBottomUpOrder(parent, children, visited, ret);
            }

            ret.add(q);
        }
    }

    protected ListMultimap<State, Rule<State>> getRuleByChildStateMap() {
        ListMultimap<State, Rule<State>> ret = ArrayListMultimap.create();

        for (Rule<State> rule : getRuleSet()) {
            for (State child : rule.getChildren()) {
                ret.put(child, rule);
            }
        }

        return ret;
    }

    private StateListToStateMap getOrCreateStateMap(String label) {
        StateListToStateMap ret = explicitRules.get(label);

        if (ret == null) {
            ret = new StateListToStateMap(label);
            explicitRules.put(label, ret);
        }

        return ret;
    }

    protected class StateListToStateMap implements Serializable {
        private Map<State, StateListToStateMap> nextStep;
        private Set<Rule<State>> rulesHere;
        private int arity;
        private String label;

        public StateListToStateMap(String label) {
            rulesHere = new HashSet<Rule<State>>();
            nextStep = new HashMap<State, StateListToStateMap>();
            arity = -1;
            this.label = label;
        }

        public void put(Rule<State> rule) {
            put(rule, 0, 1);

            if (arity != -1) {
                if (arity != rule.getChildren().length) {
                    throw new UnsupportedOperationException("Storing state lists of different length: " + rule + ", should be " + arity);
                }
            } else {
                arity = rule.getChildren().length;
            }
        }

        private void put(Rule<State> rule, int index, double weight) {
            if (index == rule.getArity()) {
                rulesHere.add(rule);
            } else {
                State nextState = rule.getChildren()[index];
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    sub = new StateListToStateMap(label);
                    nextStep.put(nextState, sub);
                }

                sub.put(rule, index + 1, weight);
            }
        }

        public Set<Rule<State>> get(List<State> stateList) {
            return get(stateList, 0);
        }

        private Set<Rule<State>> get(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return rulesHere;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return new HashSet<Rule<State>>();
                } else {
                    return sub.get(stateList, index + 1);
                }
            }
        }

        public boolean contains(List<State> stateList) {
            return contains(stateList, 0);
        }

        private boolean contains(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return true;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return false;
                } else {
                    return sub.contains(stateList, index + 1);
                }
            }
        }

        public int getArity() {
            return arity;
        }

        public Map<List<State>, Set<Rule<State>>> getAllRules() {
            Map<List<State>, Set<Rule<State>>> ret = new HashMap<List<State>, Set<Rule<State>>>();
            List<State> currentStateList = new ArrayList<State>();
            retrieveAll(currentStateList, 0, getArity(), ret);
            return ret;
        }

        private void retrieveAll(List<State> currentStateList, int index, int arity, Map<List<State>, Set<Rule<State>>> ret) {
            if (index == arity) {
                ret.put(new ArrayList<State>(currentStateList), rulesHere);
            } else {
                for (State state : nextStep.keySet()) {
                    currentStateList.add(state);
                    nextStep.get(state).retrieveAll(currentStateList, index + 1, arity, ret);
                    currentStateList.remove(index);
                }
            }
        }

        @Override
        public String toString() {
            return getAllRules().toString();
        }
    }

    private class LanguageIterable implements Iterable<Tree<String>> {
        public Iterator<Tree<String>> iterator() {
            return new LanguageIterator();
        }
    }

    /**
     * Returns an Iterable, to be used in a for-each loop.
     * See the documentation of #languageIterator for caveats.
     * 
     * @return 
     */
    public Iterable<Tree<String>> languageIterable() {
        return new LanguageIterable();
    }

    /**
     * Returns an iterator over the language of this tree automaton.
     * 
     * This only works reliably if the automaton is non-recursive, i.e. the language
     * is finite. For infinite languages, the iterator makes an effort to
     * consider rules in an order that has a good chance of returning some trees;
     * but sometimes trying to enumerate even the first tree may
     * not terminate in this case.
     * 
     * The method assumes that the automaton has a single final state.
     * 
     * The iterator is highly optimized and avoids allocating new objects
     * as much as possible. Each tree it returns therefore becomes invalid
     * with the next call to "next". If you want to retain it, you must
     * clone it.
     * 
     * @return 
     */
    public Iterator<Tree<String>> languageIterator() {
        return new LanguageIterator();
    }

    private class LanguageIterator implements Iterator<Tree<String>> {
        private Map<State, Tree<String>> tree = new HashMap<State, Tree<String>>();
        private Map<State, List<Rule<State>>> rules = new HashMap<State, List<Rule<State>>>();
        private Map<State, Integer> currentRule = new HashMap<State, Integer>();
        private State finalState;
        private boolean first = true;

        public LanguageIterator() {
            assert getFinalStates().size() == 1;
            finalState = getFinalStates().iterator().next();
            Comparator<Rule<State>> cyclicityComparator = new RuleCyclicityComparator();

            for (State state : getAllStates()) {
                // cache ordered list of all top-down rules for each state
                List<Rule<State>> rulesForThisState = new ArrayList<Rule<State>>();
                for (String label : getAllLabels()) {
                    rulesForThisState.addAll(getRulesTopDown(label, state));
                }
                
                Collections.sort(rulesForThisState, cyclicityComparator);
                rules.put(state, rulesForThisState);

                // create tree object for each state
                Tree<String> treeForState = Tree.create("");
                treeForState.setCachingPolicy(false);
                
                tree.put(state, treeForState);

                // initialize rule indices
                currentRule.put(state, 0);
            }
        }

        @Override
        public boolean hasNext() {
            return checkHasNext(finalState);
        }

        private boolean checkHasNext(State state) {
            int currentIndex = currentRule.get(state);
            int numRules = rules.get(state).size();

            // if the state has already been advanced past its limit, 
            // it has no next tree
            if (currentIndex >= numRules) {
                return false;
            }

            // tree for this state can be advanced if the state has
            // more rules
            if (currentIndex < numRules - 1) {
                return true;
            }

            // or if one of the children of the current rule can be advanced
            for (State child : rules.get(state).get(currentIndex).getChildren()) {
                if (checkHasNext(child)) {
                    return true;
                }
            }

            // otherwise, not
            return false;
        }

        @Override
        public Tree<String> next() {
            boolean hasNext;

            if (first) {
                hasNext = first(finalState);
                first = false;
            } else {
                hasNext = next(finalState);
            }

            if (hasNext) {
                return tree.get(finalState);
            } else {
                throw new NoSuchElementException();
            }
        }

        /*
         * Initializes the data structures such that the first tree
         * of all trees that can be built from state is selected.
         */
        private boolean first(State state) {
            if (rules.get(state).isEmpty()) {
                return false;
            } else {
                currentRule.put(state, 0);

                Rule<State> current = rules.get(state).get(0);
                return initializeWithRule(state, current);
            }
        }

        /*
         * Initializes the data structures such that the next tree
         * is selected from the list of trees that can be built from state.
         * If this is not possible, the method returns false.
         */
        private boolean next(State state) {
            List<Rule<State>> rulesThisState = rules.get(state);
            int currentIndex = currentRule.get(state);
            Rule<State> current = rulesThisState.get(currentIndex);
            int n = current.getArity();
            
            // try to advance the tree for state by advancing a child
            for (int i = n - 1; i >= 0; i--) {
                if (next(current.getChildren()[i])) {
                    for (int j = i + 1; j < n; j++) {
                        first(current.getChildren()[j]);
                    }

                    return true;
                }
            }

            // if that didn't work, advance to the next rule for this state
            while (currentIndex < rulesThisState.size() - 1) {
                currentIndex++;
                currentRule.put(state, currentIndex);
                
                // if we don't manage to build a tree using the new rule,
                // skip that rule and try the next, until we run out of rules
                if( initializeWithRule(state, rulesThisState.get(currentIndex)) ) {
                    return true;
                }
            }

            // if we ran out of rules for this state, return false
            return false;
        }

        /**
         * Initializes the data structures such that the given rule
         * is used for building the tree for this state. Call this whenever
         * a new rule has been selected for a state (e.g. by modifying
         * currentRule[state]).
         * 
         * @param state
         * @param rule
         * @return 
         */
        private boolean initializeWithRule(State state, Rule<State> rule) {
            Tree<String> treeHere = tree.get(state);

            // open-heart surgery on the Tree object:
            // destructively modify the subtrees to correspond to the
            // tree objects for the child states of the rule
            List<Tree<String>> children = treeHere.getChildren();
            children.clear();
            for (State child : rule.getChildren()) {
                children.add(tree.get(child));
            }

            // also modify node label to fit with rule
            treeHere.setLabel(rule.getLabel());
            
            // call first on all subtrees to ensure that they all start
            // with their own first trees
            for (State child : rule.getChildren()) {
                if (!first(child)) {
                    return false;
                }
            }
            
            return true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("LanguageIterator does not support removal.");
        }
    }


    
    private class RuleCyclicityComparator implements Comparator<Rule<State>> {
        // return -1 iff r1 < r2
        public int compare(Rule<State> r1, Rule<State> r2) {
            boolean c1 = isCyclic(r1);
            boolean c2 = isCyclic(r2);

            if (c1 && !c2) {
                // only r1 cyclic => r2 < r1
                return 1;
            } else if (!c1 && c2) {
                // only r2 cyclic => r1 < r2
                return -1;
            } else {
                return 0;
            }
        }

        private boolean isCyclic(Rule<State> r) {
            for (int i = 0; i < r.getChildren().length; i++) {
                if (r.getChildren()[i].equals(r.getParent())) {
                    return true;
                }
            }

            return false;
        }
    }
     
}





    /*** for profiling of languageIterator:
     * 
    public static void main(String[] args) throws Exception {
        LambdaTerm geo = LambdaTermParser.parse(new StringReader("(population:i (capital:c (argmax $1 (and (state:t $1) (loc:t mississippi_river:r $1)) (size:i $1))))"));
        LambdaTermAlgebra alg = new LambdaTermAlgebra();
        BottomUpAutomaton<LambdaTerm> auto = alg.decompose(geo);

        long start = System.currentTimeMillis();
        for (Tree<String> t : auto.languageIterable()) {
        }
        long end = System.currentTimeMillis();
        System.err.println("done in " + (end - start));
    }
     */