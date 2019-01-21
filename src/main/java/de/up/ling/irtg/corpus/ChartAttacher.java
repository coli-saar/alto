/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import java.util.Iterator;

/**
 * This interface provides an attach() method that takes an iterator over
 * instances and returns an iterator over copies of these instances that have
 * a chart attached to them.
 * 
 * @author christoph_teichmann
 */
public interface ChartAttacher {

    /**
     * This method must accept an instance iterator and return another instance
     * iterator which only returns instances that are copies of the original with
     * charts attached.
     * 
     * @param source
     * @return 
     */
    Iterator<Instance> attach(Iterator<Instance> source);
}
