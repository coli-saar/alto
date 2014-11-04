/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
/**
 *
 * @author jonas
 */
public class DynamicMergePartnerFinder extends MergePartnerFinder {

    private final boolean isFinal;
    private final DynamicMergePartnerFinder[] children;
    private final IntList finalSet;//list is fine, since every subgraph gets sorted in at most once.
    private final int sourceNr;
    private final int sourcesRemaining;
    private final int ALL;//give this name so its unlikely this is actually a name of a source
    private final int BOT;
    private final SGraphBRDecompositionAutomaton auto;

    public DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomaton auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        if (nrSources == 0){
            isFinal = true;
            sourceNr = -1;
            children = null;
            finalSet = new IntArrayList();
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
            /*for (int i = 0; i<nrNodes; i++)
            {
                children[i] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);//TODO: throw error if vName == ALL or vName == BOT
            }
            children[BOT] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);
            children[ALL] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);*/
        }
    }

    @Override
    public void insert(int rep)
    {
        if (isFinal)
        {
            //if (finalSet.contains(rep))
            //    System.out.println(rep + " already here!");
            finalSet.add(rep);
        }
        else
        {
            int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
            if (vNr != -1){
                 insertInto(vNr, rep);
                 insertInto(ALL, rep);
            } else{
                 insertInto(BOT, rep);
                 insertInto(ALL, rep);
            }
        }
    }
    
    private void insertInto (int index, int rep)
    {
        if (children[index] != null){
             children[index].insert(rep);
         }
         else{
             children[index] = new DynamicMergePartnerFinder(sourceNr + 1, sourcesRemaining -1, children.length-2, auto);
             children[index].insert(rep);
         }
    }

    @Override
    public IntList getAllMergePartners(int rep)
    {
        if (isFinal)
            return finalSet;
        else
        {
            int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
            if (vNr != -1)
            {
                 IntList ret = new IntArrayList();//list is fine, since the two lists we get bottom up are disjoint anyway.
                 if (!(children[vNr] == null))
                     ret.addAll(children[vNr].getAllMergePartners(rep));
                 if (!(children[BOT] == null))
                     ret.addAll(children[BOT].getAllMergePartners(rep));
                 /*if (children[BOT] != null && children[vNr]!= null)
                 {
                     IntSet set2 = children[BOT].getAllMergePartners(rep);
                     for (int i : children[vNr].getAllMergePartners(rep)){
                        if (set2.contains(i)){
                            System.out.println("overlap!");
                        }
                     }
                 }*/
                 return ret;
            }
            else
            {
                if ((children[ALL] == null))
                    return new IntArrayList();
                else
                 return children[ALL].getAllMergePartners(rep);
            }
        }
    }

}
