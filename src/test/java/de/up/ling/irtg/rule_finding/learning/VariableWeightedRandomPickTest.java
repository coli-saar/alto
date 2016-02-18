/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.pruning.intersection.NoEmpty;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class VariableWeightedRandomPickTest {
    
    /**
     * 
     */
    private Iterable<TreeAutomaton> data;
    
    /**
     * 
     */
    private VariableWeightedRandomPick vwr;
    
    
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
        
        
        vwr = new VariableWeightedRandomPick(1.5, 5, 10, 0.5);
    }

    /**
     * Test of getChoices method, of class VariableWeightedRandomPick.
     */
    @Test
    public void testGetChoices() {
        int count  = 0;
        for(Tree<String> t : vwr.getChoices(data)) {
            ++count;
        }
        
        assertEquals(count,50);
    }
}
