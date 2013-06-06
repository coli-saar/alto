/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Iterator;

/**
 *
 * @author koller
 */
public class Charts implements Iterable<TreeAutomaton> {

    public Iterator<TreeAutomaton> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static Charts computeCharts(Corpus corpus, InterpretedTreeAutomaton irtg, String filename) {
        return null;
    }
    
    public static Charts readCharts(String url) {
        return null;
    }
    
}
