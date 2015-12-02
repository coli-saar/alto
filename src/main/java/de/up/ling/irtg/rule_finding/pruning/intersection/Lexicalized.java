/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class Lexicalized extends ConcreteTreeAutomaton<Lexicalized.Lexicalization> {

    /**
     *
     * @param sig
     * @param allLabels
     */
    public Lexicalized(Signature sig, IntSet allLabels) {
        super(sig);

        int bc = this.addState(Lexicalization.BY_CHILD);
        int el = this.addState(Lexicalization.ELSEWHERE);

        this.addFinalState(bc);

        IntIterator iit = allLabels.iterator();

        while (iit.hasNext()) {
            int label = iit.nextInt();
            int arity = sig.getArity(label);

            if (Variables.IS_VARIABLE.test(sig.resolveSymbolId(label))) {
                int[] children = new int[arity];
                Arrays.fill(children, bc);

                Rule r = this.createRule(el, label, children, 1.0);
                this.addRule(r);

            } else {
                int[] children = new int[arity];
                Arrays.fill(children, el);

                Rule r = this.createRule(el, label, children, 1.0);
                this.addRule(r);

                if (arity == 0) {
                    int[] kids = Arrays.copyOf(children, arity);

                    r = this.createRule(bc, label, kids, 1.0);
                    this.addRule(r);
                } else {
                    for (int i = 0; i < arity; ++i) {
                        int[] kids = Arrays.copyOf(children, arity);
                        kids[i] = bc;

                        r = this.createRule(bc, label, kids, 1.0);
                        this.addRule(r);
                    }
                }
            }
        }
    }
    
    /**
     * 
     */
    public enum Lexicalization {
        BY_CHILD,
        ELSEWHERE;
    }
}
