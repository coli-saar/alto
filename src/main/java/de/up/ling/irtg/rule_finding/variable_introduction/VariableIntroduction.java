/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.variable_introduction;

import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 * @param <Type1>
 * @param <Type2>
 */
public interface VariableIntroduction<Type1,Type2> extends Function<AlignedTrees<Type1>,AlignedTrees<Type2>> {

    @Override
    public AlignedTrees<Type2> apply(AlignedTrees<Type1> t);   
}
