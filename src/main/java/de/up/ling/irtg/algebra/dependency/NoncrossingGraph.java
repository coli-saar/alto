/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.dependency;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 *  The root is always positioned at 0.
 * 
 * @author christoph_teichmann
 */
public class NoncrossingGraph {
    /**
     * 
     */
    public static final String ROOT_NAME = "ROOT";
    
    /**
     * 
     */
    public static final String ROOT_TAG = "ROOT_TAG";
    
    /**
     * 
     */
    private final String[] nodes;
    
    /***
     * 
     */
    private final String[] tags;
    
    /**
     * 
     */
    private final Int2ObjectSortedMap<String>[] edges;
    
    /**
     * 
     */
    private final Int2ObjectSortedMap<String>[] reverseEdges;
    
    
    /**
     * 
     */
    private final KuhlmannType type;

    /**
     * 
     * @param nodes
     * @param tags
     * @param edges
     * @param type 
     */
    private NoncrossingGraph(String[] nodes, String[] tags,
            Int2ObjectSortedMap<String>[] edges, Int2ObjectSortedMap<String>[] reverse,
            KuhlmannType type) {
        this.nodes = nodes;
        this.tags = tags;
        this.edges = edges;
        this.reverseEdges = reverse;
        this.type = type;
    }
    
    /**
     * 
     * @param nodeName 
     */
    public NoncrossingGraph(String nodeName) {
        this.nodes = new String[] {ROOT_NAME,nodeName};
        this.tags = new String[] {ROOT_TAG,null};
        this.edges = new Int2ObjectSortedMap[] {new Int2ObjectAVLTreeMap<>(),new Int2ObjectAVLTreeMap<>()};
        this.reverseEdges = new Int2ObjectSortedMap[] {new Int2ObjectAVLTreeMap<>(),new Int2ObjectAVLTreeMap<>()};
        this.type = KuhlmannType.BLAND;
    }

    /**
     * 
     * @param aThis
     * @param kt 
     */
    private NoncrossingGraph(NoncrossingGraph aThis, KuhlmannType kt) {
        this.nodes = aThis.nodes;
        this.tags = aThis.tags;
        this.edges = aThis.edges;
        this.reverseEdges = aThis.reverseEdges;
        
        this.type = kt;
    }

    /**
     * 
     * @param position
     * @param largerEqual
     * @return 
     */
    public Iterator<Int2ObjectMap.Entry<String>> getToLarger(int position, int largerEqual) {
        return this.reverseEdges[position].tailMap(largerEqual).int2ObjectEntrySet().iterator();
    }
    
    /**
     * 
     * @param position
     * @param lessThan
     * @return 
     */
    public Iterator<Int2ObjectMap.Entry<String>> getToSmaller(int position, int lessThan){
        return this.reverseEdges[position].headMap(lessThan).int2ObjectEntrySet().iterator();
    }
    
    /**
     * 
     * @param position
     * @param lessThan
     * @return 
     */
    public Iterator<Int2ObjectMap.Entry<String>> getFromSmaller(int position, int lessThan){
        return this.edges[position].headMap(lessThan).int2ObjectEntrySet().iterator();
    }
    
    /**
     * 
     * @param position
     * @param moreThan
     * @return 
     */
    public Iterator<Int2ObjectMap.Entry<String>> getFromLarger(int position, int moreThan){
        return this.edges[position].headMap(moreThan).int2ObjectEntrySet().iterator();
    }
    
    /**
     * 
     * @param tag
     * @return 
     */
    public NoncrossingGraph addTag(String tag) {
        if(this.length() > 2 || this.tags[1] != null || !this.edges[0].isEmpty()) {
            return null;
        }else {
            NoncrossingGraph copy = new NoncrossingGraph(this.getNode(1));
            
            copy.tags[1] = tag;
            return copy;
        }
    }
    
    /**
     * 
     * @param edgeLabel
     * @return 
     */
    public NoncrossingGraph addMinMaxEdge(String edgeLabel) {
        return this.addEdge(edgeLabel, true);
    }
    
    /**
     * 
     * @param edgeLabel
     * @return 
     */
    public NoncrossingGraph addMaxMinEdge(String edgeLabel) {
        return this.addEdge(edgeLabel, false);
    }
    
    /**
     * 
     * @param position
     * @return 
     */
    public String rootEdge(int position) {
        return this.reverseEdges[position].get(0);
    }
    
