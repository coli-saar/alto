/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import it.unimi.dsi.fastutil.ints.IntList;

/**
 *
 * @author jonas
 */
public abstract class MergePartnerFinder {

    /*public MergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomaton auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        if (nrSources == 0){
            isFinal = true;
            sourceNr = -1;
            children = null;
            finalSet = new IntOpenHashSet();
            ALL = -1;
            BOT = -1;
            sourcesRemaining = nrSources;
        }
        else{
            isFinal = false;
            finalSet = null;
            sourceNr = currentSource;
            children = new DynamicMergePartnerFinder[nrNodes + 2];
            ALL = nrNodes;
            BOT = nrNodes +1;
            sourcesRemaining = nrSources;
            
        }
    }*/

    public abstract void insert(int rep);
            
    public abstract IntList getAllMergePartners(int rep);

}
