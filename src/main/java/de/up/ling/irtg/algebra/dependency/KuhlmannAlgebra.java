/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.dependency;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author christoph_teichmann
 */
public class KuhlmannAlgebra extends Algebra<NoncrossingGraph> {
    /**
     * 
     */
    public static final String APPEND = "*";
    
    /**
     * 
     */
    public static final String EDGE_MIN_MAX_PREFIX = "__MIN__";
    
    /**
     * 
     */
    public static final String EDGE_MAX_MIN_PREFIX = "__MAX__";
    
    /**
     * 
     */
    public static final String EDGE_ROOT_PREFIX = "__ROOT__";
    
    /**
     * 
     * @param s
     * @return 
     */
    public static boolean introducesRootEdge(String s) {
       return s.startsWith(EDGE_ROOT_PREFIX);
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static boolean introducesMinMaxEdge(String s) {
            return s.startsWith(EDGE_MIN_MAX_PREFIX);
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static boolean introducesMaxMinEdge(String s) {
            return s.startsWith(EDGE_MAX_MIN_PREFIX);
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static String getRootEdgeLabel(String s) {
        return s.substring(EDGE_ROOT_PREFIX.length());
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static String getEdgeMinMaxLabel(String s) {
        return s.substring(EDGE_MIN_MAX_PREFIX.length());
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static String makeRootLabel(String s) {
        return EDGE_ROOT_PREFIX+s;
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static String makeEdgeMinMaxLabel(String s) {
        return EDGE_MIN_MAX_PREFIX+s;
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static String makeEdgeMaxMinLabel(String s) {
        return EDGE_MAX_MIN_PREFIX+s;
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public static String getEdgeMaxMinLabel(String s) {
        return s.substring(EDGE_MAX_MIN_PREFIX.length());
    }
    
    /**
     * 
     */
    public KuhlmannAlgebra() {
        super.getSignature().addSymbol(APPEND, 2);
    }
    
    
    @Override
    protected NoncrossingGraph evaluate(String label, List<NoncrossingGraph> childrenValues) {
        if(introducesRootEdge(label) && childrenValues.size() == 1) {
          this.getSignature().addSymbol(label, 1);
          
          return childrenValues.get(0).addRootEdge(getRootEdgeLabel(label));
          
        } else if(introducesMinMaxEdge(label) && childrenValues.size() == 1) {
          this.getSignature().addSymbol(label, 1);
          
          return childrenValues.get(0).addMinMaxEdge(getEdgeMinMaxLabel(label));
          
        } else if(introducesMaxMinEdge(label) && childrenValues.size() == 1) {
          this.getSignature().addSymbol(label, 1);
          
          return childrenValues.get(0).addMaxMinEdge(getEdgeMaxMinLabel(label));
          
        } else if(label.equals(APPEND) && childrenValues.size() == 2) {
            return childrenValues.get(0).connect(childrenValues.get(1));
            
        } else if(childrenValues.isEmpty()) {
            this.getSignature().addSymbol(label, 0);
            return new NoncrossingGraph(label);
            
        }else {
            return null;
        }
    }

    @Override
    public NoncrossingGraph parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TreeAutomaton decompose(NoncrossingGraph value) {
        return new NCDDecomposition(value,this);
    }
    
    
    /**
     * 
     */
    private static class NCDDecomposition extends ConcreteTreeAutomaton<GraphBounds>{
        /**
         * 
         */
        private Set<GraphBounds> seen;
        
        /**
         * 
         * @param value
         * @param aThis 
         */
        private NCDDecomposition(NoncrossingGraph value, KuhlmannAlgebra algebra) {
            super(algebra.getSignature());
            this.seen = new HashSet<>();
            
            GraphBounds top = new GraphBounds(1, value.length(),false);
            int start = this.addState(top);
            this.addFinalState(start);
            
            addTransitions(top,algebra,value);
            this.seen = null;
        }

        /**
         * 
         * @param state
         * @param algebra
         * @param value 
         */
        private void addTransitions(GraphBounds state, KuhlmannAlgebra algebra, NoncrossingGraph value) {
            if(this.seen.contains(state)) {
                return;
            }else{
                this.seen.add(state);
            }
            
            if(state.from == state.to) {
                handleSingleNode(state,algebra,value);
            }
            
            int left = state.from;
            
            Int2ObjectSortedMap<String> nextOut = value.getFromSmallerOrEquals(left, state.edgeDone ? state.to-1 : state.to);
            Int2ObjectSortedMap<String> nextIn = value.getToSmallerOrEquals(left, state.edgeDone ? state.to-1 : state.to);
            
            int longestEdge = -1;
            boolean minMax = true;
            if(!nextOut.isEmpty()) {
                int lon = nextOut.lastIntKey();
                if(lon > left) {
                    longestEdge = lon;
                }
            }
            
            if(!nextIn.isEmpty()) {
                int lon = nextIn.lastIntKey();
                
                if(lon > left && lon > longestEdge) {
                    longestEdge = lon;
                }
            }
            
            if(longestEdge == state.to) {
                this.handleCover(minMax,state,algebra,value);
            }else {
                IntArrayList splits = new IntArrayList();
                
                if(longestEdge > left) {
                    splits.add(longestEdge);
                }else{
                    splits.add(left);
                    splits.add(left+1);
                }
                
                addAllSplitsBetween(splits,left,state.to,value);
                
                for(int i=0;i<splits.size();++i) {
                    handleSplit(splits.getInt(i),state,algebra,value);
                }
            }
        }

        /**
         * 
         * @param state
         * @param algebra
         * @param value 
         */
        private void handleSingleNode(GraphBounds state, KuhlmannAlgebra algebra, NoncrossingGraph value) {
            boolean couldRoot = value.rootEdge(state.from) != null && !state.edgeDone;
            
            if(!couldRoot) {
                GraphBounds child = new GraphBounds(true, state.from);
                String label = EDGE_ROOT_PREFIX+value.rootEdge(state.from);
                    
                this.addRule(this.createRule(state, label, new GraphBounds[] {child}));
                this.addTransitions(child, algebra, value);
            }else {
                this.addRule(this.createRule(state, value.getNode(state.from), new GraphBounds[0]));
            }
        }
        
        /**
         * 
         * @param minMax
         * @param state
         * @param algebra
         * @param value 
         */
        private void handleCover(boolean minMax, GraphBounds state, KuhlmannAlgebra algebra, NoncrossingGraph value) {
            String name = minMax ? value.getEdge(state.from,state.to) : value.getEdge(state.to,state.from);
            
            String label = (minMax ? EDGE_MIN_MAX_PREFIX : EDGE_MAX_MIN_PREFIX) + name;
            GraphBounds child = new GraphBounds(state.from, state.to, true);
            
            this.addRule(this.createRule(state, label, new GraphBounds[] {child}));
            
            this.addTransitions(child, algebra, value);
        }

        /**
         * 
         * @param splits
         * @param left
         * @param to 
         */
        private void addAllSplitsBetween(IntArrayList splits, int left, int to,
                                           NoncrossingGraph value) {
            for(int i=splits.size()-1;i<splits.size();++i) {
                int old = splits.get(i);
                
                if(old == to) {
                    return;
                }
                
                int longestEdge = -1;
                
                Int2ObjectSortedMap<String> m = value.getFromSmallerOrEquals(old, to);
                if(!m.isEmpty()) {
                    longestEdge = m.lastIntKey();
                }
                
                m = value.getToSmallerOrEquals(old, to);
                if(!m.isEmpty()) {
                    int lon = m.lastIntKey();
                    if(lon > longestEdge) {
                        longestEdge = lon;
                    }
                }
                
                
                if(longestEdge > old) {
                    splits.add(longestEdge);
                }else {
                    splits.add(old+1);
                }
            }
        }

        /**
         * 
         * @param get
         * @param state
         * @param algebra
         * @param value 
         */
        private void handleSplit(int middle, GraphBounds state, KuhlmannAlgebra algebra,
                NoncrossingGraph value) {
            int left = state.from;
            int right = state.to;
            
            GraphBounds lChild = new GraphBounds(left, middle, false);
            GraphBounds rChild = new GraphBounds(middle, right, false);
            
            this.addRule(this.createRule(state, APPEND, new GraphBounds[] {lChild,rChild}));
            
            this.addTransitions(lChild, algebra, value);
            this.addTransitions(rChild, algebra, value);
        }
    }
    
    /**
     * 
     */
    public static class GraphBounds {
        /**
         * 
         */
        private final boolean edgeDone;
        
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
         * @param generatedRootEdge
         * @param position 
         */
        public GraphBounds(boolean generatedRootEdge, int position) {
            this.edgeDone = generatedRootEdge;
            this.from = position;
            this.to = position;
        }
        
        /**
         * 
         * @param from
         * @param to 
         * @param generatedEdge 
         */
        public GraphBounds(int from, int to, boolean generatedEdge) {
            this.from = from;
            this.to = to;
            
            this.edgeDone = generatedEdge;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + (this.edgeDone ? 1 : 0);
            hash = 59 * hash + this.from;
            hash = 59 * hash + this.to;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GraphBounds other = (GraphBounds) obj;
            if (this.edgeDone != other.edgeDone) {
                return false;
            }
            if (this.from != other.from) {
                return false;
            }
            
            return this.to == other.to;
        }
    }
}
