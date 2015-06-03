/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.SGraphBRDecompositionAutomatonBottomUp;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;

/**
 *
 * @author jonas
 */
class EdgeMPF extends MergePartnerFinder{
    private final int[] local2GlobalEdgeIDs;
    private final Int2IntMap global2LocalEdgeIDs;//maybe better just use global ids and an int to object map for children?
    private final int currentIndex;
    private final SGraphBRDecompositionAutomatonBottomUp auto;
    private final MergePartnerFinder[] children;
    private final boolean[] childIsEdgeMPF;
    private final StorageMPF storeHere;
    private final int parentEdge;
    
    EdgeMPF(IntSet vertices, SGraphBRDecompositionAutomatonBottomUp auto) {
        currentIndex = -1;
        local2GlobalEdgeIDs = auto.completeGraphInfo.getAllIncidentEdges(vertices);
        Arrays.sort(local2GlobalEdgeIDs);
        global2LocalEdgeIDs = new Int2IntOpenHashMap();
        for (int i = 0; i<local2GlobalEdgeIDs.length; i++) {
            global2LocalEdgeIDs.put(local2GlobalEdgeIDs[i], i);
        }
        this.auto = auto;
        children = new MergePartnerFinder[local2GlobalEdgeIDs.length];
        childIsEdgeMPF = new boolean[local2GlobalEdgeIDs.length];
        parentEdge = -1;
        storeHere = new StorageMPF(auto);
    }
    
    private EdgeMPF(int[] local2Global, Int2IntMap global2Local, int currentIndex, SGraphBRDecompositionAutomatonBottomUp auto, int parentEdge) {
        local2GlobalEdgeIDs = local2Global;
        global2LocalEdgeIDs = global2Local;
        this.currentIndex = currentIndex;
        this.auto = auto;
        children = new MergePartnerFinder[local2GlobalEdgeIDs.length - currentIndex];
        childIsEdgeMPF = new boolean[local2GlobalEdgeIDs.length - currentIndex];
        this.parentEdge = parentEdge;
        storeHere = new StorageMPF(auto);
    }

    
    
    
    @Override
    public void insert(int rep) {
        BoundaryRepresentation bRep = auto.getStateForId(rep);
        
        int nextEdgeIndex;
        if (parentEdge == -1) {
            nextEdgeIndex = 0;
        } else {
            nextEdgeIndex = bRep.getSortedInBEdges().indexOf(parentEdge)+1;
        }
        
        
        if (nextEdgeIndex >= bRep.getSortedInBEdges().size()) {
            storeHere.insert(rep);
        } else {
            int nextEdge = bRep.getSortedInBEdges().get(nextEdgeIndex);
            int localEdgeID = global2LocalEdgeIDs.get(nextEdge);
            int childIndex = localEdgeID-currentIndex-1;
            
            MergePartnerFinder targetChild = children[childIndex];
            if (targetChild == null) {
                if (currentIndex == local2GlobalEdgeIDs.length - 1) {
                    childIsEdgeMPF[childIndex]=false;
                    targetChild = new StorageMPF(auto);
                } else {
                    targetChild = new EdgeMPF(local2GlobalEdgeIDs, global2LocalEdgeIDs, localEdgeID, auto, nextEdge);
                    childIsEdgeMPF[childIndex]=true;
                }
                children[childIndex] = targetChild;
            }
            
            
            targetChild.insert(rep);
        }
    }

    @Override
    public IntList getAllMergePartners(int rep) {
        IntList ret = new IntArrayList();
        IntList repNonEdgesLocal = new IntArrayList();
        BoundaryRepresentation bRep = auto.getStateForId(rep);
        for (int localID = 0; localID<local2GlobalEdgeIDs.length; localID++) {
            if (!bRep.getInBoundaryEdges().contains(local2GlobalEdgeIDs[localID])) {
                repNonEdgesLocal.add(localID);
            }
        }
        ret.addAll(storeHere.getAllMergePartners(rep));
        int listIndex = 0;
        for (int i : repNonEdgesLocal) {
            if (children[i] != null) {
                if (childIsEdgeMPF[i]) {
                    ret.addAll(((EdgeMPF)children[i]).getAllMergePartners(rep, repNonEdgesLocal.subList(listIndex+1, repNonEdgesLocal.size())));
                } else {
                    ret.addAll(children[i].getAllMergePartners(rep));
                }
            }
            listIndex++;
        }
        /*auto.getStateForId(rep).getInBoundaryEdges().forEach(edgeID -> {
            if (children[edgeID] != null) {
                ret.addAll(children[edgeID].getAllMergePartners(rep));
            }
        });*/
        return ret;
    }
    
    private IntList getAllMergePartners(int rep, IntList remainingRepNonEdgesLocal) {//does not quite work, since children are general MergePartnerFinder
        IntList ret = new IntArrayList();
        ret.addAll(storeHere.getAllMergePartners(rep));
        int listIndex = 0;
        for (int i : remainingRepNonEdgesLocal) {
            int childID = i-currentIndex-1;
            if (children[childID]!= null) {
                if (childIsEdgeMPF[childID]) {
                    ret.addAll(((EdgeMPF)children[childID]).getAllMergePartners(rep, remainingRepNonEdgesLocal.subList(listIndex+1, remainingRepNonEdgesLocal.size())));
                } else {
                    ret.addAll(children[childID].getAllMergePartners(rep));
                }
            }
            listIndex++;
        }
        return ret;
    }

    @Override
    public void print(String prefix, int indent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
