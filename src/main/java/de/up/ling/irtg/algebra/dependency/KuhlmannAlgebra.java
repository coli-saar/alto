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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Iterator;
import java.util.List;

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
    public static final String TAG_PREFIX = "__TAG__";
    
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
    public static boolean isTagging(String s){
        return s.startsWith(TAG_PREFIX);
    }
    
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
    public static String getTaggingLabel(String s) {
        return s.substring(TAG_PREFIX.length());
    }
    
    /**
     * 
     * @param s
     * @return 
     */
    public String getRootEdgeLabel(String s) {
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
    public static String getEdgeMaxMinLabel(String s) {
        return s.substring(EDGE_MAX_MIN_PREFIX.length());
    }
    
    @Override
    protected NoncrossingGraph evaluate(String label, List<NoncrossingGraph> childrenValues) {
        if(isTagging(label) && childrenValues.isEmpty()) {
            this.getSignature().addSymbol(label, 1);
            
            return childrenValues.get(0).addTag(getTaggingLabel(label));
            
        } else if(introducesRootEdge(label) && childrenValues.size() == 1) {
          this.getSignature().addSymbol(label, 1);
          
          return childrenValues.get(0).addRootEdge(getRootEdgeLabel(label));
          
        } else if(introducesMinMaxEdge(label) && childrenValues.size() == 1) {
          this.getSignature().addSymbol(label, 1);
          
          return childrenValues.get(0).addMinMaxEdge(getEdgeMinMaxLabel(label));
          
        } else if(introducesMaxMinEdge(label) && childrenValues.size() == 1) {
          this.getSignature().addSymbol(label, 1);
          
          return childrenValues.get(0).addMaxMinEdge(getEdgeMaxMinLabel(label));
          
        } else if(label.equals(APPEND) && childrenValues.size() == 2) {
            return childrenValues.get(0).append(childrenValues.get(1));
            
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
         * @param value
         * @param aThis 
         */
        private NCDDecomposition(NoncrossingGraph value, KuhlmannAlgebra algebra) {
            super(algebra.getSignature());
            GraphBounds top = new GraphBounds(1, value.length());
            int start = this.addState(top);
            this.addFinalState(start);
            
            addTransitions(top,algebra,value, value.getType());
        }

        /**
         * 
         * @param state
         * @param algebra
         * @param value 
         */
        private void addTransitions(GraphBounds state, KuhlmannAlgebra algebra, NoncrossingGraph value,
                NoncrossingGraph.KuhlmannType kt) {
            if(state.from == state.to) {
                handleSingleNode(state,algebra,value);
            }
            
            if(kt.equals(NoncrossingGraph.KuhlmannType.MIN_MAX_COVERED)) {
                
                
                //TODO
            }else if(kt.equals(NoncrossingGraph.KuhlmannType.MAX_MIN_COVERED)) {
                //TODO
            }
            
            
            //TODO
            int left = state.from;
            
            Iterator<Int2ObjectMap.Entry<String>> nextOut = value.getFromSmaller(state.from, state.to);
            Iterator<Int2ObjectMap.Entry<String>> nextIn = value.getToSmaller(state.from, state.to);
            
            Int2ObjectMap.Entry<String> ent = null;
            boolean minMax = true;
            if(nextOut.hasNext()) {
                ent = nextOut.next();
            }
            if(nextIn.hasNext()) {
                Int2ObjectMap.Entry<String> alt = nextIn.next();
                
                if(ent.getIntKey() < alt.getIntKey()) {
                    ent = alt;
                    minMax = false;
                }
            }
            
            
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        /**
         * 
         * @param state
         * @param algebra
         * @param value 
         */
        private void handleSingleNode(GraphBounds state, KuhlmannAlgebra algebra, NoncrossingGraph value) {
            boolean couldTag = value.getTag(state.from) != null && !state.generatedTag;
            boolean couldRoot = value.rootEdge(state.from) != null && !state.generatedRootEdge;
            
            if(!couldRoot && !couldTag) {
                this.addRule(this.createRule(state, value.getNode(state.from), new GraphBounds[0]));
            }else {
                if(couldRoot) {
                    GraphBounds child = new GraphBounds(state.generatedTag, true, state.from);
                    String label = EDGE_ROOT_PREFIX+value.rootEdge(state.from);
                    
                    this.addRule(this.createRule(state, label, new GraphBounds[] {child}));
                    this.addTransitions(child, algebra, value, null);
                }else if(couldTag) {
                    GraphBounds child = new GraphBounds(true, state.generatedRootEdge, state.from);
                    String label = TAG_PREFIX+value.getTag(state.from);
                    
                    this.addRule(this.createRule(state, label, new GraphBounds[] {child}));
                    this.addTransitions(child, algebra, value, null);
                }
            }
        }
    }
    
    
    public static class GraphBounds {
        /**
         * 
         */
        private final boolean generatedTag;
        
        /**
         * 
         */
        private final boolean generatedRootEdge;
        
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
         * @param generatedTag
         * @param generatedRootEdge
         * @param position 
         */
        public GraphBounds(boolean generatedTag, boolean generatedRootEdge, int position) {
            this.generatedTag = generatedTag;
            this.generatedRootEdge = generatedRootEdge;
            this.from = position;
            this.to = position;
        }
        
        /**
         * 
         * @param from
         * @param to 
         */
        public GraphBounds(int from, int to) {
            this.from = from;
            this.to = to;
            
            this.generatedTag = false;
            this.generatedRootEdge = false;
        }
    }
    
}
