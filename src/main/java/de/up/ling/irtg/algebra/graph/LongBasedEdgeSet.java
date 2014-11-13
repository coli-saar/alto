/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 *
 * @author jonas
 */
public class LongBasedEdgeSet {

    /**
     *
     */
    private final LongOpenHashSet edges;

    public LongBasedEdgeSet() {
        edges = new LongOpenHashSet();
    }

    private LongBasedEdgeSet(LongBasedEdgeSet input) {
        this.edges = input.edges.clone();
    }

    public void add(int source, int target) {
        edges.add(NumbersCombine.combine(source, target));
    }
    
    public void add(long edge) {
        edges.add(edge);
    }

    public boolean contains(int source, int target) {
        return edges.contains(NumbersCombine.combine(source, target));
    }

    public boolean contains(long edge) {
        return edges.contains(edge);
    }

    public void remove(int source, int target) {
        edges.remove(NumbersCombine.combine(source, target));
    }

    public boolean disjunt(LongBasedEdgeSet other) {

        LongIterator it = edges.iterator();

        while (it.hasNext()) {
            if (other.edges.contains(it.nextLong())) {
                return false;
            }
        }

        return true;
    }

    public void addAll(LongBasedEdgeSet other) {
        edges.addAll(other.edges);
    }

    public void add(GraphEdge edge, SGraphBRDecompositionAutomaton auto) {
        add(getSource(edge, auto), getTarget(edge, auto));
    }

    public boolean contains(GraphEdge edge, SGraphBRDecompositionAutomaton auto) {
        return contains(getSource(edge, auto), getTarget(edge, auto));
    }

    public boolean containsAll(LongBasedEdgeSet other) {
        LongIterator li = other.edges.iterator();

        while (li.hasNext()) {
            long edge = li.nextLong();
            if (!edges.contains(edge)) {
                return false;
            }
        }
        return true;
    }

    private static int getSource(GraphEdge edge, SGraphBRDecompositionAutomaton auto) {
        return auto.getIntForNode(edge.getSource().getName());
    }

    private static int getTarget(GraphEdge edge, SGraphBRDecompositionAutomaton auto) {
        return auto.getIntForNode(edge.getTarget().getName());
    }

    @Override
    public LongBasedEdgeSet clone() {
        return new LongBasedEdgeSet(this);
    }

    
    
    public long smartForgetIncident(int source, LongBasedEdgeSet reference, BoundaryRepresentation br, SGraphBRDecompositionAutomaton auto) {
        LongIterator li = reference.edges.iterator();
        long res = 0;
        while (li.hasNext()) {
            long e = li.nextLong();
            int otherNr = reference.getOtherNode(e, source);
            if (otherNr != -1) {
                if (!br.isSource(otherNr) || otherNr == source) {
                    edges.remove(e);
                    res += br.getEdgeIDSummand(e, auto);
                }
            }
        }
        return res;//returns the decrease in edgeID.
    }

    private int getOtherNode(long e, int v) {
        int source = NumbersCombine.getFirst(e);
        int target = NumbersCombine.getSecond(e);
        if (source == v) {
            return target;
        } else if (target == v) {
            return source;
        } else {
            return -1;
        }
    }

    public void appendAll(StringBuilder result, SGraphBRDecompositionAutomaton auto) {
        for (long e : edges) {
            result.append("(").append(auto.getNodeForInt(NumbersCombine.getFirst(e))).append(",").append(auto.getNodeForInt(NumbersCombine.getSecond(e))).append(") ");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof LongBasedEdgeSet)) {
            return false;
        }
        LongBasedEdgeSet f = (LongBasedEdgeSet) other;
        return f.edges.equals(edges);
    }

    @Override
    public int hashCode() {
        return edges.hashCode();
    }
    //public boolean checkIterate(Fun)

    public List<BitSet> getCorrespondingBitSets(Long2ObjectMap<BitSet> map) {
        List<BitSet> res = new ArrayList<>();
        LongIterator it = edges.iterator();
        while (it.hasNext()) {
            BitSet current = map.get(it.nextLong());
            if (current != null)
                res.add(current);
            else
                return new ArrayList<>();
        }
        return res;
    }
    
    @Override
    public String toString(){
        StringBuilder res = new StringBuilder();
        res.append("[");
        LongIterator it = edges.iterator();
        while (it.hasNext()){
            long next = it.next();
            res.append("("+NumbersCombine.getFirst(next)+","+NumbersCombine.getSecond(next)+"), ");
        }
        res.append("]");
        return res.toString();
    }
    
    public boolean isEmpty(){
        return edges.isEmpty();
    }
    
    public static long getLongForEdge(GraphEdge e, SGraphBRDecompositionAutomaton auto){
        return NumbersCombine.combine(getSource(e, auto), getTarget(e, auto));
    }
}
