/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.automata.pruning.PruningPolicy;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.signature.SignatureMapper;
import java.util.HashSet;
import java.util.Set;


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
    
    public CondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper, PruningPolicy pp) {
        super(left, right, sigMapper, pp);
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
    
    /**
     * This is an old helper function to get debug info when parsing.
     * This function is in this code for legacy reasons
     * (such that older AltoLab tasks can still run).
     * @return 
     */
    @Deprecated
    @OperationAnnotation(code ="countRhsStates")
    public int getNumberOfSeenRhsStates() {
        Set<RightState> seenStates = new HashSet<>();
        for (Pair<LeftState, RightState> pair : stateInterner.getKnownObjects()) {
            seenStates.add(pair.right);
        }
        return seenStates.size();
    }
    
    public static void main(String[] args) throws Exception {
        GenericCondensedIntersectionAutomaton.main(args, true, (left, right) -> left.intersectCondensed(right));
    }
}
