/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import java.util.Collection;

/**
 *
 * @author koller
 */
public class IrtgTreeAutomatonAnnotator implements TreeAutomatonAnnotator {
    public InterpretedTreeAutomaton irtg;

    public IrtgTreeAutomatonAnnotator(InterpretedTreeAutomaton irtg) {
        this.irtg = irtg;
    }

    public Collection<String> getAnnotationIdentifiers() {
        return irtg.getInterpretations().keySet();
    }

    public String getAnnotation(Rule rule, String annotationIdentifier) {
        Homomorphism hom = irtg.getInterpretation(annotationIdentifier).getHomomorphism();
        return hom.rhsAsString(hom.get(rule.getLabel()));
    }
}
