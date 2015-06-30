/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import java.util.Iterator;

/**
 * A drop in replacement for the class method capabilities of the Charts class.
 * 
 * Use an instance of this class to compute charts for an iterator, when they
 * are needed, instead of computing them all beforehand. Note that this class
 * implements no caching policy, so charts are re-computed every time they are
 * reached by an iterator. It is possible to either read the charts from a zip
 * file, or compute them using and IRTG and an Iterator over instances.
 * 
 * @author koller
 * @author christoph teichmann
 */
public class OnTheFlyCharts implements ChartAttacher {

    /**
     * The IRTG that will compute the necessary charts if we do not read them
     * from a file.
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