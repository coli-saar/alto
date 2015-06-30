/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import java.util.Iterator;

/**
 * This attaches charts to the instances from an iterator on the fly.
 * 
 * Adds charts to instances by computing them, whenever an instance with a chart
 * is requested. Note that we copy the original instances and that we re-compute
 * the charts each time we apply attach() to an iterator.
 * 
 * @author koller
 * @author christoph teichmann
 */
public class OnTheFlyCharts implements ChartAttacher {

    /**
     * The IRTG that will compute the necessary charts.
     */
    private final InterpretedTreeAutomaton irtg;
    
    /**
     * Creates an instance that will provide an iterator over charts by iterating
     * over the instances and computing charts as they are needed.
     * 
     * @param irtg
     * @param instances 
     */
    public OnTheFlyCharts(InterpretedTreeAutomaton irtg) {
        this.irtg = irtg;
    }

    @Override
    public Iterator<Instance> attach(Iterator<Instance> source) {
        return new It(source);
    }
    
    /**
     * A simple wrapper, that creates a new instance with the chart attached.
     */
    private class It implements Iterator<Instance>
    {
        /**
         * 
         */
        private final Iterator<Instance> main;

        /**
         * 
         * @param main 
         */
        public It(Iterator<Instance> main) {
            this.main = main;
        }
        
        
        
        @Override
        public boolean hasNext() {
            return main.hasNext();
        }

        @Override
        public Instance next() {
            Instance inst = main.next();
            
            return inst.withChart(irtg.parseInputObjects(inst.getInputObjects()));
        }
        
    }
}