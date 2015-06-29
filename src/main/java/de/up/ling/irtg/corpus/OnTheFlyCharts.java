/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.collect.Iterators;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.zip.ZipEntryIterator;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class OnTheFlyCharts implements Iterable<TreeAutomaton> {

    /**
     * 
     */
    private final InterpretedTreeAutomaton irtg;
    
    /**
     * 
     */
    private final Iterable<Instance> instances;

    /**
     * 
     */
    private final FileInputStreamSupplier supp;
    
    /**
     * 
     * @param irtg
     * @param instances 
     */
    public OnTheFlyCharts(InterpretedTreeAutomaton irtg, Iterable<Instance> instances) {
        this.irtg = irtg;
        this.instances = instances;
        this.supp = null;
    }
    
    
    public OnTheFlyCharts(File chartsFile)
    {
        this.instances = null;
        this.irtg = null;
        this.supp = new FileInputStreamSupplier(chartsFile);
    }
    
    @Override
    public Iterator<TreeAutomaton> iterator() {
        if(this.supp == null){
            return Iterators.transform(instances.iterator(), inst -> irtg.parseInputObjects(inst.getInputObjects()));
        }
        else
        {
            try {
                return new ZipEntryIterator<>(this.supp.get());
            } catch (IOException ex) {
                Logger.getLogger(OnTheFlyCharts.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }   
}