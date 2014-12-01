/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringJoiner;

/**
 *
 * @author jonas
 */
public class ShortBasedEdgeSet extends IdBasedEdgeSet {

    /**
     *
     */
    static int idCounter = 0;
    //private final String ID;
    private final ShortArraySet edges;

    public ShortBasedEdgeSet() {
        edges = new ShortArraySet();
        //ID = "ID"+String.valueOf(idCounter);//for debugging
        idCounter++;
        //System.err.println(ID + " created from scratch");
    }

    private ShortBasedEdgeSet(ShortBasedEdgeSet input) {
        edges = input.edges.clone();
        //edges = cloneLongSet(input.edges);
        //ID = "ID"+String.valueOf(idCounter);
        idCounter++;
        //System.err.println(ID + " created by cloning from " + input.ID);
    }

    @Override
    public void add(int source, int target, GraphInfo graphInfo) {
        edges.add((short) graphInfo.edgesBySourceAndTarget[source][target]);
        //System.err.println(ID + " added " + NumbersCombine.combine(source, target));
    }

    @Override
    public void add(int edge) {
        edges.add((short) edge);
        //System.err.println(ID + " added " + String.valueOf(edge));
    }

    @Override
    public boolean contains(int source, int target, GraphInfo graphInfo) {
        return edges.contains((short) graphInfo.edgesBySourceAndTarget[source][target]);
    }

    @Override
    public boolean contains(int edge) {
        return edges.contains((short) edge);
    }

    @Override
    public void remove(int source, int target, GraphInfo graphInfo) {
        edges.remove((short) graphInfo.edgesBySourceAndTarget[source][target]);
    }

    @Override
    public boolean disjunt(IdBasedEdgeSet other) {

        if (other instanceof ShortBasedEdgeSet) {

            ShortIterator it = edges.iterator();

            while (it.hasNext()) {
                if (((ShortBasedEdgeSet) other).edges.contains(it.nextShort())) {
                    return false;
                }
            }

            return true;

        } else {
            System.err.println("Conflicting Types: was expecting ShortBasedEdgeSet!");
            return false;
        }

    }

    @Override
    public void addAll(IdBasedEdgeSet other) {
        if (other instanceof ShortBasedEdgeSet) {
            edges.addAll(((ShortBasedEdgeSet) other).edges);
        } else {
            System.err.println("Conflicting Types: was expecting ShortBasedEdgeSet!");
        }
        //System.err.println(ID + " added " + other.edges);
    }

    @Override
    public void add(GraphEdge edge, GraphInfo graphInfo) {
        add(graphInfo.getEdgeId(edge));
    }

    @Override
    public boolean contains(GraphEdge edge, GraphInfo graphInfo) {
        return contains(graphInfo.getEdgeId(edge));
    }

    @Override
    public boolean containsAll(int[] other) {
        for (int i = 0; i < other.length; i++) {
            short edge = (short) other[i];
            if (!edges.contains(edge)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ShortBasedEdgeSet clone() {
        return new ShortBasedEdgeSet(this);
    }

    @Override
    public long smartForgetIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo) {
        if (reference instanceof ShortBasedEdgeSet) {

            ShortIterator li = ((ShortBasedEdgeSet)reference).edges.iterator();
            long res = 0;
            //System.err.println("Next test: "+reference.ID);
            //System.err.println(reference.edges.size());
            int i = 0;
            while (li.hasNext()) {
                //System.err.println(String.valueOf(i));
                i++;
                short e = li.nextShort();

                short otherNr = ((ShortBasedEdgeSet)reference).getOtherNode(e, (short) vNr, graphInfo);
                if (otherNr != -1) {
                    res += br.getEdgeIDSummand(e, vNr, source, graphInfo);
                    if (!br.isSource(otherNr) || otherNr == vNr) {
                        edges.remove(e);
                    }
                }

            }
            return res;//returns the decrease in edgeID.
        } else {
            System.err.println("Conflicting types: was expecting ShortBasedEdgeSet!");
            return 0;
        }
    }

    /*public long computeEdgeIdBonus(int source, BitSet isSourceNode, BoundaryRepresentation br, SGraphBRDecompositionAutomaton graphInfo){
     ShortIterator li = edges.iterator();
     long res = 0;
     while (li.hasNext()) {
     long edge = li.nextLong();
     if (isSourceNode.get(NumbersCombine.getFirst(edge)) && isSourceNode.get(NumbersCombine.getSecond(edge))){
     res += br.getEdgeIDSummand(edge, br.getSourceNode(source), source, graphInfo);
     }
     }
     return res;
     }*/
    
    @Override
    public long computeEdgeIdSummand(int vNr, int source, BoundaryRepresentation br, GraphInfo graphInfo){
        long res = 0;
        ShortIterator it = edges.iterator();
        while (it.hasNext()) {
           short edge = it.nextShort();
           if (graphInfo.isIncident(edge, vNr)){
              res += br.getEdgeIDSummand(edge, vNr, source, graphInfo);
           }
        }
        return res;
    }
    
    
    private short getOtherNode(short e, short v, GraphInfo graphInfo) {
        short source = (short) graphInfo.edgeSources[e];
        short target = (short) graphInfo.edgeTargets[e];
        if (source == v) {
            return target;
        } else if (target == v) {
            return source;
        } else {
            return -1;
        }
    }

    @Override
    public void appendAll(StringBuilder result, GraphInfo graphInfo) {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (long e : edges) {
            sj.add("(" + graphInfo.getNodeForInt(NumbersCombine.getFirst(e)) + "," + graphInfo.getNodeForInt(NumbersCombine.getSecond(e)) + ")");
        }
        result.append(sj);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof ShortBasedEdgeSet)) {
            return false;
        }
        ShortBasedEdgeSet f = (ShortBasedEdgeSet) other;
        return f.edges.equals(edges);
    }

    @Override
    public int hashCode() {
        return edges.hashCode();
    }
    //public boolean checkIterate(Fun)

    @Override
    public List<BitSet> getCorrespondingBitSets(Int2ObjectMap<BitSet> map) {
        List<BitSet> res = new ArrayList<>();
        ShortIterator it = edges.iterator();
        while (it.hasNext()) {
            BitSet current = map.get(it.nextShort());
            if (current != null) {
                res.add(current);
            } else {
                return new ArrayList<>();
            }
        }
        return res;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("[");
        ShortIterator it = edges.iterator();
        while (it.hasNext()) {
            short next = it.nextShort();
            res.append("(" + NumbersCombine.getFirst(next) + "," + NumbersCombine.getSecond(next) + "), ");
        }
        res.append("]");
        return res.toString();
    }

    @Override
    public boolean isEmpty() {
        return edges.isEmpty();
    }

    /*private static LongOpenHashSet cloneLongSet(LongOpenHashSet other){
     LongOpenHashSet ret = new LongOpenHashSet();
     ret.addAll(other);
     return ret;
     }*/
}
