/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.dependency;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

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
    private final TreeSet<Edge>[] edges;
    
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
    private NoncrossingGraph(String[] nodes, String[] tags, TreeSet<Edge>[] edges, KuhlmannType type) {
        this.nodes = nodes;
        this.tags = tags;
        this.edges = edges;
        this.type = type;
    }
    
    /**
     * 
     * @param nodeName 
     */
    public NoncrossingGraph(String nodeName) {
        this.nodes = new String[] {ROOT_NAME,nodeName};
        this.tags = new String[] {ROOT_TAG,null};
        this.edges = new TreeSet[] {new TreeSet<>(),new TreeSet<>()};
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
        
        this.type = kt;
    }
    
    /**
     * 
     * @param tag
     * @return 
     */
    public NoncrossingGraph addTag(String tag) {
        if(this.length() > 2 || this.tags[1] != null) {
            return null;
        }else {
            NoncrossingGraph copy = new NoncrossingGraph(this.getNodeName(1));
            
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
                
                ng.edges[1] = new TreeSet<>(this.edges[1]);
                ng.edges[1].add(new Edge(1, this.length()-1, edgeLabel));
            }
        }else {
            KuhlmannType kt = this.type.addMaxMinEdge();
            if(kt != null){
                ng = new NoncrossingGraph(this,kt);
            
                int pos = this.length()-1;
                ng.edges[pos] = new TreeSet<>(this.edges[1]);
                ng.edges[pos].add(new Edge(pos, 1, edgeLabel));
            }
        }
        
        return ng;
    }
    
    /**
     * 
     * @param position
     * @param e
     * @return 
     */
    public Edge getNextShorterEdge(int position, Edge e) {
        TreeSet<Edge> ts = this.edges[position];
        if(e == null) {
            return ts.first();
        } else {
            return ts.floor(e);
        }
    }
    
    /**
     * 
     * @param label
     * @return 
     */
    public NoncrossingGraph addRootEdge(String label) {
        if(this.length() > 2) {
            return null;
        }else {
            NoncrossingGraph result = new NoncrossingGraph(this, this.type);
            
            result.edges[1] = new TreeSet<>(this.edges[1]);
            result.edges[1].add(new Edge(0, 1, label));
            
            return result;
        }
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
        TreeSet<Edge>[] newEdges = Arrays.copyOf(this.edges, offset+nc.length());
        String[] newTags = Arrays.copyOf(this.tags, offset+nc.length());
        
        
        Iterator<Edge> rootEdges = nc.getDecreasingIterator(0);
        while(rootEdges.hasNext()) {
            newEdges[0].add(rootEdges.next().shiftBy(offset));
        }
        
        for(int i=1;i<nc.length();++i) {
            int herePos = i+offset;
            newNodes[herePos] = nc.getNodeName(i);
            newTags[herePos] = nc.getTag(i);
            
            TreeSet<Edge> set = newEdges[herePos] = new TreeSet<>();
            Iterator<Edge> it = nc.getDecreasingIterator(i);
            while(it.hasNext()) {
                Edge e = it.next();
                e = e.shiftBy(offset);
                
                set.add(e);
            }
        }
        
        return new NoncrossingGraph(newNodes, newTags, newEdges, kt);
    }
    
    /**
     * 
     * @param position
     * @param e
     * @return 
     */
    public Edge getNextLongerEdge(int position, Edge e) {
        TreeSet<Edge> ts = this.edges[position];
        if(e == null) {
            return ts.last();
        }else {
            return ts.ceiling(e);
        }
    }
    
    /**
     * 
     * @param pos
     * @return 
     */
    public Iterator<Edge> getDecreasingIterator(int pos) {
        return this.edges[pos].iterator();
    }
    
    /**
     * 
     * @param pos
     * @return 
     */
    public Iterator<Edge> getIncreasingIterator(int pos) {
        return this.edges[pos].descendingIterator();
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
    private String getNodeName(int position) {
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
    public class Edge implements Comparable<Edge> {
        /**
         * 
         */
        private final int from;
        
        /**
         * 
         */
        private final int to;
        
        /**
         * 
         */
        private final String label;

        /**
         * 
         * @param from
         * @param to
         * @param label 
         */
        public Edge(int from, int to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }
        
        /**
         * 
         * @param number
         * @return 
         */
        private Edge shiftBy(int number) {
            if(from == 0) {
                return new Edge(from, to+number, label);
            }else {
                return new Edge(from+number, to+number, label);
            }
        }
        
        @Override
        public int compareTo(Edge o) {          
            return -Integer.compare(this.length(), o.length());
        }
        
        /**
         * 
         * @return 
         */
        public int length() {
            return this.to < this.from ? this.from-this.to : this.to-this.from;
        }

        /**
         * 
         * @return 
         */
        public int getFrom() {
            return from;
        }

        /**
         * 
         * @return 
         */
        public int getTo() {
            return to;
        }

        /**
         * 
         * @return 
         */
        public String getLabel() {
            return label;
        }
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
}
