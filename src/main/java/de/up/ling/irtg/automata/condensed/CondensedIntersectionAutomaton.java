/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.signature.SignatureMapper;


/*
 TODO: 
 - collectStatePairs: remove CartesianIterator, use IntCartesianIterator to avoid boxing
 */
/**
 *
 * @author koller
 */
public class CondensedIntersectionAutomaton<LeftState, RightState> extends GenericCondensedIntersectionAutomaton<LeftState, RightState> {
    public CondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left, right, sigMapper);
    }

    @Override
    protected void collectOutputRule(Rule outputRule) {
        storeRule(outputRule);
    }

    @Override
    protected void addAllOutputRules() {
    }
    
    public static void main(String[] args) throws Exception {
        GenericCondensedIntersectionAutomaton.main(args, true, (left, right) -> left.intersectCondensed(right));
    }
}
