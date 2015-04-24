/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.bytes.ByteIterator;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.IntConsumer;

/**
 *
 * @author jonas
 */
public class ByteBasedEdgeSet extends IdBasedEdgeSet {

    /**
     *
     */
    private static int idCounter = 0;
    //private final String ID;
    private final ByteArraySet edges;
    

    public ByteBasedEdgeSet() {
        edges = new ByteArraySet();
        //ID = "ID"+String.valueOf(idCounter);//for debugging
        idCounter++;
        //System.err.println(ID + " created from scratch");
    }

    private ByteBasedEdgeSet(ByteBasedEdgeSet input) {
        edges = input.edges.clone();
        //edges = cloneLongSet(input.edges);
        //ID = "ID"+String.valueOf(idCounter);
        idCounter++;
        //System.err.println(ID + " created by cloning from " + input.ID);
    }

    @Override
    public void add(int source, int target, GraphInfo graphInfo) {
        edges.add((byte) graphInfo.getEdge(source,target));
        //System.err.println(ID + " added " + NumbersCombine.combine(source, target));
    }

    @Override
    public void add(int edge) {
        edges.add((byte) edge);
        //System.err.println(ID + " added " + String.valueOf(edge));
    }

    @Override
    public boolean contains(int source, int target, GraphInfo graphInfo) {
        return edges.contains((byte) graphInfo.getEdge(source,target));
    }

    @Override
    public boolean contains(int edge) {
        return edges.contains((byte) edge);
    }

    @Override
    public void remove(int source, int target, GraphInfo graphInfo) {
        edges.remove((byte) graphInfo.getEdge(source,target));
    }

    @Override
    public boolean disjunt(IdBasedEdgeSet other) {

        if (other instanceof ByteBasedEdgeSet) {

            ByteIterator it = edges.iterator();

            while (it.hasNext()) {
                if (((ByteBasedEdgeSet) other).edges.contains(it.nextByte())) {
                    return false;
                }
            }

            return true;

        } else {
            System.err.println("Conflicting Types: was expecting ByteBasedEdgeSet!");
            return false;
        }

    }

    @Override
    public void addAll(IdBasedEdgeSet other) {
        if (other instanceof ByteBasedEdgeSet) {
            edges.addAll(((ByteBasedEdgeSet) other).edges);
        } else {
            System.err.println("Conflicting Types: was expecting ByteBasedEdgeSet!");
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
            byte edge = (byte) other[i];
            if (!edges.contains(edge)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ByteBasedEdgeSet clone() {
        return new ByteBasedEdgeSet(this);
    }

    @Override
    public long smartForgetIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo) {
        if (reference instanceof ByteBasedEdgeSet) {
            ByteIterator li = ((ByteBasedEdgeSet) reference).edges.iterator();
            long res = 0;
        //System.err.println("Next test: "+reference.ID);
            //System.err.println(reference.edges.size());
            //int i = 0;
            while (li.hasNext()) {
                //System.err.println(String.valueOf(i));
                //i++;
                byte e = li.nextByte();

                byte otherNr = ((ByteBasedEdgeSet) reference).getOtherNode(e, (byte) vNr, graphInfo);
                if (otherNr != -1) {
                    res += BoundaryRepresentation.getEdgeIDSummand(e, vNr, source, graphInfo);
                    if (!br.isSource(otherNr) || otherNr == vNr) {
                        edges.remove(e);
                    }
                }

            }
            return res;//returns the decrease in edgeID.
        } else {
            System.err.println("Conflicting types: was expecting ByteBasedEdgeSet!");
            return 0;
        }

    }
    
    
    @Override
    public long smartAddIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo) {
        if (reference instanceof ByteBasedEdgeSet) {
            int[] incidentEdges = graphInfo.getIncidentEdges(vNr);
            long res = 0;
        //System.err.println("Next test: "+reference.ID);
            //System.err.println(reference.edges.size());
            
            for (int i = 0; i<incidentEdges.length;i++) {
                //System.err.println(String.valueOf(i));
                
                byte e = (byte)incidentEdges[i];

                byte otherNr = ((ByteBasedEdgeSet) reference).getOtherNode(e, (byte) vNr, graphInfo);
                if (otherNr != -1) {//this should always be the case, maybe drop the check?
                    res += br.getEdgeIDSummand(e, vNr, source, graphInfo);
                    if (!((ByteBasedEdgeSet) reference).contains(e)) {//pretty much unnecessary, since the check is done in edges.add anyway
                        edges.add(e);
                    }
                }

            }
            return res;//returns the increase in edgeID.
        } else {
            System.err.println("Conflicting types: was expecting ByteBasedEdgeSet!");
            return 0;
        }
    }

    /*public long computeEdgeIdBonus(int source, BitSet isSourceNode, BoundaryRepresentation br, SGraphBRDecompositionAutomaton graphInfo){
     ByteIterator li = edges.iterator();
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
    public long computeEdgeIdSummand(int vNr, int source, GraphInfo graphInfo){
        long res = 0;
        ByteIterator it = edges.iterator();
        while (it.hasNext()) {
           byte edge = it.nextByte();
           if (graphInfo.isIncident(vNr, edge)){
              res += BoundaryRepresentation.getEdgeIDSummand(edge, vNr, source, graphInfo);
           }
        }
        return res;
    }
    
    
    private byte getOtherNode(byte e, byte v, GraphInfo graphInfo) {
        byte source = (byte) graphInfo.getEdgeSource(e);
        byte target = (byte) graphInfo.getEdgeTarget(e);
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
        if (!(other instanceof ByteBasedEdgeSet)) {
            return false;
        }
        ByteBasedEdgeSet f = (ByteBasedEdgeSet) other;
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
        ByteIterator it = edges.iterator();
        while (it.hasNext()) {
            BitSet current = map.get(it.nextByte());
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
        ByteIterator it = edges.iterator();
        while (it.hasNext()) {
            byte next = it.nextByte();
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
        ByteIterator it = edges.iterator();
        if (it.hasNext()) {
            return it.nextByte();
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
        ByteIterator it = edges.iterator();
        while(it.hasNext()){
            action.accept(it.nextByte());
        }
    }
}
