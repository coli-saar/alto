/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
    private final Long2ObjectMap<BitSet> notHasEdge;
    private final long[] relevantEdges;
    private final SGraphBRDecompositionAutomaton auto;

    public EdgeIntersectionMPF(boolean hasAll, IntSet vertices, SGraphBRDecompositionAutomaton auto) {
        this.auto = auto;
        localToGlobal = new IntArrayList();
        notHasEdge = new Long2ObjectOpenHashMap<>();

        if (hasAll) {
            relevantEdges = auto.getAllEdges();
        } else {
            relevantEdges = auto.getAllIncidentEdges(vertices);
        }
    }

    @Override
    public void insert(int rep) {
        int localIndex = localToGlobal.size();
        localToGlobal.add(rep);
        LongBasedEdgeSet inBEdges = auto.getStateForId(rep).getInBoundaryEdges();

        for (int i = 0; i < relevantEdges.length; i++) {
            long iLong = relevantEdges[i];
            if (!inBEdges.contains(iLong)) {
                if (notHasEdge.containsKey(iLong)) {
                    notHasEdge.get(iLong).set(localIndex);
                } else {
                    BitSet newBitSet = new BitSet();
                    newBitSet.set(localIndex);
                    notHasEdge.put(iLong, newBitSet);
                }
            }
        }
    }

    @Override
    public IntList getAllMergePartners(int rep) {
        LongBasedEdgeSet inBEdges = auto.getStateForId(rep).getInBoundaryEdges();
        LongBasedEdgeSet relevantInBdryEdges = new LongBasedEdgeSet();
        for (long d : relevantEdges){
            if (inBEdges.contains(d)){
                relevantInBdryEdges.add(d);
            }
        }
        if (relevantInBdryEdges.isEmpty()) {
            return getAll();//should never happen in this implementation though.
        } else {
            List<BitSet> relevantSets = relevantInBdryEdges.getCorrespondingBitSets(notHasEdge);

            if (relevantSets.isEmpty()) {
                return new IntArrayList();
            } else {
                BitSet intersection = relevantSets.get(0);
                for (int i = 1; i < relevantSets.size(); i++) {
                    intersection.and(relevantSets.get(i));
                }
                IntList res = new IntArrayList();
                for (int i = 0; i < intersection.length(); i++) {
                    if (intersection.get(i)) {
                        res.add(localToGlobal.get(i));
                    }
                }
                return res;
            }
        }
    }

    private IntList getAll() {
        ObjectCollection<BitSet> relevantSets = notHasEdge.values();
        if (relevantSets.isEmpty()) {
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
            LongBasedEdgeSet inBdryEdges = auto.getStateForId(i).getInBoundaryEdges();
            content.append(inBdryEdges.toString() + ", ");
        }
        content.append("|");
        for (long l : relevantEdges) {
            content.append("(" + String.valueOf(NumbersCombine.getFirst(l)) + ", " + String.valueOf(NumbersCombine.getSecond(l)) + "), ");
        }
        System.out.println(indenter.toString() + prefix + content);
    }

}
