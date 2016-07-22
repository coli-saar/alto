/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import com.google.common.base.Function;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.rule_finding.sampling.rule_weighting.AutomatonWeighted;
import de.up.ling.tree.Tree;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class ProposalTest {
    /**
     * 
     */
    private Proposal prop;
    /**
     *
     */
    private AutomatonWeighted auw;

    /**
     *
     */
    private TreeAutomaton tau;
    
    @Before
    public void setUp() {
        prop = new Proposal(48487458345845894L);
        
        StringAlgebra sal = new StringAlgebra();
        tau = sal.decompose(sal.parseString("a a b b a a"));

        tau.normalizeRuleWeights();
        AdaGrad ada = new AdaGrad();

        auw = new AutomatonWeighted(tau, 2, 4.0, ada);
    }

    /**
     * Test of getRawTreeSample method, of class Proposal.
     */
    @Test
    public void testGetRawTreeSample() {
        TreeSample<Integer> ts = this.prop.getRawTreeSample(auw, 10);
        
        Function<Integer,String> funct = (Integer i) -> tau.getSignature().resolveSymbolId(i);
        Tree<String> t = ts.getSample(7).map(funct);
        assertEquals(t.toString().trim(),"*(a,*(a,*(b,*(*(b,a),a))))");
    }

    /**
     * Test of getRuleTreeSample method, of class Proposal.
     */
    @Test
    public void testGetRuleTreeSample() {
        TreeSample<Rule> ts = this.prop.getTreeSample(auw, 10);
        
        assertEquals(-Math.log(5*4*3*2),ts.getLogPropWeight(7),0.000000000001);
        Function<Rule,String> funct = (Rule r) -> tau.getSignature().resolveSymbolId(r.getLabel());
        Tree<String> t = ts.getSample(7).map(funct);
        assertEquals(t.toString().trim(),"*(a,*(a,*(b,*(*(b,a),a))))");
    }

    /**
     * Test of getStringTreeSample method, of class Proposal.
     */
    @Test
    public void testGetStringTreeSample() {
        TreeSample<String> ts = this.prop.getStringTreeSample(auw, 10);
        
        assertEquals(ts.getSample(7).toString().trim(),"*(a,*(a,*(b,*(*(b,a),a))))");
    }
}
