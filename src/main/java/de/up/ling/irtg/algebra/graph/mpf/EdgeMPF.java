/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.mpf;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
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
    private final StorageMPF storeHere;
    private final int parentEdge;
    private IntSet vertices;
    
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
        parentEdge = -1;
        storeHere = new StorageMPF(auto);
        this.vertices = vertices;//DEBUGGING
    }
    
    private EdgeMPF(int[] local2Global, Int2IntMap global2Local, int currentIndex, SGraphBRDecompositionAutomatonBottomUp auto, int parentEdge) {
        local2GlobalEdgeIDs = local2Global;
        global2LocalEdgeIDs = global2Local;
        this.currentIndex = currentIndex;
        this.auto = auto;
        children = new MergePartnerFinder[local2GlobalEdgeIDs.length - currentIndex];
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
                    targetChild = new StorageMPF(auto);
                } else {
                    targetChild = new EdgeMPF(local2GlobalEdgeIDs, global2LocalEdgeIDs, localEdgeID, auto, nextEdge);
                }
                children[childIndex] = targetChild;
            }
            
            
            targetChild.insert(rep);
        }
    }

    @Override
    public IntList getAllMergePartners(int rep) {
        IntList ret = new IntArrayList();
        ret.addAll(storeHere.getAllMergePartners(rep));
        for (int i = 0; i < children.length; i++) {
            if (children[i] != null && !auto.getStateForId(rep).getInBoundaryEdges().contains(local2GlobalEdgeIDs[i+currentIndex+1])) {//this is not optimized yet! this does not give the theoretic result.
                ret.addAll(children[i].getAllMergePartners(rep));
            }
        }
        return ret;
    }

    @Override
    public void print(String prefix, int indent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
