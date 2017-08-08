/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.IntConsumer;

/**
 * The set of in-boundary edges of a {@code BoundaryRepresentation}.
 * Uses {@code byte} instead of {@code int} to save memory.
 * @author groschwitz
 */
class ShortBasedEdgeSet implements IdBasedEdgeSet {

    //static int idCounter = 0;
    //private final String ID;
    private final ShortArraySet edges;

    /**
     * Creates a new empty edge set.
     */
    ShortBasedEdgeSet() {
        edges = new ShortArraySet();
        //ID = "ID"+String.valueOf(idCounter);//for debugging
        //idCounter++;
        //System.err.println(ID + " created from scratch");
    }

    private ShortBasedEdgeSet(ShortBasedEdgeSet input) {
        edges = input.edges.clone();
        //edges = cloneLongSet(input.edges);
        //ID = "ID"+String.valueOf(idCounter);
        //idCounter++;
        //System.err.println(ID + " created by cloning from " + input.ID);
    }


    @Override
    public void add(int edge) {
        edges.add((short) edge);
        //System.err.println(ID + " added " + String.valueOf(edge));
    }
    @Override
    public boolean contains(int edge) {
        return edges.contains((short) edge);
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
    public IntList smartForgetIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo) {
        if (reference instanceof ShortBasedEdgeSet) {

            ShortIterator li = ((ShortBasedEdgeSet)reference).edges.iterator();
            IntList res = new IntArrayList();
            //System.err.println("Next test: "+reference.ID);
            //System.err.println(reference.edges.size());
            int i = 0;
            while (li.hasNext()) {
                //System.err.println(String.valueOf(i));
                i++;
                short e = li.nextShort();

                short otherNr = ((ShortBasedEdgeSet)reference).getOtherNode(e, (short) vNr, graphInfo);
                if (otherNr != -1) {
                    if (!br.isSource(otherNr) || otherNr == vNr) {
                        edges.remove(e);
                        res.add(e);
                    }
                }

            }
            return res;//returns the decrease in edgeID.
        } else {
            System.err.println("Conflicting types: was expecting ShortBasedEdgeSet!");
            return new IntArrayList();
        }
    }
    

    
    
    private short getOtherNode(short e, short v, GraphInfo graphInfo) {
        short source = (short) graphInfo.getEdgeSource(e);
        short target = (short) graphInfo.getEdgeTarget(e);
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
    
    @Override
    public int size() {
        return edges.size();
    }
    
    @Override
    public int getFirst() {
        ShortIterator it = edges.iterator();
        if (it.hasNext()) {
            return it.nextShort();
        } else {
            return -1;
        }
    }
    
    
    /*private static LongOpenHashSet cloneLongSet(LongOpenHashSet other){
     LongOpenHashSet ret = new LongOpenHashSet();
     ret.addAll(other);
     return ret;
     }*/
    
    @Override
    public void forEach(IntConsumer action) {
        ShortIterator it = edges.iterator();
        while(it.hasNext()){
            action.accept(it.nextShort());
        }
    }
    
}
