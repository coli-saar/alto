/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jonas
 */
public class EdgeIntersectionMPF extends MergePartnerFinder {

    private final IntList localToGlobal;
    private final Int2ObjectMap<BitSet> notHasEdge;
    private final int[] relevantEdges;
    private final SGraphBRDecompositionAutomaton auto;
    //private final IntList allBdryReps;
    private final IntSet vertices;
    private final boolean hasAll;

    public EdgeIntersectionMPF(boolean hasAll, IntSet vertices, SGraphBRDecompositionAutomaton auto) {
        this.auto = auto;
        this.vertices = vertices;
        this.hasAll = hasAll;
        localToGlobal = new IntArrayList();
        notHasEdge = new Int2ObjectOpenHashMap<>();
        //allBdryReps = new IntArrayList();

        if (hasAll) {
            relevantEdges = auto.completeGraphInfo.getAllEdges();
        } else {
            relevantEdges = auto.completeGraphInfo.getAllIncidentEdges(vertices);
        }
    }

    @Override
    public void insert(int rep) {
        //allBdryReps.add(rep);
        int localIndex = localToGlobal.size();
        localToGlobal.add(rep);
        IdBasedEdgeSet inBEdges = auto.getStateForId(rep).getInBoundaryEdges();

        for (int i = 0; i < relevantEdges.length; i++) {
            int edgeId = relevantEdges[i];
            if (!inBEdges.contains(edgeId)) {
                if (notHasEdge.containsKey(edgeId)) {
                    notHasEdge.get(edgeId).set(localIndex);
                } else {
                    BitSet newBitSet = new BitSet();
                    newBitSet.set(localIndex);
                    notHasEdge.put(edgeId, newBitSet);
                    if (notHasEdge.get(edgeId).isEmpty()){
                        System.out.println("Error!");
                    }
                }
            }
        }
        //debugCheckBitSets();
        
    }
  

    @Override
    public IntList getAllMergePartners(int rep) {
        IdBasedEdgeSet inBEdges = auto.getStateForId(rep).getInBoundaryEdges();
        IdBasedEdgeSet relevantInBdryEdges;
        if (auto.completeGraphInfo.useBytes) {
            relevantInBdryEdges = new ByteBasedEdgeSet();
        } else {
            relevantInBdryEdges = new ShortBasedEdgeSet();
        }
        
        
        for (int d : relevantEdges){
            if (inBEdges.contains(d)){
                relevantInBdryEdges.add(d);
            }
        }
        
        if (relevantInBdryEdges.isEmpty()) {
            return getAll();//should never happen in this implementation though.
        } else {
            List<BitSet> relevantSets = relevantInBdryEdges.getCorrespondingBitSets(notHasEdge);

            if (relevantSets.isEmpty()) {
                IntList res = new IntArrayList();
                //debugCheckBitSets();
                //debugFindMissingBRs(rep, res);
                return res;
            } else {
                BitSet intersection = (BitSet)relevantSets.get(0).clone();
                for (int i = 1; i < relevantSets.size(); i++) {
                    intersection.and(relevantSets.get(i));
                }
                IntList res = new IntArrayList();
                for (int i = 0; i < intersection.length(); i++) {
                    if (intersection.get(i)) {
                        res.add(localToGlobal.get(i));
                    }
                }
                //debugCheckBitSets();
                //debugFindMissingBRs(rep, res);
                return res;
            }
        }
    }
    
    /*private void debugFindMissingBRs(int rep, IntList found){
        BoundaryRepresentation bdryRep = auto.getStateForId(rep);
        for (int i : allBdryReps){
            BoundaryRepresentation iBdryRep = auto.getStateForId(i);
            if (!found.contains(i)&&bdryRep.edgesDisjoint(iBdryRep)){
                System.out.println("diff found: " + iBdryRep.toString());
            }
        }
    }*/
    
      
    private void debugCheckBitSets(){
        for (BitSet bitset : notHasEdge.values()){
            if (bitset.isEmpty()){
                System.out.println("Error!");
            }
        }
    }

    private IntList getAll() {
        ObjectCollection<BitSet> relevantSets = notHasEdge.values();
        if (relevantSets.isEmpty()) {
        //debugCheckBitSets();
            return new IntArrayList();
        } else {
            Iterator<BitSet> it = relevantSets.iterator();
            BitSet intersection = it.next();
            while (it.hasNext()) {
                intersection.or(it.next());
            }
            IntList res = new IntArrayList();
            for (int i = 0; i < intersection.length(); i++) {
                if (intersection.get(i)) {
                    res.add(localToGlobal.get(i));
                }
            }
        //debugCheckBitSets();
            return res;
        }
    }

    @Override
    public void print(String prefix, int indent) {
        int indentSpaces = 5;
        StringBuilder indenter = new StringBuilder();
        for (int i = 0; i < indent * indentSpaces; i++) {
            indenter.append(" ");
        }
        StringBuilder content = new StringBuilder();
        for (int i : getAll()) {
            //content.append(String.valueOf(i)+",");
            IdBasedEdgeSet inBdryEdges = auto.getStateForId(i).getInBoundaryEdges();
            content.append(inBdryEdges.toString() + ", ");
        }
        content.append("|");
        for (long l : relevantEdges) {
            content.append("(" + String.valueOf(NumbersCombine.getFirst(l)) + ", " + String.valueOf(NumbersCombine.getSecond(l)) + "), ");
        }
        System.out.println(indenter.toString() + prefix + content);
        //debugCheckBitSets();
    }

}
