/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules;

import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.math3.analysis.function.Signum;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SignificantVariablesTest {
    
    /**
     * 
     */
    private List<TreeAutomaton> corpus;   
    
    @Before
    public void setUp() {
        StringAlgebra sg = new StringAlgebra();
        corpus = new ArrayList<>();
        LeftRightXFromFinite lrx = new LeftRightXFromFinite();
        
        addEntry(sg, "Every Week Joan goes fishing for hering", lrx);
        addEntry(sg, "Every Week Mark goes fishing for salmon", lrx);
        addEntry(sg, "Every Week Mary goes hunting for deer", lrx);
        addEntry(sg, "Every Week Hank goes hunting for rabbits", lrx);
        
        
    }

    /**
     * 
     * @param sg
     * @param input
     * @param lrx
     * @param corpus 
     */
    private void addEntry(StringAlgebra sg, String input, LeftRightXFromFinite lrx) {
        List<String> words = sg.parseString(input);
        TreeAutomaton ta = sg.decompose(words);
        ta = (TreeAutomaton) lrx.apply(new Pair<>(ta,null)).getLeft();
        corpus.add(ta);
    }

    /**
     * Test of select method, of class SignificantVariables.
     */
    @Test
    public void testSelect() {
        Function<String,String> gl = (String in) ->{
            return in.split("_")[1];
        };
        Function<String,String> gr = (String in) ->{
            return in.split("_")[2];
        };
        Function<String,String> gp = (String in) ->{
            return in.split("_")[1]+"_"+in.split("_")[2];
        };
        Function<String,String> grev = (String in) ->{
            return in.split("_")[2]+"_"+in.split("_")[1];
        };
        SignificanceMeasure sm = (double leftCount, double rightCount, double pairCount, double reversePairCount, double allPairsCount) -> {
            return 1 + (pairCount / allPairsCount);
        };
        
        SignificantVariables sv = new SignificantVariables(gl, gr, grev, gp, sm);
        
        List<Tree<String>> l = sv.select(corpus, true);
        l.forEach((Tree<String> t) -> {
            System.out.println(t);
        });
        //TODO
    }
    
}
