/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author koller
 */
public class IdentitySeed extends RegularSeed {
    private Algebra sourceAlgebra;
    
    /**
     * This is only for technical reasons; don't use this constructor!
     * Use {@link #IdentitySeed(de.up.ling.irtg.algebra.Algebra) } instead.
     */
    public IdentitySeed() {
        this(null, null);
    }

    public IdentitySeed(Algebra sourceAlgebra, Algebra targetAlgebra) {
        this.sourceAlgebra = sourceAlgebra;
    }
    
    @Override
    public TreeAutomaton<String> binarize(String symbol) {
        int arity = sourceAlgebra.getSignature().getArityForLabel(symbol);
        
        assert arity <= 2;
        
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        
        String[] varStates = new String[arity];
        for( int i = 0; i < arity; i++ ) {
            String varid = Integer.toString(i+1);
            Rule rule = ret.createRule("q" + varid, "?" + varid, new String[0]);
            ret.addRule(rule);
            varStates[i] = "q" + varid;
        }
        
        Rule rule = ret.createRule("q", symbol, varStates);
        ret.addRule(rule);
        
        ret.addFinalState(rule.getParent());
        
        return ret;
    }
    
    public static IdentitySeed fromInterp(InterpretedTreeAutomaton irtg, String interpretation) {
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();
        return new IdentitySeed(alg, alg);
    }
}
