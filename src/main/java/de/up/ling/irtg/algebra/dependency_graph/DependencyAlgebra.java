/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.dependency_graph;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.SemEvalDependencyFormat;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class DependencyAlgebra extends Algebra<DependencyGraph> {

    /**
     * 
     */
    public static final String CONCAT = "*";
    
    /**
     * 
     */
    public static final String EDGE_PREFIX = "EDGE_";
    
    /**
     * 
     */
    public static final String FORGET_PREFIX = "f_";
    
    /**
     * 
     */
    public static final String RENAME_PREFIX = "r_";
    /**
     * 
     */
    private final Function<String,String> namer;

    /**
     * 
     */
    private final SemEvalDependencyFormat sedf;
    
    /**
     * 
     * @param namer 
     */
    public DependencyAlgebra(Function<String, String> namer) {
        this.sedf = new SemEvalDependencyFormat();
        this.namer = namer;
        
        this.getSignature().addSymbol(CONCAT, 2);
    }
    
    /**
     * 
     */
    public DependencyAlgebra() {
        this((String s) -> s);
    }
    
    @Override
    protected DependencyGraph evaluate(String label, List<DependencyGraph> childrenValues) {
        if(label.equals(CONCAT)) {
            if(childrenValues.size() != 2) {
                return null;
            } else {
                return childrenValues.get(0).concatenate(childrenValues.get(1));
            }
        } else if(isForget(label)) {
            String[] parts = getInfo(label);
            if(childrenValues.size() != 1) {
                return null;
            }
            int size = parts.length;
            if(size == 2 || size == 3) {
                this.signature.addSymbol(label, 1);
                
                if(size == 2) {
                    return childrenValues.get(0).forget(parts[1]);
                } else {
                    return childrenValues.get(0).forget(parts[1],Integer.parseInt(parts[2]));
                }
            } else {
                return null;
            }
        } else if(isRename(label)) {
            if(childrenValues.size() != 1) {
                return null;
            }
            
            String[] parts = getInfo(label);
            
            int size = parts.length;
            if(size == 3 || size == 4) {
                this.signature.addSymbol(label, 1);
                
                if(size == 3) {
                    return childrenValues.get(0).renameOpen(parts[1],parts[2]);
                } else {
                    return childrenValues.get(0).renameOpen(parts[1],Integer.parseInt(parts[2]),parts[3]);
                }
            } else {
                return null;
            }
        } else if(isEdge(label)) {
            if(childrenValues.size() != 1) {
                return null;
            }
            
            String[] parts = getInfo(label);
            
            int size = parts.length;
            //from=2,to=2,label=1
            if(size == 6) {
                this.signature.addSymbol(label, 1);
                
                return childrenValues.get(0).addEdge(parts[1],
                        Integer.parseInt(parts[2]), parts[3], Integer.parseInt(parts[4]), parts[5]);
            } else {
                return null;
            }
        } else {
            if(!childrenValues.isEmpty()) {
                return null;
            }
            
            this.signature.addSymbol(label, 0);
            
            DependencyGraph dg = new DependencyGraph();
            dg.addNode(label);
            
            return dg;
        }
    }

    @Override
    public DependencyGraph parseString(String representation) throws ParserException {
        return this.sedf.read(representation);
    }

    @Override
    public TreeAutomaton decompose(DependencyGraph value) {
        
        
        //TODO
        return null;
    }
    
    /**
     * 
     * @param label
     * @return 
     */
    public static boolean isForget(String label) {
        return label.startsWith(label);
    }
    
    /**
     * 
     * @param name
     * @param pos
     * @return 
     */
    public static String makeForget(String name, int pos) {
        return FORGET_PREFIX+name+"_"+pos;
    }

    /**
     * 
     * @param label
     * @return 
     */
    private static String[] getInfo(String label) {
        return label.split("_");
    }

    /**
     * 
     * @param label
     * @return 
     */
    public static boolean isRename(String label) {
        return label.startsWith(RENAME_PREFIX);
    }
    
    /**
     * 
     * @param name
     * @param number
     * @return 
     */
    public static String makeRename(String name, int number) {
        return RENAME_PREFIX+name+"_"+number;
    }
    
    /**
     * 
     * @param label
     * @return 
     */
    public static boolean isEdge(String label) {
        return label.startsWith(label);
    }
    
    /**
     * 
     * @param fromName
     * @param fromNumber
     * @param toName
     * @param toNumber
     * @param name
     * @return 
     */
    public static String makeEdge(String fromName, int fromNumber, String toName, int toNumber,
                                    String name) {
        return EDGE_PREFIX+fromName+"_"+fromNumber+"_"+toName+"_"+toNumber+"_"+name;
    }
    
    /**
     * 
     */
    public static class SpanWithForget {
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
        private final int leastToForget;

        /**
         * 
         * @param from
         * @param to
         * @param leastToForget 
         */
        public SpanWithForget(int from, int to, int leastToForget) {
            this.from = from;
            this.to = to;
            this.leastToForget = leastToForget;
        }
    }
    
    
}
