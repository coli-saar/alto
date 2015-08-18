/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.corpus;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Printing of derivation trees is controlled by key "derivation tree".
 * 
 * @author koller
 */
public class InterpretationPrintingPolicy {
    public static final String DERIVATION_TREE_KEY = "derivation tree";
    private List<Pair<String,Algebra>> interpretationToAlgebra;
    private Algebra algebraForDerivationTree;

    public InterpretationPrintingPolicy(List<Pair<String, Algebra>> interpretationToAlgebra, Algebra algebraForDerivationTree) {
        this.interpretationToAlgebra = interpretationToAlgebra;
        this.algebraForDerivationTree = algebraForDerivationTree;
    }

    public List<Pair<String, Algebra>> get() {
        return interpretationToAlgebra;
    }

    public Algebra getAlgebraForDerivationTree() {
        return algebraForDerivationTree;
    }
    
    public static InterpretationPrintingPolicy fromIrtg(InterpretedTreeAutomaton irtg) {
        List<Pair<String, Algebra>> interps = new ArrayList<>();
        Map<String,Interpretation> interpMap = irtg.getInterpretations();
        
        for( String key : interpMap.keySet() ) {
            interps.add(new Pair(key, interpMap.get(key).getAlgebra()));
        }
        
        return new InterpretationPrintingPolicy(interps, new TreeAlgebra());
    }
    
    public static boolean isDeriv(Pair<String,Algebra> pair) {
        return DERIVATION_TREE_KEY.equals(pair.getLeft());
    }
}