    /**
     * 
     * @param edgeLabel
     * @param minMax
     * @return 
     */
    private NoncrossingGraph addEdge(String edgeLabel, boolean minMax) {
        if(this.length()<3){
            return null;
        }
        
        NoncrossingGraph ng = null;
        if(minMax) {
            KuhlmannType kt = this.type.addMinMaxEdge();
            if(kt != null) {
                ng = new NoncrossingGraph(this,kt);
                
                int pos = this.length()-1;
                ng.edges[1] = new Int2ObjectAVLTreeMap<>(this.edges[1]);
                ng.edges[1].put(pos, edgeLabel);
                
                ng.reverseEdges[pos] = new Int2ObjectAVLTreeMap<>(this.reverseEdges[pos]);
                ng.reverseEdges[pos].put(1, edgeLabel);
            }
        }else {
            KuhlmannType kt = this.type.addMaxMinEdge();
            if(kt != null){
                ng = new NoncrossingGraph(this,kt);
            
                int pos = this.length()-1;
                ng.edges[pos] = new Int2ObjectAVLTreeMap<>(this.edges[pos]);
                ng.edges[pos].put(1, edgeLabel);
                
                ng.reverseEdges[1] = new Int2ObjectAVLTreeMap<>(this.reverseEdges[1]);
                ng.reverseEdges[1].put(pos, edgeLabel);
            }
        }
        
        return ng;
    }
    
