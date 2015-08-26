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
import de.up.ling.irtg.codec.AlgebraStringRepresentationOutputCodec;
import de.up.ling.irtg.codec.OutputCodec;
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
    private final List<Pair<String,OutputCodec>> interpretationToCodec;
    private final Algebra algebraForDerivationTree;

    public InterpretationPrintingPolicy(List<Pair<String, OutputCodec>> interpretationToCodec, Algebra algebraForDerivationTree) {
        this.interpretationToCodec = interpretationToCodec;
        this.algebraForDerivationTree = algebraForDerivationTree;
    }

    public List<Pair<String, OutputCodec>> get() {
        return interpretationToCodec;
    }

    public Algebra getAlgebraForDerivationTree() {
        return algebraForDerivationTree;
    }
    
    public static InterpretationPrintingPolicy fromIrtg(InterpretedTreeAutomaton irtg) {
        List<Pair<String, OutputCodec>> interps = new ArrayList<>();
        Map<String,Interpretation> interpMap = irtg.getInterpretations();
        
        for( String key : interpMap.keySet() ) {
            Algebra alg = interpMap.get(key).getAlgebra();
            OutputCodec oc = new AlgebraStringRepresentationOutputCodec(alg);
            
            interps.add(new Pair(key, oc));
        }
        
        return new InterpretationPrintingPolicy(interps, new TreeAlgebra());
    }
    
    public static InterpretationPrintingPolicy create(Algebra algebraForDerivationTree, Object... args) {
        List<Pair<String,OutputCodec>> interpretationToAlgebra = new ArrayList<>();
        
        for( int i = 0; i < args.length; i += 2 ) {
            OutputCodec oc = new AlgebraStringRepresentationOutputCodec((Algebra) args[i+1]);
            interpretationToAlgebra.add(new Pair((String) args[i], oc));
        }
        
        return new InterpretationPrintingPolicy(interpretationToAlgebra, algebraForDerivationTree);
    }
    
    public static boolean isDeriv(Pair<String,Algebra> pair) {
        return DERIVATION_TREE_KEY.equals(pair.getLeft());
    }
}
