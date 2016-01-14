/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.string;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedStructure;
import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedTree;
import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author christoph_teichmann
 */
public class LimitedTerminals implements AlignedStructure<StringAlgebra.Span> {

    @Override
    public Stream<AlignedTree> getAlignedTrees(int state1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<AlignedTree> getAlignedTrees(int state2, AlignedTree at) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StringAlgebra.Span getState(int state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IntStream getFinalStates() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IntCollection getAlignments(int state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }    
}
