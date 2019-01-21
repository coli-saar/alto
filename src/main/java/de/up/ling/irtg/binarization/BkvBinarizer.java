/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import com.google.common.collect.Iterables;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Implements the binarization algorithm of <a
 * href="http://www.ling.uni-potsdam.de/~koller/showpaper.php?id=binarization-13">Buechse/Koller/Vogler,
 * ACL 2013</a>.
 * <p>
 *
 * The input IRTG is binarized rule by rule. If a rule cannot be binarized, it
 * is copied verbatim to the binarized IRTG. If the algebra does not support the
 * operation symbols in the binarized rules, this rule will not be used when
 * parsing with the binarized IRTG.
 *
 * @author koller
 */
public class BkvBinarizer {

    private Map<String, RegularSeed> regularSeeds;
    private int nextGensym = 0;
    private boolean debug = false;
    private Function<InterpretedTreeAutomaton, BinaryRuleFactory> ruleFactoryFactory;

    public BkvBinarizer(Map<String, RegularSeed> regularSeeds) {
        this(regularSeeds, irtg -> new GensymBinaryRuleFactory());
    }

    public BkvBinarizer(Map<String, RegularSeed> regularSeeds, Function<InterpretedTreeAutomaton, BinaryRuleFactory> ruleFactory) {
        this.regularSeeds = regularSeeds;
        this.ruleFactoryFactory = ruleFactory;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public InterpretedTreeAutomaton binarize(InterpretedTreeAutomaton irtg, Map<String, Algebra> newAlgebras) {
        return binarize(irtg, newAlgebras, null);
    }

    public InterpretedTreeAutomaton binarize(InterpretedTreeAutomaton irtg, Map<String, Algebra> newAlgebras, ProgressListener listener) {
        // suppress info messages about intersection
        Level originalLevel = Logging.get().getLevel();
        Logging.get().setLevel(Level.WARNING);

        try {
            ConcreteTreeAutomaton<String> binarizedRtg = new ConcreteTreeAutomaton<>();
            Map<String, Homomorphism> binarizedHom = new HashMap<>();
            List<String> interpretationNames = new ArrayList<>(irtg.getInterpretations().keySet());
            TreeAutomaton rtg = irtg.getAutomaton();
            int numRules = Iterables.size(irtg.getAutomaton().getRuleSet());
            int max = (numRules < Integer.MAX_VALUE) ? numRules : Integer.MAX_VALUE;

            // initialize output homs
            for (String interp : interpretationNames) {
                Algebra alg = newAlgebras.get(interp);
                assert alg != null : "No output algebra defined for interpretation " + interp;

                binarizedHom.put(interp, new Homomorphism(binarizedRtg.getSignature(), alg.getSignature()));
            }

            // assemble output IRTG
            InterpretedTreeAutomaton binarizedIrtg = new InterpretedTreeAutomaton(binarizedRtg);
            for (String interp : interpretationNames) {
                binarizedIrtg.addInterpretation(new Interpretation(newAlgebras.get(interp), binarizedHom.get(interp), interp));
            }

            BinaryRuleFactory ruleFactory = ruleFactoryFactory.apply(binarizedIrtg);

            int ruleNumber = 1;
            for (Rule rule : irtg.getAutomaton().getRuleSet()) {
                RuleBinarization rb = binarizeRule(rule, irtg);

                if (debug) {
                    System.err.println(" -> binarization: " + rb);
                }

                if (rb == null) {
                    // unbinarizable => copy to result
                    if (debug) {
                        System.err.println(" -> unbinarizable, copy");
                    }

                    copyRule(rule, binarizedRtg, binarizedHom, irtg);
                } else {
                    // else, add binarized rule to result
                    for (String interp : interpretationNames) {
                        if (debug) {
                            System.err.println("\nmake hom for " + interp);
                        }

                        addEntriesToHomomorphism(binarizedHom.get(interp), rb.xi, rb.binarizationTerms.get(interp));
                    }

                    String newParent = addRulesToAutomaton(rule, rb.xi, irtg, binarizedIrtg, ruleFactory);

                    if (rtg.getFinalStates().contains(rule.getParent())) {
                        binarizedRtg.addFinalState(binarizedRtg.getIdForState(newParent));
                    }
                }

                if (listener != null) {
                    listener.accept(ruleNumber, max, ruleNumber + " / " + max + " rules");
                    ruleNumber++;
                }
            }

            // depending on rule factory, may have to renormalize rule weights
            binarizedRtg.normalizeRuleWeights();

            return binarizedIrtg;
        } finally {
            // make sure to restore original logging level, even if 
            // an exception occurred
            Logging.get().setLevel(originalLevel);
        }
    }

    private void copyRule(Rule rule, ConcreteTreeAutomaton<String> binarizedRtg, Map<String, Homomorphism> binarizedHom, InterpretedTreeAutomaton irtg) {
        Rule transferredRule = transferRule(rule, irtg.getAutomaton(), binarizedRtg);
        binarizedRtg.addRule(transferredRule);

        String label = transferredRule.getLabel(binarizedRtg);
        assert label.equals(rule.getLabel(irtg.getAutomaton()));

        if (irtg.getAutomaton().getFinalStates().contains(rule.getParent())) {
            binarizedRtg.addFinalState(transferredRule.getParent());
        }

        for (String interp : irtg.getInterpretations().keySet()) {
            Homomorphism outputHom = binarizedHom.get(interp);
            Homomorphism inputHom = irtg.getInterpretation(interp).getHomomorphism();
            Tree<String> term = inputHom.get(label);

            outputHom.add(label, term);

//            binarizedHom.get(interp).add(transferredRule.getLabel(), irtg.getInterpretations().get(interp).getHomomorphism().get(rule.getLabel()));
        }

        if (debug) {
            System.err.println("\ncopied rule:");
            System.err.println("  " + transferredRule.toString(binarizedRtg));
            for (String interp : irtg.getInterpretations().keySet()) {
                Homomorphism hom = binarizedHom.get(interp);
                System.err.println("  [" + interp + "] " + HomomorphismSymbol.toStringTree(hom.get(transferredRule.getLabel()), hom.getTargetSignature()));
            }
        }
    }

    // inserts rules with fresh states into the binarized RTG
    // for generating q -> xi(q1,...,qk)
    private String addRulesToAutomaton(Rule originalRule, final Tree<String> vartree, InterpretedTreeAutomaton irtg, InterpretedTreeAutomaton binarizedIrtg, BinaryRuleFactory binarizedRuleFactory) {
        TreeAutomaton rtg = irtg.getAutomaton();
        ConcreteTreeAutomaton<String> binarizedRtg = (ConcreteTreeAutomaton<String>) binarizedIrtg.getAutomaton();
        
        return dfsWithPaths(vartree, (tree, node, childrenValues, path) -> {
            if (childrenValues.isEmpty() && NUMBER_PATTERN.matcher(node.getLabel()).matches()) {
                    int var = Integer.parseInt(node.getLabel());
                    return rtg.getStateForId(originalRule.getChildren()[var]).toString();
                } else {
                    Rule newRule = binarizedRuleFactory.generateBinarizedRule(node, childrenValues, path, originalRule, vartree, irtg, binarizedIrtg);
                    binarizedRtg.addRule(newRule);
                    return binarizedRtg.getStateForId(newRule.getParent());
                }
        });
    }
    
    public interface TreeWithPathVisitor<E,F> {
        E combine(Tree<F> tree, Tree<F> node, List<E> childrenValues, String path);
    }

    private static <E,F> E dfsWithPaths(Tree<F> tree, TreeWithPathVisitor<E,F> visitor) {
        return dfsWithPaths(tree, tree, visitor, "");
    }
    
    private static <E,F> E dfsWithPaths(Tree<F> tree, Tree<F> node, TreeWithPathVisitor<E,F> visitor, String path) {
        List<E> childrenValues = new ArrayList<>();
        
        for( int i = 0; i < node.getChildren().size(); i++ ) {
            String pathToChild = path.equals("") ? Integer.toString(i) : (path + "_" + i);
            childrenValues.add(dfsWithPaths(tree, node.getChildren().get(i), visitor, pathToChild));
        }
        
        return visitor.combine(tree, node, childrenValues, path);
    }

    RuleBinarization binarizeRule(Rule rule, InterpretedTreeAutomaton irtg) {
        try {
            TreeAutomaton commonVariableTrees = null;
            Map<String, TreeAutomaton<String>> binarizationTermsPerInterpretation = new HashMap<>();
            Map<String, Int2ObjectMap<IntSet>> varPerInterpretation = new HashMap<>();
            RuleBinarization ret = new RuleBinarization();

            if (debug) {
                System.err.println("\n\n*** BINARIZE " + rule.toString(irtg.getAutomaton()) + " ***");
            }

            for (String interpretation : irtg.getInterpretations().keySet()) {
                if (debug) {
                    System.err.println("\ninterpretation " + interpretation + ":");
                    System.err.println(" - algebra signature = " + irtg.getInterpretation(interpretation).getAlgebra().getSignature());
                    System.err.println(" - hom target signature = " + irtg.getInterpretation(interpretation).getHomomorphism().getTargetSignature());

                }
                String label = irtg.getAutomaton().getSignature().resolveSymbolId(rule.getLabel());            // this is alpha from the paper
                Tree<String> rhs = irtg.getInterpretation(interpretation).getHomomorphism().get(label);        // this is h_i(alpha)

                TreeAutomaton<String> binarizationTermsHere = regularSeeds.get(interpretation).binarize(rhs);  // this is G_i

                binarizationTermsPerInterpretation.put(interpretation, binarizationTermsHere);

                if (debug) {
                    System.err.println("\nG(" + interpretation + "):\n" + binarizationTermsHere);
                }

                if (debug) {
                    for (Tree t : binarizationTermsHere.language()) {
                        System.err.println("   " + t);
                    }
                }

                Int2ObjectMap<IntSet> varHere = computeVar(binarizationTermsHere);                             // this is var_i
                varPerInterpretation.put(interpretation, varHere);

                if (debug) {
                    System.err.println("\nvars(" + interpretation + "):\n");
                    for (int q : varHere.keySet()) {
                        System.err.println(binarizationTermsHere.getStateForId(q) + " -> " + varHere.get(q));
                    }
                }

                TreeAutomaton<IntSet> variableTrees = vartreesForAutomaton(binarizationTermsHere, varHere);    // this is G'_i  (accepts variable trees)
                if (debug) {
                    System.err.println("\nG'(" + interpretation + "):\n" + variableTrees);
                }

                if (debug) {
                    for (Tree t : variableTrees.language()) {
                        System.err.println("   " + t);
                    }
                }

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

            if (debug) {
                System.err.println("\nvartree = " + commonVariableTree);
            }
            if (debug) {
                System.err.println("xi = " + ret.xi);
            }

            for (String interpretation : irtg.getInterpretations().keySet()) {
                // this is G''_i
                TreeAutomaton binarizationsForThisVartree = binarizationsForVartree(binarizationTermsPerInterpretation.get(interpretation), commonVariableTree, varPerInterpretation.get(interpretation));
                if (debug) {
                    System.err.println("\nG''(" + interpretation + "):\n" + binarizationsForThisVartree);
                }

                Tree<String> binarization = binarizationsForThisVartree.viterbi();
                if (debug) {
                    System.err.println("\nbin(" + interpretation + "):\n" + binarization);
                }

                ret.binarizationTerms.put(interpretation, binarization);
            }

            return ret;
        } catch (RuntimeException e) {
            throw new RuntimeException("Error while binarizing rule " + rule.toString(irtg.getAutomaton()), e);
        }

    }

    private Tree<String> xiFromVartree(Tree<String> vartree, final String originalLabel) {
        //special case for vartree from unary rules, we need to make sure that
        // we do not confuse them with the leafs of larger variable trees
        if (vartree.getChildren().isEmpty() && isNumber(vartree.getLabel())) {
            return Tree.create(gensym(originalLabel + "_br"), vartree);
        } else {
            return vartree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
                @Override
                public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                    if (childrenValues.isEmpty() && isNumber(node.getLabel())) {
                        return node;
                    } else {
                        return Tree.create(gensym(originalLabel + "_br"), childrenValues);
                    }
                }
            });
        }
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
        ConcreteTreeAutomaton<IntSet> ret = new ConcreteTreeAutomaton<>();

