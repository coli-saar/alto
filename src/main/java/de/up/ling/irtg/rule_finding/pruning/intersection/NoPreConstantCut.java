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
 * @author christoph
 */
public class NoPreConstantCut extends ConcreteTreeAutomaton<Boolean> {
    /**
     * 
     * @param sig
     * @param allLabels 
     */
    public NoPreConstantCut(Signature sig, IntSet allLabels) {
        int justCut = this.addState(true);
        int cutDistant = this.addState(false);

        this.addFinalState(cutDistant);

        IntIterator iit = allLabels.iterator();
        while (iit.hasNext()) {
            int label = iit.nextInt();
            if (Variables.isVariable(sig.resolveSymbolId(label))) {
                int[] children = new int[sig.getArity(label)];
                Arrays.fill(children, justCut);

                Rule r = this.createRule(justCut, label, children, 1.0);
                this.addRule(r);

                r = this.createRule(cutDistant, label, children, 1.0);
                this.addRule(r);
            } else {
                int[] children = new int[sig.getArity(label)];

                Arrays.fill(children, cutDistant);
                Rule r = this.createRule(cutDistant, label, children, 1.0);
                this.addRule(r);

                if (sig.getArity(label) > 0) {
                    r = this.createRule(justCut, label, Arrays.copyOf(children, children.length), 1.0);
                    this.addRule(r);
                }
            }
        }
    }
}
