/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author koller
 */
public interface ChartComputationListener {
    public void update(int index, Instance instance, TreeAutomaton chart);
}
