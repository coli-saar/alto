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
public class OnTheFlyCharts implements Iterable<TreeAutomaton> {

    /**
     * The IRTG that will compute the necessary charts if we do not read them
     * from a file.
     */
    private final InterpretedTreeAutomaton irtg;
    
    /**
     * An iterable used to obtain inputs for which charts will be computed.
     */
    private final Iterable<Instance> instances;

    /**
     * A source of input streams from which the charts can be read.
     */
    private final FileInputStreamSupplier supp;
    
    /**
     * Creates an instance that will provide an iterator over charts by iterating
     * over the instances and computing charts as they are needed.
     * 
     * @param irtg
     * @param instances 
     */
    public OnTheFlyCharts(InterpretedTreeAutomaton irtg, Iterable<Instance> instances) {
        this.irtg = irtg;
        this.instances = instances;
        this.supp = null;
    }
    
    /**
     * Creates an instance that iterates over charts by reading them from the file
     * one by one.
     * 
     * @param chartsFile 
     */
    public OnTheFlyCharts(File chartsFile)
    {
        this.instances = null;
        this.irtg = null;
        this.supp = new FileInputStreamSupplier(chartsFile);
    }
    
    @Override
    public Iterator<TreeAutomaton> iterator() {
        // if there is no file from which we can read results, we compute them.
        if(this.supp == null){
            // for corpora we have to avoid an infinite loop.
            if(this.instances instanceof Corpus){
                return Iterators.transform(((Corpus) instances).blandIterator(),
                        inst -> irtg.parseInputObjects(inst.getInputObjects()));
            }
            else{
                return Iterators.transform(instances.iterator(), inst -> irtg.parseInputObjects(inst.getInputObjects()));
            }
        }
        else
        {
            try {
                //This is problematic, does the stream ever get closed?
                return new ZipEntryIterator<>(this.supp.get());
            } catch (IOException ex) {
                // If we have a file, then we do not have an IRTG so we can only return null.
                Logger.getLogger(OnTheFlyCharts.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }
}