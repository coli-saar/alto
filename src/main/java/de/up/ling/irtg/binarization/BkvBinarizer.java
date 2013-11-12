/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import com.google.common.base.Function;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implements the binarization algorithm of Buechse/Koller/Vogler 2013.
 * See the following paper for details:
 * http://www.ling.uni-potsdam.de/~koller/showpaper.php?id=binarization-13
 *
 * @author koller
 */
public class BkvBinarizer {
    private Map<String, RegularSeed> regularSeeds;
    private int nextGensym = 0;
    private boolean debug = false;

    public BkvBinarizer(Map<String, RegularSeed> regularSeeds) {
        this.regularSeeds = regularSeeds;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private void log(String str) {
        if (debug) {
            System.err.println(str);
        }
    }

    public InterpretedTreeAutomaton binarize(InterpretedTreeAutomaton irtg) {
        ConcreteTreeAutomaton<String> binarizedRtg = new ConcreteTreeAutomaton<String>();
        Map<String, Homomorphism> binarizedHom = new HashMap<String, Homomorphism>();
        List<String> interpretationNames = new ArrayList<String>(irtg.getInterpretations().keySet());
        TreeAutomaton rtg = irtg.getAutomaton();

        // initialize output homs
        for (String interp : interpretationNames) {
            Homomorphism oldHom = irtg.getInterpretations().get(interp).getHomomorphism();
            binarizedHom.put(interp, new Homomorphism(binarizedRtg.getSignature(), oldHom.getTargetSignature()));
        }

        for (Rule rule : irtg.getAutomaton().getRuleSet()) {
            log("\n\n\nbinarizing rule: " + rule.toString(irtg.getAutomaton()));

            if (rule.getArity() <= 2) {
                // rules of low arity => simply copy these to result
                log(" -> low arity, copy");
                copyRule(rule, binarizedRtg, binarizedHom, irtg);
            } else {
                // rules of higher arity => binarize
                RuleBinarization rb = binarizeRule(rule, irtg);

                log(" -> binarization: " + rb);

                if (rb == null) {
                    // unbinarizable => copy to result
                    log(" -> unbinarizable, copy");
                    copyRule(rule, binarizedRtg, binarizedHom, irtg);
                } else {
                    // else, add binarized rule to result
                    String[] childStates = new String[rule.getArity()];
                    for (int i = 0; i < rule.getArity(); i++) {
                        childStates[i] = rtg.getStateForId(rule.getChildren()[i]).toString();
                    }

                    Object parent = rtg.getStateForId(rule.getParent());
                    String newParent = addRulesToAutomaton(binarizedRtg, rb.xi, parent.toString(), childStates);

                    if (rtg.getFinalStates().contains(rule.getParent())) {
                        binarizedRtg.addFinalState(binarizedRtg.getIdForState(newParent));
                    }

                    for (String interp : interpretationNames) {
                        log("\nmake hom for " + interp);
                        addEntriesToHomomorphism(binarizedHom.get(interp), rb.xi, rb.binarizationTerms.get(interp));
                    }
                }
            }
        }

        // assemble output IRTG
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(binarizedRtg);
        for (String interp : interpretationNames) {
            ret.addInterpretation(interp, new Interpretation(irtg.getInterpretations().get(interp).getAlgebra(), binarizedHom.get(interp)));
        }

        return ret;
    }

    private void copyRule(Rule rule, ConcreteTreeAutomaton<String> binarizedRtg, Map<String, Homomorphism> binarizedHom, InterpretedTreeAutomaton irtg) {
        Rule transferredRule = transferRule(rule, irtg.getAutomaton(), binarizedRtg);
        binarizedRtg.addRule(transferredRule);

        for (String interp : irtg.getInterpretations().keySet()) {
            binarizedHom.get(interp).add(transferredRule.getLabel(), irtg.getInterpretations().get(interp).getHomomorphism().get(rule.getLabel()));
        }
    }

    // inserts rules with fresh states into the binarized RTG
    // for generating q -> xi(q1,...,qk)
    private String addRulesToAutomaton(final ConcreteTreeAutomaton binarizedRtg, final Tree<String> vartree, final String oldRuleParent, final String[] oldRuleChildren) {
        return vartree.dfs(new TreeVisitor<String, Void, String>() {
            @Override
            public String combine(Tree<String> node, List<String> childrenValues) {
                if (childrenValues.isEmpty()) {
                    int var = Integer.parseInt(node.getLabel());
                    return oldRuleChildren[var];
                } else {
                    String parent;

                    if (node == vartree) {
                        parent = oldRuleParent;
                    } else {
                        parent = gensym("q");
                    }

                    Rule newRule = binarizedRtg.createRule(parent, node.getLabel(), childrenValues);
                    binarizedRtg.addRule(newRule);

                    return parent;
                }
            }
        });
    }

    RuleBinarization binarizeRule(Rule rule, InterpretedTreeAutomaton irtg) {
        TreeAutomaton commonVariableTrees = null;
        Map<String, TreeAutomaton<String>> binarizationTermsPerInterpretation = new HashMap<String, TreeAutomaton<String>>();
        Map<String, Int2ObjectMap<IntSet>> varPerInterpretation = new HashMap<String, Int2ObjectMap<IntSet>>();
        RuleBinarization ret = new RuleBinarization();

        for (String interpretation : irtg.getInterpretations().keySet()) {
            String label = irtg.getAutomaton().getSignature().resolveSymbolId(rule.getLabel());            // this is alpha from the paper
            Tree<String> rhs = irtg.getInterpretation(interpretation).getHomomorphism().get(label);        // this is h_i(alpha)

            TreeAutomaton<String> binarizationTermsHere = regularSeeds.get(interpretation).binarize(rhs);  // this is G_i
            binarizationTermsPerInterpretation.put(interpretation, binarizationTermsHere);

            Int2ObjectMap<IntSet> varHere = computeVar(binarizationTermsHere);                             // this is var_i
            varPerInterpretation.put(interpretation, varHere);

            TreeAutomaton<IntSet> variableTrees = vartreesForAutomaton(binarizationTermsHere, varHere);    // this is G'_i  (accepts variable trees)

            if (commonVariableTrees == null) {
                commonVariableTrees = variableTrees;
            } else {
                commonVariableTrees = commonVariableTrees.intersect(variableTrees);
            }

            if (commonVariableTrees.isEmpty()) {
                return null;
            }
        }

        assert commonVariableTrees != null;

        Tree<String> commonVariableTree = commonVariableTrees.viterbi();                                   // this is tau, some vartree they all have in common
        ret.xi = xiFromVartree(commonVariableTree, irtg.getAutomaton().getSignature().resolveSymbolId(rule.getLabel()));

        for (String interpretation : irtg.getInterpretations().keySet()) {
            // this is G''_i
            TreeAutomaton binarizationsForThisVartree = binarizationsForVartree(binarizationTermsPerInterpretation.get(interpretation), commonVariableTree, varPerInterpretation.get(interpretation));
            Tree<String> binarization = binarizationsForThisVartree.viterbi();

            ret.binarizationTerms.put(interpretation, binarization);
        }

        return ret;
    }

    private Tree<String> xiFromVartree(Tree<String> vartree, final String originalLabel) {
        return vartree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return node;
                } else {
                    return Tree.create(gensym(originalLabel + "_br"), childrenValues);
                }
            }
        });
    }

    /**
     * **********************************************************************************
     *
     * Step (ii) of the algorithm: Compute G'_i from G_i. G'_i accepts language
     * of variable trees of the trees in L(G_i).
     *
     ***********************************************************************************
     */
    // step (ii) of the algorithm: construct G'_i from G_i and vars_i
    // The automata computed by this method look like this:
    // '{2}' -> '2' [1.0]
    // '{1, 0}' -> '0_1'('{0}', '{1}') [1.0]
    // '{1}' -> '1' [1.0]
    // '{1, 2, 0}'! -> '0_1_2'('{1, 0}', '{2}') [1.0]
    // '{1, 2, 0}'! -> '0_1_2'('{0}', '{1, 2}') [1.0]
    // '{0}' -> '0' [1.0]
    // '{1, 2}' -> '1_2'('{1}', '{2}') [1.0]
    static TreeAutomaton<IntSet> vartreesForAutomaton(TreeAutomaton<String> automaton, Int2ObjectMap<IntSet> vars) {
        ConcreteTreeAutomaton<IntSet> ret = new ConcreteTreeAutomaton<IntSet>();

        for (Rule rule : automaton.getRuleSet()) {
            if (rule.getArity() == 0) {
                String label = automaton.getSignature().resolveSymbolId(rule.getLabel());

                if (label.startsWith("?")) {
                    int var = HomomorphismSymbol.getVariableIndex(label);

                    IntSet is = new IntOpenHashSet();
                    is.add(var);

                    Rule newRule = ret.createRule(is, representVarSet(is), new ArrayList<IntSet>());
                    ret.addRule(newRule);
                }
            } else {
                List<IntSet> rhsVarsets = new ArrayList<IntSet>();

                // this work is repeated in (iv); if it becomes a performance issue, cache results
                for (int i = 0; i < rule.getArity(); i++) {
                    IntSet varset = getVars(vars, rule.getChildren()[i]);

                    if (!varset.isEmpty()) {
                        rhsVarsets.add(varset);
                    }
                }

                if (rhsVarsets.size() >= 2) {
                    Collections.sort(rhsVarsets, new IntSetComparator());

                    IntSet parentSet = vars.get(rule.getParent());
                    Rule newRule = ret.createRule(parentSet, representVarSet(parentSet), rhsVarsets);
                    ret.addRule(newRule);

                    if (automaton.getFinalStates().contains(rule.getParent())) {
                        ret.addFinalState(newRule.getParent());
                    }
                }
            }
        }
        return ret;
    }

    private static IntSet getVars(Int2ObjectMap<IntSet> vars, int state) {
        IntSet ret = vars.get(state);

        if (ret == null) {
            return new IntOpenHashSet();
        } else {
            return ret;
        }
    }

    private static class IntSetComparator implements Comparator<IntSet> {
        public int compare(IntSet o1, IntSet o2) {
            return representVarSet(o1).compareTo(representVarSet(o2));
        }
    }

    private static class Subtree {
        public Tree<String> tree;
        public IntSet vars;
        public List<IntSet> varsConstruction;

        public Subtree(Tree<String> tree, IntSet is, List<IntSet> varsConstruction) {
            this.tree = tree;
            this.vars = is;
            this.varsConstruction = varsConstruction;
        }

        @Override
        public String toString() {
            return tree + "@" + vars + "=" + representVarSets(varsConstruction);
        }
    }

    // add mappings to hom that assign suitable parts of the binarization term to the new labels in xi
    // binarizationTerm: *('?3',*(a,*('?1','?2')))
    // xi: _br1(_br0('0','1'),'2')
    // -> hom is _br0 -> *(a, *(?1,?2)), _br1 -> *(?2,?1)
    void addEntriesToHomomorphism(final Homomorphism hom, Tree<String> xi, Tree<String> binarizationTerm) {
        log("makehom: " + binarizationTerm + " along " + xi);

        hom.getTargetSignature().addAllSymbols(binarizationTerm);

        final Map<String, String> labelForFork = new HashMap<String, String>();  // 0+1 -> _br0, 0_1+2 -> _br1
        xi.dfs(new TreeVisitor<String, Void, IntSet>() {
            @Override
            public IntSet combine(Tree<String> node, List<IntSet> childrenValues) {
                IntSet here = new IntOpenHashSet();

                if (node.getChildren().isEmpty()) {
                    // leaf of vartree => node label is a number
                    here.add(Integer.parseInt(node.getLabel()));
                } else {
                    labelForFork.put(representVarSets(childrenValues), node.getLabel());

                    for (IntSet is : childrenValues) {
                        here.addAll(is);
                    }
                }

                return here;
            }
        });

        Subtree subtreeForRoot = binarizationTerm.dfs(new TreeVisitor<String, Void, Subtree>() {
            @Override
            public Subtree combine(Tree<String> node, final List<Subtree> childrenValues) {
                IntSet is = new IntOpenHashSet();
                List<IntSet> childrenVarSets = new ArrayList<IntSet>();
                List<Tree<String>> childrenTrees = new ArrayList<Tree<String>>();
                List<IntSet> nonemptyChildConstruction = null;
                int childrenWithNonemptyVarsets = 0;
                Subtree ret;

                if (childrenValues.isEmpty()) {
                    if (node.getLabel().startsWith("?")) {
                        is.add(HomomorphismSymbol.getVariableIndex(node.getLabel()));
                        ret = new Subtree(Tree.create("?1"), is, new ArrayList<IntSet>());
                    } else {
                        ret = new Subtree(node, is, new ArrayList<IntSet>());
                    }
                } else {
                    for (Subtree st : childrenValues) {
                        is.addAll(st.vars);

                        childrenTrees.add(st.tree);
                        childrenVarSets.add(st.vars);

                        if (!st.vars.isEmpty()) {
                            childrenWithNonemptyVarsets++;
                            nonemptyChildConstruction = st.varsConstruction;
                        }
                    }

                    assert childrenWithNonemptyVarsets <= 2;

                    if (childrenWithNonemptyVarsets < 2) {
                        ret = new Subtree(Tree.create(node.getLabel(), childrenTrees), is, nonemptyChildConstruction);
                    } else {
                        List<IntSet> orderedChildrenVarSets = new ArrayList<IntSet>(childrenVarSets);
                        Collections.sort(orderedChildrenVarSets, new IntSetComparator());

                        List<Tree<String>> subtrees = new ArrayList<Tree<String>>();

                        for (int i = 0; i < childrenTrees.size(); i++) {
                            IntSet childVarSetHere = childrenVarSets.get(i);

                            if (childVarSetHere.isEmpty()) {
                                // child contains 0 variables => simply copy subtree
                                subtrees.add(childrenTrees.get(i));
                            } else if (childrenValues.get(i).varsConstruction.isEmpty()) {
                                // child contains 1 variable (childVarset != 0, but no construction from smaller pieces recorded)
                                // => rename the ?1 in the child to the correct variable position
                                final int varNum = orderedChildrenVarSets.indexOf(childVarSetHere);

                                subtrees.add(childrenTrees.get(i).substitute(new Function<Tree<String>, Tree<String>>() {
                                    public Tree<String> apply(Tree<String> st) {
                                        if (st.getLabel().equals("?1")) {
                                            return Tree.create("?" + (varNum + 1));
                                        } else {
                                            return null;
                                        }
                                    }
                                }));
                            } else {
                                // child contains >= 2 variables (and construction from smaller pieces recorded)
                                // => record this child as the homomorphic value of this construction, and replace by ?i
                                String label = labelForFork.get(representVarSets(childrenValues.get(i).varsConstruction));

                                if (label != null) {
                                    hom.add(label, childrenTrees.get(i));
                                    log("add hom: " + label + " -> " + childrenTrees.get(i));
                                }

                                int varNum = orderedChildrenVarSets.indexOf(childVarSetHere);
                                subtrees.add(Tree.create("?" + (varNum + 1)));
                            }
                        }

                        ret = new Subtree(Tree.create(node.getLabel(), subtrees), is, childrenVarSets);
                    }
                }

                log("return: " + node + " -> " + ret);
                return ret;
            }
        });

        hom.add(xi.getLabel(), subtreeForRoot.tree);
        log("add hom (r): " + xi.getLabel() + " -> " + subtreeForRoot.tree);

        log("hom is: " + hom);
    }

    /**
     * **********************************************************************************
     *
     * Step (iv) of the algorithm: Compute G''_i from G'_i, var_i, and tau.
     * G''_i accepts the binarization trees that are consistent with tau.
     *
     ***********************************************************************************
     */
    // step (iv) of the algorithm: compute G''_i from G_i, var_i, and tau
    static TreeAutomaton<String> binarizationsForVartree(TreeAutomaton<String> binarizations, Tree<String> commonVariableTree, Int2ObjectMap<IntSet> var) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        Set<String> forksInVartree = collectForks(commonVariableTree);

        for (Rule rule : binarizations.getRuleSet()) {
            List<IntSet> rhsVarsets = new ArrayList<IntSet>();

            for (int i = 0; i < rule.getArity(); i++) {
                IntSet varset = var.get(rule.getChildren()[i]);

                if (!varset.isEmpty()) {
                    rhsVarsets.add(varset);
                }
            }

            if (rhsVarsets.size() < 2 || forksInVartree.contains(representVarSets(rhsVarsets))) {
                Rule newRule = transferRule(rule, binarizations, ret);
                ret.addRule(newRule);

                if (binarizations.getFinalStates().contains(rule.getParent())) {
                    ret.addFinalState(newRule.getParent());
                }
            }
        }

        return ret;
    }


    // transfer rule from one automaton to another, and return the rule in the new automaton
    private static <E> Rule transferRule(Rule oldRule, TreeAutomaton<E> fromAutomaton, TreeAutomaton<E> toAutomaton) {
        E[] ruleRhs = (E[]) new Object[oldRule.getArity()];

        for (int i = 0; i < oldRule.getArity(); i++) {
            ruleRhs[i] = fromAutomaton.getStateForId(oldRule.getChildren()[i]);
        }

        Rule ret = toAutomaton.createRule(fromAutomaton.getStateForId(oldRule.getParent()), fromAutomaton.getSignature().resolveSymbolId(oldRule.getLabel()), ruleRhs);
        return ret;
    }

    // The templace for the binarization of a rule, consisting of a relabeled
    // variable tree xi and binarization terms for each interpretation that are
    // consistent (in their variable bracketing behavior) with xi.
    // xi is a tree of the form _rb1(_rb0(0,1),2).
    private static class RuleBinarization {
        Tree<String> xi;
        Map<String, Tree<String>> binarizationTerms;

        public RuleBinarization() {
            binarizationTerms = new HashMap<String, Tree<String>>();
        }

        @Override
        public String toString() {
            return "<" + xi + " " + binarizationTerms + ">";
        }
    }

    private String gensym(String prefix) {
        return prefix + (nextGensym++);
    }
    
    
    
    
    /**
     * **********************************************************************************
     *
     * Methods for constructing the output automaton and homomorphisms.
     *
     ***********************************************************************************
     */
    
    
    /**
     * **********************************************************************************
     *
     * Methods for constructing, comparing, managing forks.
     *
     ***********************************************************************************
     */
    
    
    // Computes the var-map of a tree automaton. The var-map maps a state q
    // of the automaton to the set of all variable numbers that are contained
    // in trees that can be derived from q. As explained in BKV, this variable set
    // is unique if the automaton guarantees that in every tree of the language,
    // every variable from 0,...,k-1 occurs exactly once. The implementation
    // assumes this uniqueness, but does not check it.
    static Int2ObjectMap<IntSet> computeVar(TreeAutomaton auto) {
        Int2ObjectMap<IntSet> ret = new Int2ObjectOpenHashMap<IntSet>();

        stateLoop:
        for (Integer state : (List<Integer>) auto.getStatesInBottomUpOrder()) {
            Collection<Integer> labelsForState = auto.getLabelsTopDown(state);

            for (int label : labelsForState) {
                for (Rule rule : (Set<Rule>) auto.getRulesTopDown(label, state)) {
                    String labelString = auto.getSignature().resolveSymbolId(label);
                    IntSet s = new IntOpenHashSet();

                    if (labelString.startsWith("?")) {
                        s.add(HomomorphismSymbol.getVariableIndex(labelString));
                    } else {
                        for (int childState : rule.getChildren()) {
                            s.addAll(ret.get(childState));
                        }
                    }

                    ret.put(state, s);
                    continue stateLoop;
                }
            }
        }

        return ret;
    }
    
    // map IntSet {0,2,1} to string 0_1_2; 
    // unique (sorted) string representation is guaranteed for equal IntSets
    private static String representVarSet(IntSet vs) {
        int[] vars = vs.toIntArray();
        Arrays.sort(vars);

        StringBuilder buf = new StringBuilder();
        boolean first = true;

        for (int i = 0; i < vars.length; i++) {
            if (first) {
                first = false;
            } else {
                buf.append("_");
            }

            buf.append(vars[i]);
        }

        return buf.toString();
    }

    // map collection of IntSets <{0,2,1}, {3}> to string 0_1_2+3
    // +-separated parts of the collection are sorted ascending as strings,
    // i.e. unique representation for any equals Collection<IntSet>
    private static String representVarSets(Collection<IntSet> vss) {
        SortedSet<String> reprs = new TreeSet<String>();

        for (IntSet vs : vss) {
            if (!vs.isEmpty()) {
                reprs.add(representVarSet(vs));
            }
        }

        return StringTools.join(reprs, "+");
    }

    // Collects the set of string representations of the forks in the
    // given variable tree. A fork is a binary construction 0_1+2_3, which
    // happens when two children of a node have disjoint variable sets.
    static Set<String> collectForks(Tree<String> vartree) {
        final Set<String> ret = new HashSet<String>();

        vartree.dfs(new TreeVisitor<String, Void, IntSet>() {
            @Override
            public IntSet combine(Tree<String> node, List<IntSet> childrenValues) {
                IntSet here = new IntOpenHashSet();

                if (node.getChildren().isEmpty()) {
                    // leaf of vartree => node label is a number
                    ret.add(node.getLabel());
                    here.add(Integer.parseInt(node.getLabel()));
                } else {
                    ret.add(representVarSets(childrenValues));

                    for (IntSet is : childrenValues) {
                        here.addAll(is);
                    }
                }

                return here;
            }
        });

        return ret;
    }
}
