/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.dependency_graph;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.SemEvalDependencyFormat;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
    public static final String ROOT_PREFIX = "root_";
    
    /**
     *
     */
    private final Function<String, String> namer;

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
        if (label.equals(CONCAT)) {
            if (childrenValues.size() != 2) {
                return null;
            } else {
                return childrenValues.get(0).concatenate(childrenValues.get(1));
            }
        } else if (isForget(label)) {
            String[] parts = getInfo(label);
            if (childrenValues.size() != 1) {
                return null;
            }
            int size = parts.length;
            if (size == 2 || size == 3) {
                this.signature.addSymbol(label, 1);

                if (size == 2) {
                    return childrenValues.get(0).forget(parts[1]);
                } else {
                    return childrenValues.get(0).forget(parts[1], Integer.parseInt(parts[2]));
                }
            } else {
                return null;
            }
        } else if (isRename(label)) {
            if (childrenValues.size() != 1) {
                return null;
            }

            String[] parts = getInfo(label);

            int size = parts.length;
            if (size == 3 || size == 4) {
                this.signature.addSymbol(label, 1);

                if (size == 3) {
                    return childrenValues.get(0).renameOpen(parts[1], parts[2]);
                } else {
                    return childrenValues.get(0).renameOpen(parts[1], Integer.parseInt(parts[2]), parts[3]);
                }
            } else {
                return null;
            }
        } else if (isEdge(label)) {
            if (childrenValues.size() != 1) {
                return null;
            }

            String[] parts = getInfo(label);

            int size = parts.length;
            //from=2,to=2,label=1
            if (size == 6) {
                this.signature.addSymbol(label, 1);

                return childrenValues.get(0).addEdge(parts[1],
                        Integer.parseInt(parts[2]), parts[3], Integer.parseInt(parts[4]), parts[5]);
            } else {
                return null;
            }
        } else if(isRootEdge(label)) {
            if(childrenValues.size() != 1) {
                return null;
            }
            
            DependencyGraph dg = childrenValues.get(0);
            
            if(dg.length() != 1) {
                return null;
            }
            
            String[] parts = getInfo(label);
            if(parts.length != 2) {
                return null;
            }
            
            return dg.addRootEdge(parts[1]);
        }   else {
            if (!childrenValues.isEmpty()) {
                return null;
            }


            String[] parts = label.split("/");
            
            DependencyGraph dg = new DependencyGraph();
            
            if(parts.length == 1) {
                dg.addNode(parts[0].trim());
            } else if (parts.length == 2) {
                dg.addNode(parts[0].trim(), parts[1].trim());
            } else {
                return null;
            }
                
            this.signature.addSymbol(label, 0);
            return dg;
        }
    }

    @Override
    public DependencyGraph parseString(String representation) throws ParserException {
        return this.sedf.read(representation);
    }

    @Override
    public TreeAutomaton decompose(DependencyGraph value) {
        return new DependencyGraphDecomposition(value, this.getSignature());
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
        return FORGET_PREFIX + name + "_" + pos;
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
        return RENAME_PREFIX + name + "_" + number;
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
        return EDGE_PREFIX + fromName + "_" + fromNumber + "_" + toName + "_" + toNumber + "_" + name;
    }
    
    /**
     * 
     * @param label
     * @return 
     */
    public static boolean isRootEdge(String label) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * 
     * @param label
     * @return 
     */
    public static String makeRootEdge(String label){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     */
    private class DependencyGraphDecomposition extends ConcreteTreeAutomaton<SpanWithForget> {

        /**
         *
         * @param dg
         */
        private DependencyGraphDecomposition(DependencyGraph dg, Signature sig) {
            int from = 0;
            int to = dg.length();

            SpanWithForget swf = new SpanWithForget(from, to, to);
            
            
            int fin = this.addState(swf);

            this.getFinalStates().add(fin);

            IntList leastEdge = new IntArrayList();
            IntList maxEdge = new IntArrayList();

            for (int i = 0; i < dg.length(); ++i) {
                leastEdge.add(Integer.MAX_VALUE);
                maxEdge.add(-1);
            }

            for (int i = 0; i < dg.length(); ++i) {
                for (Entry<String> ent : dg.getEdges(i)) {
                    int other = ent.getIntKey();

                    insert(leastEdge, maxEdge, i, other);
                    insert(leastEdge, maxEdge, other, i);
                }
            }
            
            //TODO
            for(int pos=0;pos<dg.length();++pos) {
                String source = namer.apply(dg.getNode(pos));
                SpanWithForget state = new SpanWithForget(from, from, false);
                this.addRule(this.createRule(state, dg.getNode(pos)+"/"+source, new SpanWithForget[0]));
                
                if(dg.getRootEdge(pos) != null) {
                    SpanWithForget par = new SpanWithForget(pos, pos, true);
                    
                    this.addRule(this.createRule(par,makeRootEdge(dg.getRootEdge(pos)),new SpanWithForget[] {state}));
                    state = par;
                }
                
                
            }
            
            
            //TODO
        }

        /**
         *
         * @param leastEdge
         * @param maxEdge
         * @param node
         * @param connection
         */
        private void insert(IntList leastEdge, IntList maxEdge, int node, int connection) {
            int max = maxEdge.getInt(node);
            if (max < connection) {
                maxEdge.set(node, connection);
            }

            int min = leastEdge.getInt(node);
            if (min > connection) {
                leastEdge.set(node, connection);
            }
        }
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
        private final int forgetPoint;
        
        /**
         * 
         */
        private final boolean rootProcessed;

        /**
         * 
         * @param from
         * @param forgetPoint
         * @param rootProcessed 
         */
        public SpanWithForget(int from, int forgetPoint, boolean rootProcessed) {
            this.from = from;
            this.to = from+1;
            this.forgetPoint = forgetPoint;
            this.rootProcessed = rootProcessed;
        }
        
        /**
         * 
         * @param from
         * @param to
         * @param forgetPoint 
         */
        public SpanWithForget(int from, int to, int forgetPoint) {
            this.from  = from;
            this.to = to;
            
            this.forgetPoint = forgetPoint;
            
            this.rootProcessed = true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 13 * hash + this.from;
            hash = 13 * hash + this.to;
            hash = 13 * hash + this.forgetPoint;
            hash = 13 * hash + (this.rootProcessed ? 1 : 0);
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
            final SpanWithForget other = (SpanWithForget) obj;
            if (this.from != other.from) {
                return false;
            }
            if (this.to != other.to) {
                return false;
            }
            if (this.forgetPoint != other.forgetPoint) {
                return false;
            }
            
            return this.rootProcessed == other.rootProcessed;
        }
    }
}
