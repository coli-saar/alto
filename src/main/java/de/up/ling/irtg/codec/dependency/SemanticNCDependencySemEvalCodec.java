/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.dependency;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.dependency.KuhlmannAlgebra;
import de.up.ling.irtg.algebra.dependency.NoncrossingGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecMetadata;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 *
 * @author christoph_teichmann
 */
@CodecMetadata(name = "sem-dep", description = "SemEval style semantic dependency graphs", type = NoncrossingGraph.class)
public class SemanticNCDependencySemEvalCodec extends InputCodec<NoncrossingGraph> {
    
    /**
     * 
     */
    public static Iterable<Rule> EMPTY = () -> new Iterator<Rule>() {
        
        @Override
        public boolean hasNext() {
            return false;
        }
    
        @Override
        public Rule next() {
            throw new NoSuchElementException();
        }
    };
    
    /**
     * 
     */
    public static final String POSITIVE = "+";
    
    /**
     * 
     */
    public static final int PREAMBLE_FIELDS = 7;
    
    /**
     * 
     */
    private final KuhlmannAlgebra algebra = new KuhlmannAlgebra();
    
    
    @Override
    public NoncrossingGraph read(InputStream is) throws CodecParseException, IOException {
        List<String[]> lines = new ArrayList<>();
        
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            
            while((line = br.readLine()) != null) {
                line = line.trim();
                if(line.startsWith("#")){
                    continue;
                }
                
                if(line.isEmpty()) {
                    break;
                }
                
                String[] parts = line.split("\t");
                
                for(int i=0;i<parts.length;++i) {
                    parts[i] = parts[i].trim();
                }
                
                lines.add(parts);
            }
        }
        
        Int2IntMap predicateIDs = new Int2IntOpenHashMap();
        IntSet roots = new IntOpenHashSet();
        
        int pos = 0;
        for(int i=0;i<lines.size();++i) {
            String[] line = lines.get(i);
            
            if(line[4].equals(POSITIVE)) {
                roots.add(i);
            }
            
            if(line[5].equals(POSITIVE)) {
                predicateIDs.put(pos++, i);
            }
        }
        
        String[][] edges = new String[lines.size()][lines.size()];
        String[] nodes = new String[lines.size()];
        
        for(int i=0;i<lines.size();++i){
            String[] line = lines.get(i);
            nodes[i] = line[1];
            
            for(int arg=0;arg+PREAMBLE_FIELDS < line.length;++arg){
                String label = line[arg+PREAMBLE_FIELDS];
                
                edges[predicateIDs.get(arg)][i] = label;
            }
        }
        
