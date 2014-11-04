/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
/**
 *
 * @author jonas
 */
public class EdgeMergePartnerFinderBot extends MergePartnerFinder {

    private final boolean isFinal;
    private final EdgeMergePartnerFinderBot[] children;
    private final IntSet finalSet;//list is fine, since every subgraph gets sorted in at most once.
    public final SGraphBRDecompositionAutomaton auto;
    private final int edgeSource;
    private final int edgeTarget;
    private final int nrNodes;
    private final int HASEDGE = 0;
    private final int NOTHASEDGE = 1;

    public EdgeMergePartnerFinderBot(int edgeSource, int edgeTarget, int nrNodes, SGraphBRDecompositionAutomaton auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
            this.nrNodes = nrNodes;
        if (edgeSource > nrNodes-1){
            isFinal = true;
            children = null;
            finalSet = new IntOpenHashSet();
            this.edgeSource = -1;
            this.edgeTarget = -1;
        }
        else{
            isFinal = false;
            finalSet = null;
            children = new EdgeMergePartnerFinderBot[2];//0: the ones that have edge, 1: do not have edge
            this.edgeSource = edgeSource;
            this.edgeTarget = edgeTarget;
        }
    }

    @Override
    public void insert(int rep)
    {
        if (isFinal)
        {
            finalSet.add(rep);
        }
        else
        {
            BoundaryRepresentation br = auto.getStateForId(rep);//maybe also give this as argument?
            boolean hasEdge = br.getInBoundaryEdges().contains(new IntBasedEdge(edgeSource, edgeTarget));
            
            if (hasEdge){
                if (children[HASEDGE] != null){
                    children[HASEDGE].insert(rep);
                }
                else{
                    int newTarget;
                    int newSource;

                    if (edgeTarget < nrNodes -1){
                        newSource = edgeSource;
                        newTarget = edgeTarget +1;
                    } else {//then edgeSource must be smaller than nrNodes, since not final.
                        newSource = edgeSource+1;
                        newTarget = 0;
                    }

                    children[HASEDGE] = new EdgeMergePartnerFinderBot(newSource, newTarget, nrNodes, auto);
                    children[HASEDGE].insert(rep);
                }
            }
            else{
                if (children[NOTHASEDGE] != null){
                    children[NOTHASEDGE].insert(rep);
                }
                else{
                    int newTarget;
                    int newSource;

                    if (edgeTarget < nrNodes -1){
                        newSource = edgeSource;
                        newTarget = edgeTarget +1;
                    } else {//then edgeSource must be smaller than nrNodes -1, since not final.
                        newSource = edgeSource+1;
                        newTarget = 0;
                    }

                    children[NOTHASEDGE] = new EdgeMergePartnerFinderBot(newSource, newTarget, nrNodes, auto);
                    children[NOTHASEDGE].insert(rep);
                }
            }
        }
    }

    @Override
    public IntSet getAllMergePartners(int rep)
    {
        if (isFinal)
            return finalSet;
        else{
            BoundaryRepresentation br = auto.getStateForId(rep);//maybe also give this as argument?
            boolean hasEdge = br.getInBoundaryEdges().contains(new IntBasedEdge(edgeSource, edgeTarget));
            
            if (hasEdge){
                if ((children[NOTHASEDGE] == null))
                    return new IntOpenHashSet();
                else
                    return children[NOTHASEDGE].getAllMergePartners(rep);
            }
            else{
                IntSet ret = new IntOpenHashSet();//list is fine, since the two lists we get bottom up are disjoint anyway.
                if (!(children[NOTHASEDGE] == null))
                    ret.addAll(children[NOTHASEDGE].getAllMergePartners(rep));
                if (!(children[HASEDGE] == null))
                    ret.addAll(children[HASEDGE].getAllMergePartners(rep));
                return ret;
            }
        }
    }

}