        // iterate over the rules of G_i
        for (Rule rule : automaton.getRuleSet()) {
            Rule newRule = null;

            // nullary rules: make rules {} -> {} (for constants) or {0} -> 0 (for variables)
            if (rule.getArity() == 0) {
                String label = automaton.getSignature().resolveSymbolId(rule.getLabel());
                IntSet is = new IntOpenHashSet();

                if (HomomorphismSymbol.isVariableSymbol(label)) {
                    int var = HomomorphismSymbol.getVariableIndex(label);
                    is.add(var);
                }

                newRule = ret.createRule(is, representVarSet(is), new ArrayList<>());
            } else {
                List<IntSet> rhsVarsets = new ArrayList<>();

                // compute fork
                // this work is repeated in (iv); if it becomes a performance issue, cache results
                for (int i = 0; i < rule.getArity(); i++) {
                    IntSet varset = getVars(vars, rule.getChildren()[i]);

                    if (!varset.isEmpty()) {
                        rhsVarsets.add(varset);
                    }
                }

                // if rule has two children with non-empty varsets, make rule
                if (rhsVarsets.size() >= 2) {
                    Collections.sort(rhsVarsets, new IntSetComparator());

                    IntSet parentSet = vars.get(rule.getParent());
                    newRule = ret.createRule(parentSet, representVarSet(parentSet), rhsVarsets);

                }
            }

            if (newRule != null) {
                ret.addRule(newRule);
            } else {
            }
        }

