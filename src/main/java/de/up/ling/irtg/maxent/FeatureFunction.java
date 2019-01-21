/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Map;

/**
 * A feature function for a log-linear model that assigns a value to
 * a chart rule. Technically, the value is assigned by executing
 * the {@link #evaluate(de.up.ling.irtg.automata.Rule, de.up.ling.irtg.automata.TreeAutomaton, de.up.ling.irtg.maxent.MaximumEntropyIrtg, java.util.Map) }
 * method on each rule of a {@link TreeAutomaton} with state set
 * State. The evaluate method can additionally see the {@link Rule}
 * itself, the {@link MaximumEntropyIrtg} from which the rule
 * originally came, and the inputs that are being parsed.<p>
 * 
 * For instance, in a log-linear model for a context-free grammar,
 * you will have a MaximumEntropyIrtg with a single interpretation
 * into the {@link StringAlgebra}. Therefore the states of the chart
 * will be pairs of String (= nonterminal from the grammar) and
 * {@link StringAlgebra.Span} (indicating a span in the input string).
 * A feature function will be passed the rule in the chart; the chart
 * itself; the IRTG grammar; and a map that maps the interpretation name
 * to the string that is currently being parsed. A feature that responds
 * to the first and last words in the span can look these up in the string.<p>
 * 
 * Concrete implementations of FeatureFunction must implement
 * the {@link #evaluate(de.up.ling.irtg.automata.Rule, de.up.ling.irtg.automata.TreeAutomaton, de.up.ling.irtg.maxent.MaximumEntropyIrtg, java.util.Map) }
 * method. Note that feature values can come from any class V,
 * but the standard methods for training and parsing in
 * {@link MaximumEntropyIrtg} only support V = {@link Double}.
 * 
 * @author koller
 */
public abstract class FeatureFunction<State, V> {
    /**
     * Computes the value of the feature function for a given rule.
     * Implement this method in a subclass to realize the actual
     * feature function.
     * 
     * @param rule a rule in "automaton"
     * @param automaton the tree automaton to which "rule" belongs, typically a parse chart
     * @param irtg the maxent IRTG which defined the grammar rule on which "rule" is based
     * @param inputs the inputs that are currently being parsed
     * @return the value of this feature function on those inputs
     */
    public abstract V evaluate(Rule rule, TreeAutomaton<State> automaton, MaximumEntropyIrtg irtg, Map<String,Object> inputs);

    @Override
    public String toString() {
        return this.getClass().getName();
    }

    protected State getLabelFor(Object state) {
        if (state instanceof Pair) {
            Pair s = (Pair) state;
            if (s.left instanceof Pair) {
                return this.getLabelFor(s.left);
            }
            return (State) s.left;
        }
        return (State) state;
    }

    protected String masking(String s) {
        if (s.contains("'")) {
            return "\"" + s + "\"";
        }
        return "'" + s + "'";
    }
}
