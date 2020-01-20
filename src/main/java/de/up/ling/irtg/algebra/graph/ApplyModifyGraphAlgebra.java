/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
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
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
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
    public static final String ROOT_SOURCE_NAME = "root";

    @Override
    public TreeAutomaton decompose(Pair<SGraph, ApplyModifyGraphAlgebra.Type> value) {
        //first, if the signature is empty, we make one. This is necessary for e.g. the GUI to work
        /* EDIT: this no longer works, since the signature builder is in the
        am-tools project now. Thus, the next bit is commented out and
        decomposing with the AM algebra
        in the GUI is currently not possible.
          -- JG
        */
        //TODO fix this, there should be a (maybe stupid) default decomposition method that works.
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
    //document what kind of graphs we can evaluate.
    protected Pair<SGraph, ApplyModifyGraphAlgebra.Type> evaluate(String label, List<Pair<SGraph, ApplyModifyGraphAlgebra.Type>> childrenValues) {
        if (childrenValues.contains(null)) {
            return null;
        }
        if (label.startsWith(OP_APPLICATION) && childrenValues.size() == 2) {
            String appSource = label.substring(OP_APPLICATION.length());
            SGraph target = childrenValues.get(1).left;
            Type targetType = childrenValues.get(1).right;
            Type leftType = childrenValues.get(0).right;
            
            //check if we can apply
            if (!leftType.canApplyTo(targetType, appSource)) {
                return null;
            }
            if (!target.getAllSources().contains(ROOT_SOURCE_NAME)) {
                System.err.println("target had no root in APP!");
                return null;//target must have root for APP to be allowed.
            }
            
            //rename right sources to temps
            List<String> orderedSources = new ArrayList<>(target.getAllSources());
            for (int i = 0; i < orderedSources.size(); i++) {
                target = target.renameSource(orderedSources.get(i), "temp"+i);
            }

            //rename temps to left sources
            for (int i = 0; i < orderedSources.size(); i++) {
                String src = orderedSources.get(i);
                if (target.getAllSources().contains("temp" + i)) {
                    if (src.equals(ROOT_SOURCE_NAME)) {
                        target = target.renameSource("temp" + i, appSource);
                    } else {
                        target = target.renameSource("temp" + i, leftType.getRenameTarget(appSource, orderedSources.get(i)));
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
            return new Pair<>(retGraph, leftType.performApply(appSource));

        } else if (label.startsWith(OP_MODIFICATION) && childrenValues.size() == 2) {
            String modSource = label.substring(OP_MODIFICATION.length());
            SGraph target = childrenValues.get(1).left;
            Type targetType = childrenValues.get(1).right;
            Type leftType = childrenValues.get(0).right;

            //check if mod is allowed
            if (!leftType.canBeModifiedBy(targetType, modSource)) {
                //System.err.println("MOD evaluation failed: invalid types! " + leftType + " mod by " + targetType);
                return null;
            }
            
            // remove old root source of modifier and rename modSource to root
            if (target.getNodeForSource(ROOT_SOURCE_NAME) != null) {
                Set<String> retainedSources = new HashSet<>(target.getAllSources());
                retainedSources.remove(ROOT_SOURCE_NAME);
                target = target.forgetSourcesExcept(retainedSources);
            }
            target = target.renameSource(modSource, ROOT_SOURCE_NAME);

            //then just merge
            SGraph leftGraph = childrenValues.get(0).left;
            SGraph retGraph = leftGraph.merge(target);
            if (retGraph == null) {
                System.err.println("MOD merge failed after type checks succeeded! This should not happen, check the code");
            }
            return new Pair<>(retGraph, childrenValues.get(0).right);
        } else {
            try {
                return parseString(label);
            } catch (ParserException ex) {
                throw new RuntimeException("could not parse label '"+label+"' in the AM algebra.");
            }
        }
    }

    @Override
    //TODO document what kinds of strings we can read
    public Pair<SGraph, ApplyModifyGraphAlgebra.Type> parseString(String representation) throws ParserException {
        if (representation.contains(GRAPH_TYPE_SEP)) {
            if (representation.startsWith(OP_COREFMARKER)) {
                representation = representation.substring(OP_COREFMARKER.length());
                representation = representation.substring(representation.indexOf("_") + 1);
            }
            String[] parts = representation.split(GRAPH_TYPE_SEP);
            try {
                return new Pair<>(new IsiAmrInputCodec().read(parts[0]), new Type(parts[1]));
            } catch (ParseException | IllegalArgumentException ex) {
                throw new ParserException(ex);
            }
        } else if (representation.startsWith(OP_COREF)) {
            String corefIndex = representation.substring(OP_COREF.length());
            String graphString = "(u<root,COREF" + corefIndex + ">)";
            return new Pair<>(new IsiAmrInputCodec().read(graphString), Type.EMPTY_TYPE);
        } else {
            //TODO should maybe have a default case where we just put all non-root sources in the type.
            return new Pair<>(new IsiAmrInputCodec().read(representation), Type.EMPTY_TYPE);
        }
    }

    
    /**
     * The type system of the AM algebra as described in Chapter 5 of
     * coli.uni-saarland.de/~jonasg/thesis.pdf
     */
    public static class Type implements Serializable {

        public static final Type EMPTY_TYPE;

        static {
            Type temp;
            try {
                temp = new Type("()");
            } catch (ParseException | IllegalArgumentException ex) {
                temp = null;
                System.err.println("Creating EMPTY_TYPE in ApplyModifyGraphAlgebra.Type failed. this should really never happen");
            }
            EMPTY_TYPE = temp;
        }
        
        private final DirectedAcyclicGraph<String, Edge> graph;
        private final Set<String> origins;
        private final Map<Pair<String, String>, String> parentAndRequestSource2rename;

        /**
         * Creates a type from a string representation. Example format: (S,
         * O(O2_UNIFY_S, O_UNIFY_O2), O2(S_UNIFY_S)).
         * Differences to the notation in the
         * paper include round brackets instead of square brackets, and
         * '_UNIFY_' instead of '{@literal ->}'.
         *
         * @param typeString string representation of type
         * @throws de.up.ling.tree.ParseException if the input string is not in the right recursive form
         *
         */
        public Type(String typeString) throws ParseException {
            this(TreeParser.parse("TOP" + typeString.replaceAll("\\(\\)", "")));
        }

        private Type(Tree<String> typeTree) {
            this.graph = new DirectedAcyclicGraph<>(Edge.class);
            this.origins = new HashSet<>();
            parentAndRequestSource2rename = new HashMap<>();
            boolean success = addTree(typeTree);//addTree calls processUpdates
            if (!success) {
                throw new IllegalArgumentException("Type string led to invalid type: "+typeTree.toString());
            }
        }
        
        /**
         * Creates a new type based on a copy(!) of the provided DAG.
         * @param dag new type is based on this DAG
         */
        private Type(DirectedAcyclicGraph<String, Edge> dag) {
            // copy the dag
            this.graph = new DirectedAcyclicGraph<>(Edge.class);
            for (String v : dag.vertexSet()) {
                graph.addVertex(v);
            }
            for (Edge e : dag.edgeSet()) {
                // don't need to use addDagEdge here, since input is a DAG.
                // Since all properties of e are final, we can use same e here and don't have to create a copy of e.
                graph.addEdge(e.getSource(), e.getTarget(), e);
            }
            origins = new HashSet<>();
            parentAndRequestSource2rename = new HashMap<>();
            boolean success = processUpdates();
            if (!success) {
                throw new IllegalArgumentException("DAG led to invalid type: "+dag.toString());
            }
        }
        
        /**
         * Adds the given tree encoding of a type to this type and updates.
         * @param typeTree tree to be added
         * @return false iff adding the tree leads to an invalid type
         */
        private boolean addTree(Tree<String> typeTree) {
            boolean success = addTreeRecursive(typeTree);
            return success && processUpdates();
        }
        
        /**
         * returns true if success, returns false if we found an inconsistency.
         * Note: Don't use this function, it is only the recursive bit
         * of addTree. In particular, This function does not call updateGraphs. 
         * @param typeTree tree to be added
         * @return false iff adding the tree leads to an invalid type
         */
        private boolean addTreeRecursive(Tree<String> typeTree) {
            String rootLabel = typeTree.getLabel();
            String parent;
            if (rootLabel.contains("_UNIFY")) {
                parent = typeTree.getLabel().split("_UNIFY_")[1];
            } else {
                parent = rootLabel;
            }
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
                graph.addVertex(target);
                if (!parent.equals("TOP")) {
                    Edge existingEdge = graph.getEdge(parent, target);
                    if (existingEdge != null && !existingEdge.getLabel().equals(edgeLabel)) {
                        return false;
                    }
                    try {
                        graph.addDagEdge(parent, target, new Edge(parent, target, edgeLabel));
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
        
        /**
         * Call this after modifying the graph to update internal structures and
         * verify well-formedness. Every public function that changes the type
         * must call this function in the end! Every private function that updates
         * the graph but does
         * not call this function should be marked as such.
         * @return true iff well-formed
         */
        private boolean processUpdates() {
            updateOrigins();
            ensureClosure();
            updateRenameMap();
            return verify();
        }
        
        /**
         * Updates the origin set. To be used when the graph has been updated or created.
         */
        private void updateOrigins() {
            origins.clear();
            for (String node : graph.vertexSet()) {
                if (graph.incomingEdgesOf(node).isEmpty()) {
                    origins.add(node);
//                    System.err.println("added "+node+" to origins");
//                    System.err.println(graph.inDegreeOf(node));
//                    System.err.println(graph.incomingEdgesOf(node));
//                    System.err.println(graph.edgesOf(node));
//                    System.err.println(graph.outgoingEdgesOf(node));
                }
            }
//            System.err.println(this.toString());
//            System.err.println(this.origins.toString());
        }
        
        /**
         * Ensures that all descendents of a node are direct children. If not,
         * this function adds the respective edge (with label equal to the child)
         */
        private void ensureClosure() {
            for (String node : graph.vertexSet()) {
                for (String desc : getDescendants(node)) {
                    if (graph.getEdge(node, desc) == null) {
                        graph.addEdge(node, desc, new Edge(node, desc, desc));//no rename, so edge label = desc
                    }
                }
            }
        }

        /**
         * always run after ensureClosure.
         */
        private void updateRenameMap() {
            parentAndRequestSource2rename.clear();
            for (Edge e : graph.edgeSet()) {
                parentAndRequestSource2rename.put(new Pair<>(e.getSource(), e.getLabel()), e.getTarget());
            }
        }


        /**
         * Checks if all requests are types again.
         * @return false iff two identical nodes would exist in a request
         */
        private boolean verify() {
            for (String node : graph.vertexSet()) {
                Set<String> seenLabels = new HashSet<>();
                for (Edge e : graph.outgoingEdgesOf(node)) {
                    if (seenLabels.contains(e.getLabel())) {
                        return false;
                    } else {
                        seenLabels.add(e.getLabel());
                    }
                }
            }
            return true;
        }
        
        
        @Override
        public String toString() {
            
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (String origin : origins) {
                sj.add(dominatedSubgraphToString(origin));
            }
            
            return sj.toString();
        }

        /**
         * Helper for the toString() function.
         * @param node
         * @return 
         */
        private String dominatedSubgraphToString(String node) {
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            
            for (Edge e : graph.outgoingEdgesOf(node)) {
                String eRep = "";
                if (!e.getLabel().equals(e.getTarget())) {
                    eRep = e.getLabel()+"_UNIFY_";
                }
                sj.add(eRep+dominatedSubgraphToString(e.getTarget()));
            }
            
            return node+sj.toString();
            
        }

        
        
        /**
         * Checks whether this type is a subgraph of type 'other'.
         *
         * @param other
         * @return
         */
        private boolean isCompatibleWith(Type other) {
            return other.graph.vertexSet().containsAll(graph.vertexSet())
                    && other.graph.edgeSet().containsAll(graph.edgeSet());
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
        
        /**
         * helper for the depth function (since depth=height for the full graph)
         * @param node
         * @return 
         */
        private int heightRecursive(String node) {
            int ret = 1;//using 1-based height here, so we can use 0 for the empty type.
            for (Edge e : graph.outgoingEdgesOf(node)) {
                ret = Math.max(ret, heightRecursive(e.getTarget())+1);
            }
            return ret;
        }
            
        //TODO a newer package of jgrapht (org.jgrapht:1.3.1) has this method built in. This implementation here is horribly inefficient (but with such small types in practice it doesn't matter much).
        /**
         * Returns the descendants of v. After ensureClosure has been called, a node has an edge to each descendant,
         * so that getChildren(v) returns the same thing as this (and is faster). However, before the transitive
         * closure has been ensured, the functions may yield different results. In fact, the only usage of this
         * function is (should be) in the creation of the transitive closure.
         * @param v Node to get the descendants of.
         * @return The descendants of v in the type graph.
         */
        private Set<String> getDescendants(String v) {
            Set<String> ret = new HashSet<>();
            for (String child : getChildren(v)) {
                ret.add(child);
                ret.addAll(getDescendants(child));
            }
            return ret;
        }

        /**
         * Returns all children of v. After ensureClosure has been called, a node has an edge to each descendant,
         * so that this returns the same thing as getDescendants(v) (and is faster). However, before the transitive
         * closure has been ensured, the functions may yield different results.
         * @param v Node to get the children of.
         * @return The children of v in the type graph.
         */
        private Set<String> getChildren(String v) {
            Set<String> ret = new HashSet<>();
            for (Edge e : graph.outgoingEdgesOf(v)) {
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
            if (!graph.vertexSet().contains(s)) {
                return null;
            }
            
            Set<String> descendants = getChildren(s);//since every node has a direct edge to all its descendants, we can just do this.
            DirectedAcyclicGraph<String, Edge> ret = new DirectedAcyclicGraph(Edge.class);
            
            for (String node : descendants) {
                ret.addVertex(toRequestNamespace(s, node));
            }
            for (Edge e : graph.edgeSet()) {
                if (descendants.contains(e.getSource()) && descendants.contains(e.getTarget())) {
                    String newSource = toRequestNamespace(s, e.getSource());
                    String newTarget = toRequestNamespace(s, e.getTarget());
                    Edge newEdge = new Edge(newSource, newTarget, e.getLabel());
                    ret.addEdge(newSource, newTarget, newEdge);
                }
            }
            return new Type(ret);
        }
        
        /**
         * Maps the descendant to its counterpart in req(parent). Returns null
         * if "descendant" is not actually a descendant of parent.
         * @param parent
         * @param descendant
         * @return 
         */
        public String toRequestNamespace(String parent, String descendant) {
            Edge e = graph.getEdge(parent, descendant);
            if (e == null) {
                return null;
            } else {
                return e.getLabel();
            }
        }

        /**
         * Maps the source in the request namespace of parent to its counterpart in this graph. Returns null
         * if "inRequestNamespace" is not actually in the request namespace.
         * @param parent
         * @param inRequestNamespace
         * @return
         */
        public String getRenameTarget(String parent, String inRequestNamespace) {
            return parentAndRequestSource2rename.get(new Pair(parent, inRequestNamespace));
        }
        
        /**
         * Given an s-graph with the given sources in it, is this type
         * valid for that graph? Also checks that the graph has a root source,
         * while we're at it.
         * C.f. Definition 5.3 in coli.uni-saarland.de/~jonasg/thesis.pdf
         * (Condition (iii) reformulated here).
         * @param sources
         * @return 
         */
        public boolean isValidTypeForSourceSet(Collection<String> sources) {
            for (String s : sources) {
                if (s.equals(ROOT_SOURCE_NAME) || s.startsWith("COREF")) {
                    //do nothing, we ignore those in types
                } else if (!graph.vertexSet().contains(s)) {
                    // all other sources must be in this type
                    return false;
                }
            }
            // graph must contain root source and all origins.
            return sources.contains(ROOT_SOURCE_NAME) && sources.containsAll(origins);
        }
        
        
        public boolean isEmpty() {
            return this.equals(EMPTY_TYPE);
        }
        

        /**
         * Get all types that can be obtained from this type through a sequence
         * of apply operations. Should maybe be renamed to be more precise?
         * @return 
         */
        public Set<Type> getAllSubtypes() {
            Set<Type> ret = new HashSet<>();
            ret.add(this);
            //TODO the following check is an arbitrary choice to keep complexity explosion in check. This should be properly fixed.
            if (origins.size() < 10) {
                for (String s : origins) {
                    Type after = performApply(s);
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
         * Returns the set of source names such that if we call apply for all those
         * source names on this type, the given subtype remains. Returns null if
         * no such set of source names exists.
         * @param subtype
         * @return 
         */
        public Set<String> getApplySet(Type subtype) {
            if (!subtype.isCompatibleWith(this)) {
                return null;
            }
            // return value is all sources in this type that are not in subtype
            Set<String> ret = Sets.difference(graph.vertexSet(), subtype.graph.vertexSet());
            // but if any source s in ret is a descendant of a node t in subtype,
            // then we can't remove s via apply without removing t before.
            // Can check for that by just looking at the children of the nodes in subtype.
            for (String t : subtype.graph.vertexSet()) {
                if (!Sets.intersection(getChildren(t), ret).isEmpty()) {
                    return null;
                }
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
                    && modifier.origins.contains(modSource) && modifier.copyWithRemoved(modSource).isCompatibleWith(this);
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
                    return head.copyWithRemoved(s);
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

        
        
        /**
         * Returns the type that we obtain after using APP_s on this type (returns
         * a copy and does
         * not modify this type). Returns null if APP_s is not allowed for this
         * type (i.e. if this type does not contain s, or s is needed for later
         * unification).
         *
         * This is a bit redundant with performOperation and copyWithRemoved,
         * but performOperation is clunkier to use (and requires righthand side type)
         * and copyWithRemoved is not public. So I think this has a place -- JG
         * 
         * @param s
         * @return
         */
        public Type performApply(String s) {
            if (!canApplyNow(s)) {
                return null;
            }
            return copyWithRemoved(s);
        }

        
        /**
         * Creates a copy with r removed from the domain. Does not modify the
         * original type.
         *
         * @param r
         * @return
         */
        private Type copyWithRemoved(String r) {
            Type copy = new Type(this.graph);
            copy.graph.removeVertex(r);
            boolean success = copy.processUpdates();
            if (!success) {
                //this should never happen, so we don't set it up to be caught
                throw new RuntimeException("removing a node in type led to invalid type: "+copy.toString());
            }
            return copy;
        }
        
        private boolean isOrigin(String source) {
            return origins.contains(source);
        }
        
        
        /**
         * If an application at this source would be well-typed at the moment.
         * @param source
         * @return 
         */
        public boolean canApplyNow(String source) {
            return isOrigin(source);
        }
        
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + Objects.hashCode(this.graph.vertexSet());
            hash = 79 * hash + Objects.hashCode(this.graph.edgeSet());
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
            final Type other = (Type) obj;
            if (!Objects.equals(this.graph.vertexSet(), other.graph.vertexSet())) {
                return false;
            }
            if (!Objects.equals(this.graph.edgeSet(), other.graph.edgeSet())) {
                return false;
            }
            return true;
        }

        
        
        
        /**
         * Just a simple helper class for edges between nodes that are
         * strings (the sources here) and that also have string labels.
         * The GraphEdge class does not work here, since that requires
         * GraphNode objects 
         */
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

            @Override
            public String toString() {
                return source + "-" + label +  "->" + target;
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
