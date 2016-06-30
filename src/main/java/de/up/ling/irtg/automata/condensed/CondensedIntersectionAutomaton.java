/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.signature.SignatureMapper;


/*
 TODO: 
 - collectStatePairs: remove CartesianIterator, use IntCartesianIterator to avoid boxing
 */
/**
 * Intersecting two automatons using a CKY-algorithm.
 * See GenericCondensedIntersectionAutomaton for further details about this class.
 * @author koller
 * @param <LeftState>
 * @param <RightState>
 */
public class CondensedIntersectionAutomaton<LeftState, RightState> extends GenericCondensedIntersectionAutomaton<LeftState, RightState> {
    
    /**
     * Assumes the signatures are identical.
     * @param left
     * @param right 
     */
    @OperationAnnotation(code = "condensedIntersection")
    public CondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right) {
        this(left, right, left.getSignature().getIdentityMapper());
    }
    
    public CondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left, right, sigMapper);
    }
    
    public CondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper, boolean debug) {
        super(left, right, sigMapper);
        this.DEBUG = debug;
    }

    @Override
    protected void collectOutputRule(Rule outputRule) {
        storeRuleBoth(outputRule);
    }

    @Override
    protected void addAllOutputRules() {
    }
    
    public static void main(String[] args) throws Exception {
        GenericCondensedIntersectionAutomaton.main(args, true, (left, right) -> left.intersectCondensed(right));
    }
}