        return findBestGraph(nodes,edges,roots);
    }

    /**
     * 
     * @param nodes
     * @param edges
     * @param roots
     * @return 
     */
    private NoncrossingGraph findBestGraph(String[] nodes, String[][] edges, IntSet roots) {
        TreeAutomaton<SpanWithType> ta = new ConstructionAutomaton(nodes, roots, edges);
        
        Tree<String> best = ta.viterbi();
        
        return this.algebra.evaluate(best);
    }
    
    /**
     * 
     */
    private class ConstructionAutomaton extends TreeAutomaton<SpanWithType> {
        
        /**
         * 
         */
        private final IntSet roots;
        
        /**
         * 
         */
        private final String[] nodes;
        
        /**
         * 
         */
        private final String[][] edges;
        
        /**
         * 
         * @param signature 
         */
        public ConstructionAutomaton(String[] nodes, IntSet roots, String[][] edges) {
            super(algebra.getSignature());
            
            if(nodes.length < 1) {
                throw new IllegalArgumentException("Cannot construct graph without nodes.");
            }
            
            Signature sig = algebra.getSignature();
            
            this.roots = roots;
            
            sig.addSymbol(KuhlmannAlgebra.makeRootLabel(""), 1);
            
            this.nodes = nodes;
            for(String node : nodes) {
                sig.addSymbol(node, 0);
            }
            
           this.edges = edges;
           for(String[] local : edges) {
               for(String edge : local) {
                   if(edge != null) {
                       sig.addSymbol(KuhlmannAlgebra.makeEdgeMaxMinLabel(edge), 1);
                       sig.addSymbol(KuhlmannAlgebra.makeEdgeMinMaxLabel(edge), 1);
                   }
               }
           }
           
           if(nodes.length == 1) {
               SpanWithType top;
               
               if(roots.contains(0)) {
                   top = new SpanWithType(0, true, NoncrossingGraph.KuhlmannType.BLAND);
               }else {
                   top = new SpanWithType(0, false, NoncrossingGraph.KuhlmannType.BLAND);
               }
               
               int state = this.addState(top);
               this.addFinalState(state);
           }else {
               for(NoncrossingGraph.KuhlmannType type : NoncrossingGraph.KuhlmannType.values()) {
                   int state = this.addState(new SpanWithType(0, nodes.length, type));
                   
                   this.addFinalState(state);
               }
           }
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            if(this.useCachedRuleBottomUp(labelId, childStates)) {
                return this.getRulesBottomUpFromExplicit(labelId, childStates);
            }
            
            String label = this.getSignature().resolveSymbolId(labelId);
            if(label.equals(KuhlmannAlgebra.APPEND)) {
                append(childStates, labelId);
            } else if(KuhlmannAlgebra.introducesRootEdge(label)) {
                makeRoot(childStates, labelId);
            } else if(KuhlmannAlgebra.introducesMinMaxEdge(label)) {
                if(childStates.length != 1) {
                    return EMPTY;
                }
                
                SpanWithType swt = this.getStateForId(childStates[0]);
                NoncrossingGraph.KuhlmannType pType = swt.type.addMinMaxEdge();
                String intended = edges[swt.start][swt.end-1];
                String term = KuhlmannAlgebra.getEdgeMinMaxLabel(label);
                
                introduceEdge(pType, intended, term, swt, labelId, childStates);
            } else if(KuhlmannAlgebra.introducesMaxMinEdge(label)) {
                if(childStates.length != 1) {
                    return EMPTY;
                }
                
                SpanWithType swt = this.getStateForId(childStates[0]);
                NoncrossingGraph.KuhlmannType pType = swt.type.addMaxMinEdge();
                String intended = edges[swt.end-1][swt.start];
                String term = KuhlmannAlgebra.getEdgeMaxMinLabel(label);
                
                introduceEdge(pType, intended, term, swt, labelId, childStates);
            } else if(childStates.length == 0) {               
                makeTerminal(label);
            }
            
            return this.getRulesTopDownFromExplicit(labelId, labelId);
        }

        /**
         * 
         * @param label 
         */
        public void makeTerminal(String label) {
            for(int i=0;i<nodes.length;++i) {
                if(nodes[i].equals(label)) {
                    SpanWithType swt = new SpanWithType(i, false, NoncrossingGraph.KuhlmannType.BLAND);
                    
                    Rule r = this.createRule(swt, label, new SpanWithType[0]);
                    
                    this.storeRuleBottomUp(r);
                }
            }
        }

        /**
         * 
         * @param pType
         * @param intended
         * @param term
         * @param swt
         * @param labelId
         * @param childStates
         * @return 
         */
        public void introduceEdge(NoncrossingGraph.KuhlmannType pType, String intended, String term, SpanWithType swt, int labelId, int[] childStates) {
            if (pType == null) {
                return;
            }
            if (!intended.equals(term)) {
                return;
            }
            SpanWithType parent = new SpanWithType(swt.start, swt.end, pType);
            int code = this.addState(parent);
            Rule r = this.createRule(code, labelId, childStates,2.0);
            this.storeRuleBottomUp(r);
        }

        /**
         * 
         * @param childStates
         * @param labelId
         * @return 
         */
        public void makeRoot(int[] childStates, int labelId) {
            if (childStates.length != 1) {
                return;
            }
            
            SpanWithType child = this.getStateForId(childStates[0]);
            if (child.end-child.start != 1 || child.rootConnectedIfSingular
                    || !this.roots.contains(child.start)) {
                return;
            }
            
            SpanWithType swt = new SpanWithType(child.start, true, child.type);
            int code = this.addState(swt);
            
            Rule r = this.createRule(code, labelId, childStates, 2.0);
            this.storeRuleBottomUp(r);
        }

        /**
         * 
         * @param childStates
         * @param labelId
         * @return 
         */
        public void append(int[] childStates, int labelId) {
            if (childStates.length != 2) {
                return;
            }
            
            SpanWithType left = this.getStateForId(childStates[0]);
            SpanWithType right = this.getStateForId(childStates[1]);
            
            int size = left.end-left.start;
            if (size == 1) {
                if (right.start == left.end) {
                    this.storeAppend(left, right, labelId, childStates);
                }
            } else {
                if (right.start == left.end+1) {
                    this.storeAppend(left, right, labelId, childStates);
                }
            }
        }

        /**
         * 
         * @param left
         * @param right
         * @param labelId
         * @param childStates 
         */
        public void storeAppend(SpanWithType left, SpanWithType right, int labelId, int[] childStates) {
            NoncrossingGraph.KuhlmannType kt = left.type.append(right.type);
            
            SpanWithType swt = new SpanWithType(left.start, right.end, kt);
            int code = this.addState(swt);
            
            Rule r = this.createRule(code, labelId, childStates, 1.0);
            this.storeRuleBottomUp(r);
        }

        @Override
        public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {            
            if(this.useCachedRuleTopDown(labelId, parentState)) {
                return this.getRulesTopDownFromExplicit(labelId, parentState);
            }
            
            String label = this.getSignature().resolveSymbolId(labelId);
            SpanWithType swt = this.getStateForId(parentState);
            
            if(KuhlmannAlgebra.introducesRootEdge(label)) {
                handleRootEdgeTopDown(swt, label);
            }
            
            //TODO handle more cases
            //ACTUALLY DERIVE THE RULES
            
            
            return this.getRulesTopDownFromExplicit(labelId, parentState);
        }

        /**
         * 
         * @param swt
         * @param label 
         */
        public void handleRootEdgeTopDown(SpanWithType swt, String label) {
            int dist = swt.end-swt.start;
            
            if(dist == 1 && swt.rootConnectedIfSingular) {
                SpanWithType child = new SpanWithType(swt.start, false, swt.type);
                
                Rule r = this.createRule(swt, label, new SpanWithType[] {child}, 2.0);
            }
        }
        
        @Override
        public boolean isBottomUpDeterministic() {
            return false;
        }
    }
    
    /**
     * 
     */
    private class SpanWithType extends StringAlgebra.Span {
        /**
         * 
         */
        private final NoncrossingGraph.KuhlmannType type;
        
        /**
         * 
         */
        private final boolean rootConnectedIfSingular;
        
        /**
         * 
         * @param start
         * @param end
         * @param type 
         */
        public SpanWithType(int start, int end, NoncrossingGraph.KuhlmannType type) {
            super(start, end);
            this.type = type;
            this.rootConnectedIfSingular = false;
        }
        
        /**
         * 
         * @param start
         * @param end
         * @param type 
         */
        public SpanWithType(int start, boolean rootConnected, NoncrossingGraph.KuhlmannType type) {
            super(start, start+1);
            this.type = type;
            this.rootConnectedIfSingular = rootConnected;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + this.end;
            hash = 53 * hash + this.start;
            hash = 53 * hash + Objects.hashCode(this.type);
            hash = 53 * hash + (this.rootConnectedIfSingular ? 1 : 0);
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
            final SpanWithType other = (SpanWithType) obj;
            if (this.end != other.end) {
                return false;
            }
            if (this.start != other.start) {
                return false;
            }
            if (this.type != other.type) {
                return false;
            }
            return this.rootConnectedIfSingular == other.rootConnectedIfSingular;
        }
    }
}
