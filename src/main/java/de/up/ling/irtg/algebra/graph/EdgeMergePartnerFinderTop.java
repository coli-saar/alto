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
public class EdgeMergePartnerFinderTop extends MergePartnerFinder {

    private final boolean isFinal;
    private final EdgeMergePartnerFinderTop[] children;
    private final EdgeMergePartnerFinderBot botMPF;
    private final int sourceNr;
    private final int sourcesRemaining;
    private final int ALL;//give this name so its unlikely this is actually a name of a source
    private final int BOT;
    private final SGraphBRDecompositionAutomaton auto;

    public EdgeMergePartnerFinderTop(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomaton auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        
        if (nrSources == 0){
            isFinal = true;
            sourceNr = -1;
            children = null;
            botMPF = new EdgeMergePartnerFinderBot(0,0,nrNodes, auto);
            ALL = -1;
            BOT = -1;
            sourcesRemaining = nrSources;
        }
        else{
            isFinal = false;
            botMPF = null;
            sourceNr = currentSource;
            children = new EdgeMergePartnerFinderTop[nrNodes + 2];
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
            botMPF.insert(rep);
        }
        else
        {
            int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
            if (vNr != -1)
            {
                 if (children[vNr] != null){
                     children[vNr].insert(rep);
                 }
                 else{
                     children[vNr] = new EdgeMergePartnerFinderTop(sourceNr + 1, sourcesRemaining -1, children.length-2, auto);
                     children[vNr].insert(rep);
                 }
                 if (children[ALL] != null){
                     children[ALL].insert(rep);
                 }
                 else{
                     children[ALL] = new EdgeMergePartnerFinderTop(sourceNr + 1, sourcesRemaining -1, children.length-2, auto);
                     children[ALL].insert(rep);
                 }
            }
            else
            {
                if (children[BOT] != null){
                     children[BOT].insert(rep);
                 }
                 else{
                     children[BOT] = new EdgeMergePartnerFinderTop(sourceNr + 1, sourcesRemaining -1, children.length-2, auto);
                     children[BOT].insert(rep);
                 }
                if (children[ALL] != null){
                     children[ALL].insert(rep);
                 }
                 else{
                     children[ALL] = new EdgeMergePartnerFinderTop(sourceNr + 1, sourcesRemaining -1, children.length-2, auto);
                     children[ALL].insert(rep);
                 }
            }
        }
    }

    @Override
    public IntList getAllMergePartners(int rep)
    {
        if (isFinal)
            return botMPF.getAllMergePartners(rep);
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

