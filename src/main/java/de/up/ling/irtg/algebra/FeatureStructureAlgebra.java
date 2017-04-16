/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ImmutableMap;
import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.FeatureStructure;
import de.saar.coli.featstruct.FsParsingException;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class FeatureStructureAlgebra extends Algebra<FeatureStructure> {
    public static final String UNIFY = "unify";
    public static final String PROJ = "proj_";
    public static final String EMBED = "emb_";

    public FeatureStructureAlgebra() {
        signature.addSymbol(UNIFY, 2);
    }

    @Override
    protected FeatureStructure evaluate(String label, List<FeatureStructure> childrenValues) {
        if (UNIFY.equals(label)) {
            assert childrenValues.size() == 2;
            return childrenValues.get(0).unify(childrenValues.get(1));
        } else if (label.startsWith(PROJ)) {
            assert childrenValues.size() == 1;
            return childrenValues.get(0).get(withoutPrefix(label, PROJ));
        } else if (label.startsWith(EMBED)) {
            assert childrenValues.size() == 1;
            AvmFeatureStructure ret = new AvmFeatureStructure();
            ret.put(withoutPrefix(label, EMBED), childrenValues.get(0));
            return ret;
        } else {
            assert childrenValues.isEmpty();
            
            try {
                return FeatureStructure.parse(label);
            } catch (FsParsingException ex) {                
                Logger.getLogger(FeatureStructureAlgebra.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    private static String withoutPrefix(String s, String prefix) {
        return s.substring(prefix.length());
    }

    @Override
    public FeatureStructure parseString(String representation) throws ParserException {
        try {
            return FeatureStructure.parse(representation);
        } catch (FsParsingException ex) {
            throw new ParserException(ex);
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException, ParserException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/fcfg.irtg"));
        TreeAutomaton<?> chart = irtg.parse(ImmutableMap.of("string", "john sleeps"));
        int count = 0;
        
        for( Tree<String> dt : chart.languageIterable() ) {
            Object fs = irtg.interpret(dt, "ft");
            Object t = irtg.interpret(dt, "tree");
            
            if( fs != null ) {
                System.err.printf("[%3d] %s\n      %s\n      %s\n", ++count, dt, t, fs);
            }
        }
    }
}
