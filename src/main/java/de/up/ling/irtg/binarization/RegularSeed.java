/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class RegularSeed {
    // returns a tree automaton for StringOrVar trees using variables x1,...,xk, where k = arity(symbol)
    // assumption: this automaton has exactly one final state
    // assumption: no state expands both into a variable symbol (?123) and into a terminal symbol (f or a)
    public abstract TreeAutomaton<String> binarize(String symbol);

    public TreeAutomaton<String> binarize(Tree<String> term) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        int finalState = binarize(term, ret, "q");
        ret.addFinalState(finalState);
        return ret;
    }

    private int binarize(Tree<String> term, ConcreteTreeAutomaton<String> ret, String nodeName) {
        int arity = term.getChildren().size();
        int finalStateHere = -1;

        int[] finalStatesOfChildren = new int[arity];
        for (int i = 0; i < arity; i++) {
            finalStatesOfChildren[i] = binarize(term.getChildren().get(i), ret, nodeName + (i + 1));
        }

        TreeAutomaton<?> autoHere = binarize(term.getLabel());

        System.err.println("bin auto for " + term.getLabel() + ":\n" + autoHere);
        System.err.println("sig: " + autoHere.getSignature());
        Int2IntMap variableStates = findVariableStates(autoHere, arity);

        for (int state : variableStates.keySet()) {
            System.err.println(autoHere.getStateForId(state) + " -> " + variableStates.get(state));
        }

        for (Rule rule : autoHere.getRuleSet()) {
            System.err.println("processing rule : " + rule.toString(autoHere));
            
            if (!variableStates.containsKey(rule.getParent())) {
                String newParent = nodeName + "_" + autoHere.getStateForId(rule.getParent()).toString();

                String[] newChildren = new String[rule.getArity()];
                for (int i = 0; i < rule.getArity(); i++) {
                    if (variableStates.containsKey(rule.getChildren()[i])) {
                        newChildren[i] = ret.getStateForId(finalStatesOfChildren[variableStates.get(rule.getChildren()[i])]);
                    } else {
                        newChildren[i] = nodeName + "_" + autoHere.getStateForId(rule.getChildren()[i]).toString();
                    }
                }

                String label = autoHere.getSignature().resolveSymbolId(rule.getLabel());
                Rule newRule = ret.createRule(newParent, label, newChildren, rule.getWeight());
                ret.addRule(newRule);
                System.err.println(" -> added: " + newRule.toString(ret));

                if (autoHere.getFinalStates().contains(rule.getParent())) {
                    finalStateHere = ret.getIdForState(newParent);
                }
            }
        }

        assert finalStateHere > -1;
        return finalStateHere;
    }

    private Int2IntMap findVariableStates(TreeAutomaton automaton, int arity) {
        Int2IntMap ret = new Int2IntOpenHashMap();
        int[] emptyChildren = new int[0];
        Signature sig = automaton.getSignature();

        for (int i = 1; i <= arity; i++) {
            Set<Rule> rules = automaton.getRulesBottomUp(sig.getIdForSymbol("?" + i), emptyChildren);
            assert rules.size() == 1; // by assumption, see above

            ret.put(rules.iterator().next().getParent(), i-1);
        }

        return ret;
    }
}