    /**
     * 
     * @param label
     * @return 
     */
    public NoncrossingGraph addRootEdge(String label) {
        if(this.length() > 2  || !this.reverseEdges[0].isEmpty()) {
            return null;
        }else {
            NoncrossingGraph result = new NoncrossingGraph(this, this.type);
            
            result.edges[0].put(1,label);
            
            result.reverseEdges[1] = new Int2ObjectAVLTreeMap<>();
            result.reverseEdges[1].put(0, label);
            
            return result;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NoncrossingGraph other = (NoncrossingGraph) obj;
        if (!Arrays.deepEquals(this.nodes, other.nodes)) {
            return false;
        }
        if (!Arrays.deepEquals(this.tags, other.tags)) {
            return false;
        }
        if (!Arrays.deepEquals(this.edges, other.edges)) {
            return false;
        }
        if (!Arrays.deepEquals(this.reverseEdges, other.reverseEdges)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Arrays.deepHashCode(this.nodes);
        hash = 41 * hash + Arrays.deepHashCode(this.tags);
        hash = 41 * hash + Arrays.deepHashCode(this.edges);
        hash = 41 * hash + Arrays.deepHashCode(this.reverseEdges);
        hash = 41 * hash + Objects.hashCode(this.type);
        return hash;
    }
    
    /**
     * 
     * @param nc
     * @return 
     */
    public NoncrossingGraph append(NoncrossingGraph nc) {
        KuhlmannType kt = this.type.append(nc.type);
        
        int offset = this.length()-1;
        
        String[] newNodes = Arrays.copyOf(this.nodes, offset+nc.length());
        
        Int2ObjectSortedMap<String>[] newEdges = Arrays.copyOf(this.edges, offset+nc.length());
        Int2ObjectSortedMap<String>[] newReverse = Arrays.copyOf(this.reverseEdges, offset+nc.length());
                
        String[] newTags = Arrays.copyOf(this.tags, offset+nc.length());
        
        
        Iterator<Int2ObjectMap.Entry<String>> rootEdges = nc.getFromLarger(0, 0);
        while(rootEdges.hasNext()) {
            Entry<String> e = rootEdges.next();
            
            newEdges[0].put(e.getIntKey()+offset, e.getValue());
        }
        
        for(int i=1;i<nc.length();++i) {
            int herePos = i+offset;
            newNodes[herePos] = nc.getNode(i);
            newTags[herePos] = nc.getTag(i);
            
            Int2ObjectSortedMap<String> outgoing = newEdges[herePos] = new Int2ObjectAVLTreeMap<>();
            Int2ObjectSortedMap<String> incoming = newReverse[herePos] = new Int2ObjectAVLTreeMap<>();
            
            Iterator<Int2ObjectMap.Entry<String>> it = nc.getFromLarger(i, 0);
            while(it.hasNext()) {
                Entry<String> e = it.next();
                outgoing.put(e.getIntKey()+offset, e.getValue());
            }
            
            it = nc.getToLarger(i, 0);
            while(it.hasNext()) {
                Entry<String> e = it.next();
                incoming.put(e.getIntKey(), e.getValue());
            }
        }
        
        return new NoncrossingGraph(nodes, tags, edges, newReverse, type);
    }
    
    /**
     * 
     * @return 
     */
    public int length() {
        return this.nodes.length;
    }

    /**
     * 
     * @param position
     * @return 
     */
    public String getNode(int position) {
        return this.nodes[position];
    }

    /**
     * 
     * @return 
     */
    public KuhlmannType getType() {
        return type;
    }

    /**
     * 
     * @param pos
     * @return 
     */
    public String getTag(int pos) {
        return this.tags[pos];
    }
    
    /**
     * 
     */
    public enum KuhlmannType {
        /**
         * 
         */
        MIN_MAX_COVERED {

            @Override
            public KuhlmannType addMinMaxEdge() {
                return null;
            }

            @Override
            public KuhlmannType addMaxMinEdge() {
                return null;
            }

            @Override
            public KuhlmannType append(KuhlmannType with) {
                switch(with) {
                    case MIN_MAX_COVERED:
                        return MIN_MAX_CONNECTED;
                    case MAX_MIN_COVERED:
                        return BLAND;
                    case MIN_MAX_CONNECTED:
                        return MIN_MAX_CONNECTED;
                    case MAX_MIN_CONNECTED:
                        return BLAND;
                    case BLAND:
                        return BLAND;
                    default:
                        return null;
                }
            }
        },
        /**
         * 
         */
        MAX_MIN_COVERED {
            @Override
            public KuhlmannType addMinMaxEdge() {
                return null;
            }

            @Override
            public KuhlmannType addMaxMinEdge() {
                return null;
            }

            @Override
            public KuhlmannType append(KuhlmannType with) {
                switch(with){
                    case MIN_MAX_COVERED:
                        return BLAND;
                    case MAX_MIN_COVERED:
                        return MAX_MIN_CONNECTED;
                    case MIN_MAX_CONNECTED:
                        return BLAND;
                    case MAX_MIN_CONNECTED:
                        return MAX_MIN_CONNECTED;
                    case BLAND:
                        return BLAND;
                    default:
                        return null;
                }
            }

        },
        MIN_MAX_CONNECTED {
            @Override
            public KuhlmannType addMinMaxEdge() {
                return MIN_MAX_COVERED;
            }

            @Override
            public KuhlmannType addMaxMinEdge() {
                return null;
            }

            @Override
            public KuhlmannType append(KuhlmannType with) {
                switch(with){
                    case MIN_MAX_COVERED:
                        return MIN_MAX_CONNECTED;
                    case MAX_MIN_COVERED:
                        return BLAND;
                    case MIN_MAX_CONNECTED:
                        return MIN_MAX_CONNECTED;
                    case MAX_MIN_CONNECTED:
                        return BLAND;
                    case BLAND:
                        return BLAND;
                    default:
                        return null;
                }
            }
        },
        MAX_MIN_CONNECTED {
            @Override
            public KuhlmannType addMinMaxEdge() {
                return null;
            }

            @Override
            public KuhlmannType addMaxMinEdge() {
                return MAX_MIN_COVERED;
            }

            @Override
            public KuhlmannType append(KuhlmannType with) {
                switch(with){
                    case MIN_MAX_COVERED:
                        return BLAND;
                    case MAX_MIN_COVERED:
                        return MAX_MIN_CONNECTED;
                    case MIN_MAX_CONNECTED:
                        return BLAND;
                    case MAX_MIN_CONNECTED:
                        return BLAND;
                    case BLAND:
                        return BLAND;
                    default:
                        return BLAND;
                }
            }
        },
        BLAND {
            @Override
            public KuhlmannType addMinMaxEdge() {
                return MIN_MAX_COVERED;
            }

            @Override
            public KuhlmannType addMaxMinEdge() {
                return MAX_MIN_COVERED;
            }

            @Override
            public KuhlmannType append(KuhlmannType with) {
                return BLAND;
            }
        };
        
        /**
         * 
         * @return 
         */
        public abstract KuhlmannType addMinMaxEdge();
        
        /**
         * 
         * @return 
         */
        public abstract KuhlmannType addMaxMinEdge();
        
        /**
         * 
         * @param with
         * @return 
         */
        public abstract KuhlmannType append(KuhlmannType with);
    }

    @Override
    public String toString() {
        return "NoncrossingGraph{" + "nodes=" + nodes + ", tags=" + tags + ", edges=" + edges + ", reverseEdges=" + reverseEdges + ", type=" + type + '}';
    }
}
