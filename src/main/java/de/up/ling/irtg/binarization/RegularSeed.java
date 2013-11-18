/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class RegularSeed {
    private Map<String, TreeAutomaton> binarizationCache = new HashMap<String, TreeAutomaton>();

    /**
     * Computes an automaton for all binarizations of a given symbol. If f is a
     * k-place symbol, then each binarization is a term that contains each
     * variable symbol ?1, ..., ?k exactly once. The intended use of these
     * automata is that every term accepted by the automaton evaluates to the
     * same function over some given algebra as the original k-place symbol (but
     * expresses this function using symbols of lower arity).<p>
     *
     * The automaton returned by this method must satisfy a number of
     * requirements.
     * <ul>
     * <li> The automaton has exactly one final state.</li>
     * <li> There is no state that expands both into a variable (i.e. there is a
     * rule q -&gt; ?i) and into a terminal symbol (i.e. there is a rule q -&gt;
     * f(q1,...,qn) where f is not of the form ?i).</li>
     * </ul>
     *
     * @param symbol
     * @return
     */
    public abstract TreeAutomaton<String> binarize(String symbol);

    /**
     * Computes an automaton for all binarizations of a given term. This
     * automaton is obtained from the binarizations of the individual symbols by
     * tree automaton substitution. That is, the binarization of f(t1,...,tn)
     * contains all the rules from the binarizations of f and all t1,...,tn. The
     * states of these automata are renamed apart, and connected in the
     * following way. If the binarization automaton for f contains a rule q ->
     * ?i (i.e., the i-th variable), then all occurrences of q in the automaton
     * are replaced by the final state of the binarization automaton for ti.
     *
     * @param term
     * @return
     */
    public TreeAutomaton<String> binarize(Tree<String> term) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        int finalState = binarize(term, ret, "q");
        ret.addFinalState(finalState);
        return ret;
    }
    
    private TreeAutomaton binarizeCached(String label) {
        TreeAutomaton ret = binarizationCache.get(label);
        
        if( ret == null ) {
            ret = binarize(label);
            binarizationCache.put(label, ret);
        }
        
        return ret;
    }

    private int binarize(Tree<String> term, ConcreteTreeAutomaton<String> ret, String nodeName) {
        int arity = term.getChildren().size();
        int finalStateHere = -1;

        // compute automata for all subtrees and store their final states
        int[] finalStatesOfChildren = new int[arity];
        for (int i = 0; i < arity; i++) {
            finalStatesOfChildren[i] = binarize(term.getChildren().get(i), ret, nodeName + (i + 1));
        }

        // compute binarization automaton for the current node and compute map
        // that maps q to i iff q -> ?i is a rule
        TreeAutomaton<?> autoHere = binarizeCached(term.getLabel());

        Int2IntMap variableStates = findVariableStates(autoHere, arity);

        // copy rules of this automaton into ret, renaming states
        for (Rule rule : autoHere.getRuleSet()) {
            // skip rules of the form q -> ?i
            if (!variableStates.containsKey(rule.getParent())) {
                String newParent = nodeName + "_" + autoHere.getStateForId(rule.getParent()).toString();

                // each child state is either a renamed copy of the original child state,
                // or the final state of the i-th child automaton (if original child state was a q with q -> ?i)
                String[] newChildren = new String[rule.getArity()];
                for (int i = 0; i < rule.getArity(); i++) {
                    if (variableStates.containsKey(rule.getChildren()[i])) {
                        newChildren[i] = ret.getStateForId(finalStatesOfChildren[variableStates.get(rule.getChildren()[i])]);
                    } else {
                        newChildren[i] = nodeName + "_" + autoHere.getStateForId(rule.getChildren()[i]).toString();
                    }
                }

                // create new rule
                String label = autoHere.getSignature().resolveSymbolId(rule.getLabel());
                Rule newRule = ret.createRule(newParent, label, newChildren, rule.getWeight());
                ret.addRule(newRule);

                // if original parent was final state, add new parent as final state to ret
                if (autoHere.getFinalStates().contains(rule.getParent())) {
                    int newParentId = ret.getIdForState(newParent);
                    assert finalStateHere == -1 || finalStateHere == newParentId; // ensure at most one final state
                    finalStateHere = newParentId;
                }
            }
        }

        assert finalStateHere > -1; // ensure at least one final state
        return finalStateHere;
    }

    private Int2IntMap findVariableStates(TreeAutomaton automaton, int arity) {
        Int2IntMap ret = new Int2IntOpenHashMap();
        int[] emptyChildren = new int[0];
        Signature sig = automaton.getSignature();

        for (int i = 1; i <= arity; i++) {
            String varsym = "?" + i;
            int varid = sig.getIdForSymbol(varsym);
            Set<Rule> rules = automaton.getRulesBottomUp(varid, emptyChildren);
            assert rules.size() == 1 : "found " + rules.size() + " rules for " + varsym; // by assumption, see above

            ret.put(rules.iterator().next().getParent(), i - 1);
        }

        return ret;
    }

    /**
     * Returns an iterator over all subclasses of RegularSeed.
     *
     * @return
     */
    public static Iterator<Class> getAllRegularSeedClasses() {
        ServiceLoader<RegularSeed> rsLoader = ServiceLoader.load(RegularSeed.class);
        return Iterators.transform(rsLoader.iterator(), new Function<RegularSeed, Class>() {
            public Class apply(RegularSeed f) {
                return f.getClass();
            }
        });
    }
}
 