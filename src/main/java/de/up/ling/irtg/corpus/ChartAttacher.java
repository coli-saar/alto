/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 */
public interface ChartAttacher {

    /**
     * 
     * @param source
     * @return 
     */
    public Iterator<Instance> attach(Iterator<Instance> source);    
}
