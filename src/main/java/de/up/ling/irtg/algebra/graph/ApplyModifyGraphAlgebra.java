/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;
import org.jgrapht.experimental.isomorphism.IsomorphismRelation;

/**
 *
 * @author Jonas
 */
public class ApplyModifyGraphAlgebra extends Algebra<Pair<SGraph, ApplyModifyGraphAlgebra.Type>> {

    public ApplyModifyGraphAlgebra() {
        this(new Signature());
    }

    public ApplyModifyGraphAlgebra(Signature signature) {
        this.signature = signature;
    }

    public static final String GRAPH_TYPE_SEP = "--TYPE--";

    public static final String OP_APPLICATION = "APP_";
    public static final String OP_MODIFICATION = "MOD_";
    public static final String OP_COREF = "COREF_";
    public static final String OP_COREFMARKER = "MARKER_";

    @Override
    public TreeAutomaton decompose(Pair<SGraph, ApplyModifyGraphAlgebra.Type> value) {
        //first, if the signature is empty, we make one. This is necessary for e.g. the GUI to work
        /* EDIT: this no longer works, since the signature builder is in the
        am-tools project now. Thus, the next bit is commented out and
        decomposing with the AM algebra
        in the GUI is currently not possible.
          -- JG
        */
//        if (signature.getSymbols().isEmpty()) {
//            try {
//                signature = AMSignatureBuilder.makeDecompositionSignature(value.left, 0);
//            } catch (ParseException ex) {
//                Logger.getLogger(ApplyModifyGraphAlgebra.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        if (value.right.isEmpty()) {
            return new AMDecompositionAutomaton(this, null, value.left);
        } else {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public JComponent visualize(Pair<SGraph, Type> object) {
        return new GraphAlgebra().visualize(object.left); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Pair<SGraph, ApplyModifyGraphAlgebra.Type> evaluate(String label, List<Pair<SGraph, ApplyModifyGraphAlgebra.Type>> childrenValues) {
        if (label.startsWith(OP_APPLICATION) && childrenValues.size() == 2) {
            String appSource = label.substring(OP_APPLICATION.length());
            SGraph target = childrenValues.get(1).left;
            Type targetType = childrenValues.get(1).right;
            Type leftType = childrenValues.get(0).right;
            
            //check if we can apply
            if (!leftType.canApplyTo(targetType, appSource)) {
                return null;
            }
            if (!target.getAllSources().contains("root")) {
                System.err.println("target had no root in APP!");
                return null;//target must have root for APP to be allowed.
            }
            
            //rename right sources to temps
            List<String> orderedSources = new ArrayList(target.getAllSources());
            for (int i = 0; i < orderedSources.size(); i++) {
                target = target.renameSource(orderedSources.get(i), "temp"+i);
            }

            //rename temps to left sources
            for (int i = 0; i < orderedSources.size(); i++) {
                String src = orderedSources.get(i);
                if (target.getAllSources().contains("temp" + i)) {
                    if (src.equals("root")) {
                        target = target.renameSource("temp" + i, appSource);
                    } else {
                        target = target.renameSource("temp" + i, leftType.redomain(appSource, orderedSources.get(i)));
                    }
                }
            }

            //merge and then remove appSource from head (prepare the source removal first)
            SGraph leftGraph = childrenValues.get(0).left;
            Set<String> retainedSources = new HashSet<>(leftGraph.getAllSources());
            retainedSources.addAll(target.getAllSources());
            retainedSources.remove(appSource);

            SGraph retGraph = target.merge(childrenValues.get(0).left).forgetSourcesExcept(retainedSources);
            if (retGraph == null) {
                System.err.println("APP merge failed!");
            }
            return new Pair(retGraph, leftType.simulateApply(appSource));

        } else if (label.startsWith(OP_MODIFICATION) && childrenValues.size() == 2) {
            String modSource = label.substring(OP_MODIFICATION.length());
            SGraph target = childrenValues.get(1).left;
            Type targetType = childrenValues.get(1).right;
            Type leftType = childrenValues.get(0).right;

            //check if mod is allowed
            if (!leftType.canBeModifiedBy(targetType, modSource)) {
                System.err.println("MOD evaluation failed: invalid types! " + leftType + " mod by " + targetType);
                return null;
            }
            
            // remove old root source of modifier and rename modSource to root
            if (target.getNodeForSource("root") != null) {
                Set<String> retainedSources = new HashSet<>(target.getAllSources());
                retainedSources.remove("root");
                target = target.forgetSourcesExcept(retainedSources);
            }
            target = target.renameSource(modSource, "root");

            //then just merge
            SGraph leftGraph = childrenValues.get(0).left;
            SGraph retGraph = leftGraph.merge(target);
            if (retGraph == null) {
                System.err.println("MOD merge failed!");
            }
            return new Pair(retGraph, childrenValues.get(0).right);
        } else {
            try {
                return parseString(label);
            } catch (ParserException ex) {
                System.err.println("could not parse label!");
                return null;
            }
        }
    }

    @Override
    public Pair<SGraph, ApplyModifyGraphAlgebra.Type> parseString(String representation) throws ParserException {
        if (representation.contains(GRAPH_TYPE_SEP)) {
            if (representation.startsWith(OP_COREFMARKER)) {
                representation = representation.substring(OP_COREFMARKER.length());
                representation = representation.substring(representation.indexOf("_") + 1);
            }
            String[] parts = representation.split(GRAPH_TYPE_SEP);
            try {
                return new Pair(new IsiAmrInputCodec().read(parts[0]), new Type(parts[1]));
            } catch (ParseException ex) {
                throw new ParserException(ex);
            }
        } else if (representation.startsWith(OP_COREF)) {
            String corefIndex = representation.substring(OP_COREF.length());
            String graphString = "(u<root,COREF" + corefIndex + ">)";
            try {
                return new Pair(new IsiAmrInputCodec().read(graphString), new Type("()"));
            } catch (ParseException ex) {
                throw new ParserException(ex);
            }
        } else {
            try {
                return new Pair(new IsiAmrInputCodec().read(representation), new Type("()"));
            } catch (ParseException ex) {
                throw new ParserException(ex);
            }
        }
    }

    public static class Type extends DirectedAcyclicGraph<String, Type.Edge> implements Serializable {

        public static final Type EMPTY_TYPE;

        static {
            Type temp;
            try {
                temp = new Type("()");
            } catch (ParseException ex) {
                temp = null;
                System.err.println("this should really never happen");
            }
            EMPTY_TYPE = temp;
        }
        
        
        private final Set<String> origins;

        /**
         * Creates a type from a string representation. Example format: (S,
         * O(O2_UNIFY_S, O_UNIFY_O2), O2(S_UNIFY_S)).
         * Differences to the notation in the
         * paper include round brackets instead of square brackets, and
         * '_UNIFY_' instead of '{@literal ->}'.
         *
         * @param typeString
         * @throws de.up.ling.tree.ParseException
         *
         */
        public Type(String typeString) throws ParseException {
            this(TreeParser.parse("TOP" + typeString.replaceAll("\\(\\)", "")));
        }

        private Type(Tree<String> typeTree) {
            super(Edge.class);
            addTreeRecursive(typeTree);
            origins = new HashSet<>();
            updateOrigins();
        }
        
        /**
         * Use this to create a copy.
         * @param dag
         */
        private Type(DirectedAcyclicGraph<String, Edge> dag) {
            super(Edge.class);
            for (String v : dag.vertexSet()) {
                addVertex(v);
            }
            for (Edge e : dag.edgeSet()) {
                try {
                    addDagEdge(e.getSource(), e.getTarget(), new Edge(e.getSource(), e.getTarget(), e.getLabel()));
                } catch (CycleFoundException ex) {
                    System.err.println("WARNING! cycle found in "+dag.toString()+".");
                }
            }
            origins = new HashSet<>();
            updateOrigins();
        }
        
        /**
         * returns true if success, returns false if we found an inconsistency.
         * @param typeTree
         * @return 
         */
        private boolean addTreeRecursive(Tree<String> typeTree) {
            String parent = typeTree.getLabel().split("_UNIFY_")[0];
            for (Tree<String> childTree : typeTree.getChildren()) {
                String[] parts = childTree.getLabel().split("_UNIFY_");
                String target;
                String edgeLabel;
                if (parts.length >1) {
                    target = parts[1];
                    edgeLabel = parts[0];
                } else {
                    target = parts[0];
                    edgeLabel = parts[0];
                }
                this.addVertex(target);
                if (!parent.equals("TOP")) {
                    Edge existingEdge = getEdge(target, parent);
                    if (existingEdge != null && existingEdge.getLabel().equals(edgeLabel)) {
                        return false;
                    }
                    try {
                        addDagEdge(target, target, new Edge(parent, target, edgeLabel));
                    } catch (CycleFoundException ex) {
                        return false;
                    }
                }
                boolean recursiveSuccess = addTreeRecursive(childTree);
                if (!recursiveSuccess) {
                    return false;
                }
            }
            return true;
        }
        
        //TODO might be better to keep track of this dynamically, by overriding addVertex and addEdge methods (former has origins.add(v), latter has origins.remove(e.getTarget()))
        private void updateOrigins() {
            origins.clear();
            for (String node : vertexSet()) {
                if (incomingEdgesOf(node).isEmpty()) {
                    origins.add(node);
                }
            }
        }
        
        
        
        
        @Override
        public String toString() {
            
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (String origin : origins) {
                sj.add(dominatedSubgraphToString(origin));
            }
            
            return sj.toString();
        }

        private String dominatedSubgraphToString(String node) {
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            
            for (Edge e : outgoingEdgesOf(node)) {
                String eRep = "";
                if (!e.getLabel().equals(e.getTarget())) {
                    eRep = e.getLabel()+"_UNIFY_";
                }
                sj.add(eRep+dominatedSubgraphToString(e.getTarget()));
            }
            
            return node+sj.toString();
            
        }

        /**
         * Creates a copy with r removed from the domain. Does not modify the
         * original type.
         *
         * @param r
         * @return
         */
        public Type remove(String r) {
            Type copy = new Type(this);
            copy.removeVertex(r);
            return copy;
        }

        /**
         * Returns the type that we obtain after using APP_s on this type (does
         * not modify this type). Returns null if APP_s is not allowed for this
         * type (i.e. if this type does not contain s, or s is needed for later
         * unification).
         *
         * @param s
         * @return
         */
        public Type simulateApply(String s) {

            if (!origins.contains(s)) {
                return null;
            }

            Type copy = new Type(this);
            copy.remove(s);
            return copy;
        }

        /**
         * Checks whether this type is a subgraph of type 'other'.
         *
         * @param other
         * @return
         */
        public boolean isCompatibleWith(Type other) {
            return other.vertexSet().containsAll(vertexSet())
                    && other.edgeSet().containsAll(edgeSet());
        }

        /**
         * [] has depth 0, [O] has depth 1, [O[S]] has depth 2, etc.
         *
         * @return
         */
        public int depth() {
            if (origins.isEmpty()) {
                return 0;
            } else {
                //depth is same as height
                return origins.stream().map(node -> heightRecursive(node))
                        .collect(Collectors.maxBy(Comparator.naturalOrder())).get();
            }
        }
        
        private int heightRecursive(String node) {
            int ret = 1;//using 1-based height here, so we can use 0 for the empty type.
            for (Edge e : outgoingEdgesOf(node)) {
                ret = Math.max(ret, heightRecursive(e.getTarget())+1);
            }
            return ret;
        }
            
        //TODO a newer package of jgrapht (org.jgrapht:1.3.1) has this method built in. This implementation here is horribly inefficient.
        private Set<String> getDescendants(String v) {
            Set<String> ret = new HashSet<>();
            for (String child : getChildren(v)) {
                ret.add(child);
                ret.addAll(getDescendants(child));
            }
            return ret;
        }
        
        private Set<String> getChildren(String v) {
            Set<String> ret = new HashSet<>();
            for (Edge e : outgoingEdgesOf(v)) {
                ret.add(e.getTarget());
            }
            return ret;
        }

        /**
         * Returns the request of this type at source s (=req(s)). Returns null
         * if s is not in this type.
         * @param s
         * @return 
         */
        public Type getRequest(String s) {
            if (!vertexSet().contains(s)) {
                return null;
            }
            
            Set<String> descendants = getChildren(s);//since every node has a direct edge to all its descendants, we can just do this.
            Type ret = new Type(EMPTY_TYPE);
            
            for (String node : descendants) {
                ret.addVertex(redomain(s, node));
            }
            for (Edge e : edgeSet()) {
                if (descendants.contains(e.getSource()) && descendants.contains(e.getTarget())) {
                    String newSource = redomain(s, e.getSource());
                    String newTarget = redomain(s, e.getTarget());
                    Edge newEdge = new Edge(newSource, newTarget, e.getLabel());
                    ret.addEdge(newSource, newTarget, newEdge);
                }
            }
            return ret;
        }
        
        /**
         * Maps the descendant to its counterpart in req(parent). Returns null
         * if "descendant" is not actually a descendant of parent.
         * @param parent
         * @param descendant
         * @return 
         */
        public String redomain(String parent, String descendant) {
            return getEdge(parent, descendant).getLabel();
        }
        
        
        
        public boolean isEmpty() {
            return equals(EMPTY_TYPE);
        }
        

        public Set<Type> getAllSubtypes() {
            Set<Type> ret = new HashSet<>();
            ret.add(this);
            //TODO the following check is an arbitrary choice to keep complexity explosion in check. This should be properly fixed.
            if (origins.size() < 10) {
                for (String s : origins) {
                    Type after = simulateApply(s);
                    if (after != null) {
                        ret.addAll(after.getAllSubtypes());
                    }
                }
            } else {
                System.err.println("***WARNING*** skipping computation of all subtypes since expected complexity is too high!");
                System.err.println(this.toString());
            }
            return ret;
        }

        /**
         * Checks whether APP_appSource(G_1, G_2) is allowed, given G_1 has this
         * type, and G_2 has type 'argument'.
         *
         * @param argument
         * @param appSource
         * @return
         */
        public boolean canApplyTo(Type argument, String appSource) {
            //check if the type expected here at appSource is equal to the argument type
            Type request = getRequest(appSource);
            if (request == null || !request.equals(argument)) {
                return false;
            }
            // check that appSource is an origin (and thus not needed for unification later)
            return origins.contains(appSource);
        }

        /**
         * Checks whether MOD_modSource(G_1, G_2) is allowed, given G_1 has this
         * type, and G_2 has type argument.
         *
         * @param modifier
         * @param modSource
         * @return
         */
        public boolean canBeModifiedBy(Type modifier, String modSource) {
            Type requestAtMod = modifier.getRequest(modSource);
            return requestAtMod != null && requestAtMod.equals(EMPTY_TYPE)
                    && modifier.origins.contains(modSource) && modifier.remove(modSource).isCompatibleWith(this);
        }

        /**
         * Returns the type of operation(left, right) if that is defined, and
         * null otherwise.
         *
         * @param head
         * @param argOrMod
         * @param operation
         * @return
         */
        public static Type evaluateOperation(Type head, Type argOrMod, String operation) {
            String s;
            boolean app = false;
            
            //System.err.println(operation);

            if (operation.startsWith(OP_APPLICATION)) {
                s = operation.substring(OP_APPLICATION.length());
                app = true;
                //System.err.println("apply!");
            } else {
                s = operation.substring(OP_MODIFICATION.length());
            }
            if (app) {
                if (head.canApplyTo(argOrMod, s)) {
                    return head.simulateApply(s);
                } else {
                    return null;
                }
            } else {
                if (head.canBeModifiedBy(argOrMod, s)) {
                    return head;
                } else {
                    return null;
                }
            }
        }

        private static class Edge {
        
        private final String source;
        private final String target;
        private final String label;
        
        public Edge(String source, String target, String label) {
            this.source = source;
            this.target = target;
            this.label = label;
        }

        public String getTarget() {
            return target;
        }

        public String getSource() {
            return source;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + Objects.hashCode(this.target);
            hash = 43 * hash + Objects.hashCode(this.source);
            hash = 43 * hash + Objects.hashCode(this.label);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Edge other = (Edge) obj;
            if (!Objects.equals(this.target, other.target)) {
                return false;
            }
            if (!Objects.equals(this.source, other.source)) {
                return false;
            }
            if (!Objects.equals(this.label, other.label)) {
                return false;
            }
            return true;
        }
        
        
        
    }
        
    }
    
    
    
    
    

    /**
     * Checks whether g1 and g2 are isomorphic, taking only sources into account
     * that do not start with 'COREF'.
     *
     * @param g1
     * @param g2
     * @return
     */
    public static boolean isomExceptCOREFs(SGraph g1, SGraph g2) {
        GraphIsomorphismInspector iso
                = AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
                        g1.getGraph(),
                        g2.getGraph(),
                        new GraphNode.NodeLabelEquivalenceComparator(),
                        new GraphEdge.EdgeLabelEquivalenceComparator());

        if (!iso.isIsomorphic()) {
            return false;
        } else {
            while (iso.hasNext()) {
                final IsomorphismRelation<GraphNode, GraphEdge> ir = (IsomorphismRelation<GraphNode, GraphEdge>) iso.next();

                boolean foundSourceMismatch = false;

                for (String source : g1.getAllSources()) {
                    if (!source.startsWith("COREF")) {
                        GraphNode newNode = ir.getVertexCorrespondence(g1.getNode(g1.getNodeForSource(source)), true);
                        Collection<String> sourcesHere = g2.getSourcesAtNode(newNode.getName());
                        if (sourcesHere == null || !sourcesHere.contains(source)) {
                            foundSourceMismatch = true;
                        }
                    }
                }

                if (!foundSourceMismatch) {
                    return true;
                }
            }

            return false;
        }
    }

}