        // if the state vars(q) was added to the vartree automaton for some
        // final state q of G, then make vars(q) a final state in G'
        for (int finalState : automaton.getFinalStates()) {
            IntSet v = vars.get(finalState);
            int q = ret.getIdForState(v);

            if (q > 0) {
                ret.addFinalState(q);
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

        @Override
        public int compare(IntSet o1, IntSet o2) {
            int min1 = o1.isEmpty() ? Integer.MIN_VALUE : Collections.min(o1);
            int min2 = o2.isEmpty() ? Integer.MIN_VALUE : Collections.min(o2);
            return Integer.compare(min1,min2);
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
            String v = "null";

            if (varsConstruction != null) {
                v = representVarSets(varsConstruction);
            }

            return tree + "@" + vars + "=" + v;
        }
    }

    private static boolean isNumber(String s) {
        return NUMBER_PATTERN.matcher(s).matches();
    }

    // add mappings to hom that assign suitable parts of the binarization term to the new labels in xi
    // binarizationTerm: *('?3',*(a,*('?1','?2')))
    // xi: _br1(_br0('0','1'),'2')
    // -> hom is _br0 -> *(a, *(?1,?2)), _br1 -> *(?2,?1)
    void addEntriesToHomomorphism(final Homomorphism hom, Tree<String> xi, Tree<String> binarizationTerm) {
        if (debug) {
            System.err.println("makehom: " + binarizationTerm + " along " + xi);
        }

        try {
            hom.getTargetSignature().addAllSymbols(binarizationTerm);
            hom.getSourceSignature().addAllSymbols(xi);
        } catch (Exception e) {
            System.err.println(binarizationTerm);
            System.err.println(hom.getTargetSignature());
            System.exit(1);
        }

        final Map<String, String> labelForFork = new HashMap<>();  // 0+1 -> _br0, 0_1+2 -> _br1
        xi.dfs(new TreeVisitor<String, Void, IntSet>() {
            @Override
            public IntSet combine(Tree<String> node, List<IntSet> childrenValues) {
                IntSet here = new IntOpenHashSet();

                if (node.getChildren().isEmpty() && isNumber(node.getLabel())) {
                    // variable leaf of vartree => node label is a number
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

        Subtree subtreeForRoot;
        subtreeForRoot = binarizationTerm.dfs(new TreeVisitor<String, Void, Subtree>() {
            @Override
            public Subtree combine(Tree<String> node, final List<Subtree> childrenValues) {
                IntSet is = new IntOpenHashSet();
                List<IntSet> childrenVarSets = new ArrayList<>();
                List<Tree<String>> childrenTrees = new ArrayList<>();
                List<IntSet> nonemptyChildConstruction = new ArrayList<>();
                int childrenWithNonemptyVarsets = 0;
                Subtree ret;

                if (childrenValues.isEmpty()) {
                    if (HomomorphismSymbol.isVariableSymbol(node.getLabel())) {
                        is.add(HomomorphismSymbol.getVariableIndex(node.getLabel()));
                        ret = new Subtree(Tree.create("?1"), is, new ArrayList<>());
                    } else {
                        ret = new Subtree(node, is, new ArrayList<>());
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
                        List<IntSet> orderedChildrenVarSets = new ArrayList<>(childrenVarSets);
                        Collections.sort(orderedChildrenVarSets, new IntSetComparator());

                        List<Tree<String>> subtrees = new ArrayList<>();

                        for (int i = 0; i < childrenTrees.size(); i++) {
                            IntSet childVarSetHere = childrenVarSets.get(i);

                            if (childVarSetHere.isEmpty()) {
                                // child contains 0 variables => simply copy subtree
                                subtrees.add(childrenTrees.get(i));
                            } else if (childrenValues.get(i).varsConstruction.isEmpty()) {
                                // child contains 1 variable (childVarset != 0, but no construction from smaller pieces recorded)
                                // => rename the ?1 in the child to the correct variable position
                                final int varNum = orderedChildrenVarSets.indexOf(childVarSetHere);

                                subtrees.add(childrenTrees.get(i).substitute(st -> {
                                    if (st.getLabel().equals("?1")) {
                                        return Tree.create("?" + (varNum + 1));
                                    } else {
                                        return null;
                                    }
                                }));

//                                        new com.google.common.base.Function<Tree<String>, Tree<String>>() {
//                                    public Tree<String> apply(Tree<String> st) {
//                                        if (st.getLabel().equals("?1")) {
//                                            return Tree.create("?" + (varNum + 1));
//                                        } else {
//                                            return null;
//                                        }
//                                    }
//                                }));
                            } else {
                                // child contains >= 2 variables (and construction from smaller pieces recorded)
                                // => record this child as the homomorphic value of this construction, and replace by ?i
                                String label = labelForFork.get(representVarSets(childrenValues.get(i).varsConstruction));

                                if (label != null) {
                                    hom.add(label, childrenTrees.get(i));
//                                    log("add hom: " + label + " -> " + childrenTrees.get(i));
                                }

                                int varNum = orderedChildrenVarSets.indexOf(childVarSetHere);
                                subtrees.add(Tree.create("?" + (varNum + 1)));
                            }
                        }

                        ret = new Subtree(Tree.create(node.getLabel(), subtrees), is, childrenVarSets);
                    }
                }

                if (debug) {
                    System.err.println("return: " + node + " -> " + ret);
                }
                return ret;
            }
        });

        hom.add(xi.getLabel(), subtreeForRoot.tree);

        if (debug) {
            System.err.println("add hom (r): " + xi.getLabel() + " -> " + subtreeForRoot.tree);
        }

        if (debug) {
            System.err.println("hom is: " + hom);
        }
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
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<>();
        Set<String> forksInVartree = collectForks(commonVariableTree);

        for (Rule rule : binarizations.getRuleSet()) {
            List<IntSet> rhsVarsets = new ArrayList<>();

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

        Rule ret = toAutomaton.createRule(fromAutomaton.getStateForId(oldRule.getParent()), fromAutomaton.getSignature().resolveSymbolId(oldRule.getLabel()), ruleRhs, oldRule.getWeight());
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
            binarizationTerms = new HashMap<>();
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
        Int2ObjectMap<IntSet> ret = new Int2ObjectOpenHashMap<>();

        stateLoop:
        for (Integer state : (List<Integer>) auto.getStatesInBottomUpOrder()) {
            IntIterable labelsForState = auto.getLabelsTopDown(state);

            for (int label : labelsForState) {
                for (Rule rule : (Iterable<Rule>) auto.getRulesTopDown(label, state)) {
                    String labelString = auto.getSignature().resolveSymbolId(label);
                    IntSet s = new IntOpenHashSet();

                    if (HomomorphismSymbol.isVariableSymbol(labelString)) {
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
        List<IntSet> vssList = new ArrayList<>(vss);
        Collections.sort(vssList, new IntSetComparator());

        SortedSet<String> reprs = new TreeSet<>();

        for (IntSet vs : vssList) {
            if (!vs.isEmpty()) {
                reprs.add(representVarSet(vs));
            }
        }

        return StringTools.join(reprs, "+");
    }

    /**
     *
     */
    private static Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    // Collects the set of string representations of the forks in the
    // given variable tree. A fork is a binary construction 0_1+2_3, which
    // happens when two children of a node have disjoint variable sets.
    static Set<String> collectForks(Tree<String> vartree) {
        final Set<String> ret = new HashSet<>();

        vartree.dfs(new TreeVisitor<String, Void, IntSet>() {
            @Override
            public IntSet combine(Tree<String> node, List<IntSet> childrenValues) {
                IntSet here = new IntOpenHashSet();

                if (node.getChildren().isEmpty()) {
                    // leaf of vartree
                    if (NUMBER_PATTERN.matcher(node.getLabel()).matches()) {
                        // node label is a number => this is variable index, collect it
                        ret.add(node.getLabel());
                        here.add(Integer.parseInt(node.getLabel()));
                    } else {
                        // otherwise, just return empty set
                    }
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
