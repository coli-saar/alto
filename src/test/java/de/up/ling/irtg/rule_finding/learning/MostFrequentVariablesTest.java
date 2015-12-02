/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.pruning.intersection.NoEmpty;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class MostFrequentVariablesTest {
    
    /**
     * 
     */
    private Iterable<TreeAutomaton> data;
    
    /**
     * 
     */
    private MostFrequentVariables mfv;
    
    @Before
    public void setUp() {
        StringAlgebra sta = new StringAlgebra();
        
        List<TreeAutomaton> list = new ArrayList<>();
        data = list;
        
        LeftRightXFromFinite<StringAlgebra.Span> variables = new LeftRightXFromFinite<>();
        
        TreeAutomaton ta = sta.decompose(sta.parseString("Hans geht heim"));
        AlignedTrees at = new AlignedTrees(ta, new SpecifiedAligner(ta));
        ta = variables.apply(at).getTrees();
        TreeAutomaton constrain = new NoEmpty(ta.getSignature(), ta.getAllLabels());
        ta = new IntersectionAutomaton(ta, constrain);
        list.add(ta);
        
        ta = sta.decompose(sta.parseString("Mary geht morgen heim"));
        at = new AlignedTrees(ta, new SpecifiedAligner(ta));
        ta = variables.apply(at).getTrees();
        constrain = new NoEmpty(ta.getSignature(), ta.getAllLabels());
        ta = new IntersectionAutomaton(ta, constrain);
        list.add(ta);
        
        ta = sta.decompose(sta.parseString("Der Jüngste geht heute heim"));
        at = new AlignedTrees(ta, new SpecifiedAligner(ta));
        ta = variables.apply(at).getTrees();
        constrain = new NoEmpty(ta.getSignature(), ta.getAllLabels());
        ta = new IntersectionAutomaton(ta, constrain);
        list.add(ta);
        
        ta = sta.decompose(sta.parseString("dann heute heim"));
        at = new AlignedTrees(ta, new SpecifiedAligner(ta));
        ta = variables.apply(at).getTrees();
        constrain = new NoEmpty(ta.getSignature(), ta.getAllLabels());
        ta = new IntersectionAutomaton(ta, constrain);
        list.add(ta);
        
        ta = sta.decompose(sta.parseString("Der Jüngste kommt"));
        at = new AlignedTrees(ta, new SpecifiedAligner(ta));
        ta = variables.apply(at).getTrees();
        constrain = new NoEmpty(ta.getSignature(), ta.getAllLabels());
        ta = new IntersectionAutomaton(ta, constrain);
        list.add(ta);
        
        mfv = new MostFrequentVariables();
    }

    /**
     * Test of getOptimalChoices method, of class MostFrequentVariables.
     */
    @Test
    public void testGetOptimalChoices() {
        Iterable<Tree<String>> it = mfv.getOptimalChoices(data);
        Object2DoubleMap<String> counts = mfv.countVariablesTopDown(data);
        Iterator<Tree<String>> iter = it.iterator();
        
        data.iterator().forEachRemaining((TreeAutomaton ta) ->{
            Tree<String> best = this.getBest(ta.languageIterable(), counts);
            
            assertEquals(best,iter.next());
        });
        
        assertFalse(iter.hasNext());
    }

    /**
     * Test of getBestAnalysis method, of class MostFrequentVariables.
     */
    @Test
    public void testGetBestAnalysis() {
        Object2DoubleMap<String> counts = mfv.countVariablesTopDown(data);
        
        data.iterator().forEachRemaining((TreeAutomaton ta) ->{
            Tree<String> t = mfv.getBestAnalysis(ta, counts);
            Tree<String> best = this.getBest(ta.languageIterable(), counts);
            
            assertEquals(best,t);
        });
    }

    /**
     * 
     * @param lang
     * @param counts
     * @return 
     */
    private Tree<String> getBest(Iterable<Tree<String>> lang, Object2DoubleMap<String> counts) {
        Tree<String> best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for(Tree<String> ts : lang){
            double sum = 0.0;
            
            for(Tree<String> node : ts.getAllNodes()){
                sum += counts.getDouble(node.getLabel());
            }
            
            if(sum > bestScore){
                best = ts;
                bestScore = sum;
            }
        }
        return best;
    }

    /**
     * Test of countVariablesTopDown method, of class MostFrequentVariables.
     */
    @Test
    public void testCountVariablesTopDown() {
        Object2DoubleMap<String> counts = mfv.countVariablesTopDown(data);
        
        assertEquals(counts.getDouble("Xgeht_heim"),3.0,0.000001);
        assertEquals(counts.getDouble("XMary_Mary"),1.0,0.000001);
        assertEquals(counts.getDouble("Xheim_Mary"),0.0,0.000001);
    }
    
}
