/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphEdgeFactory;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.codec.bolinas_hrg.BolinasHrgLexer;
import de.up.ling.irtg.codec.bolinas_hrg.BolinasHrgParser;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 * An input codec for reading hyperedge replacement grammars (HRGs) in the input
 * format for the <a
 * href="http://www.isi.edu/publications/licensed-sw/bolinas/">Bolinas
 * parser</a>. The codec reads a monolingual graph grammar and converts it into
 * an IRTG with a single interpretation, called "Graph", over the
 * {@link GraphAlgebra}
 * .<p>
 *
 * Because the graph algebra only represents graphs (and not hypergraphs), the
 * conversion will only be successful if every ordinary hyperedge in the rules
 * (i.e., every hyperedge that is not labeled with a nonterminal) has one or two
 * endpoints. These hyperedges are translated as follows:
 * <ul>
 * <li> Hyperedges with two endpoints are translated into ordinary labeled
 * edges.</li>
 * <li> By default, hyperedges with a single endpoint (and label L) are
 * translated into node labels (i.e., the source node of the edge is taken to
 * carry the label L).</li>
 * <li> You can call {@link #setConvertUnaryEdgesToNodeLabels(boolean) } to
 * switch to a behavior where hyperedges with a single endpoint are translated
 * into loops, i.e. into edges from the source node to itself with the given
 * edge label.</li>
 * </ul><p>
 *
 * Whether you want the loop encoding of unary edges or the node label encoding
 * depends on how you represent node labels in the graphs you're trying to
 * parse. The unmodified AMR-Bank uses node labels, which is why the node-label
 * encoding is the default behvior of the codec.<p>
 *
 * Nonterminal hyperedges are treated differently, and may still have an
 * arbitrary number of endpoints.<p>
 *
 * The codec allows you to specify external nodes of the graph on the right-hand
 * side of a rule either with an anonymous marker (".*") or with an explicit
 * marker ("*.2"). The Bolinas documentation does not specify precisely how
 * anonymous and explicit markers can be mixed, so we recommend against mixing
 * both kinds in the same rule. Anonymous markers are translated into external
 * nodes in ascending order, from left to right in the rule. Explicit markers
 * are translated into external nodes in ascending order; note that it is okay
 * to use e.g. *.1 and *.3 but not *.2. The root of the RHS graph is always
 * translated into the first external node.<p>
 *
 * A note of caution: This class is not thread-safe. If you want to use it in a
 * multi-threaded environment, you should make a separate codec object for each
 * thread.
 *
 * @author koller
 */
@CodecMetadata(name = "bolinas_hrg", description = "Hyperedge replacement grammar (Bolinas format)", extension = "hrg", type = InterpretedTreeAutomaton.class)
public class BolinasHrgInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    private boolean convertUnaryEdgesToNodeLabels = true;

    private static final String TEST = "N_1_0_1_2 -> ( 0. :boy :N_0_0_1$  ( 1.*0 :N_0_0$ ) :N_0_0_2$  2.*1 );	0.0022123893805309734\n"
            + "N_0_0_1 -> (. :ARG1 .);0.0001";

//    private static final String TEST = "T -> (. :want' :arg0 (x. :E$) :arg1 (. :T$ x.));\n"
//            + "T -> (. :believe' :arg0 (. :girl') :arg1 (. :T$ .*)); \n"
//            + "T -> (. :want' :arg1 .*);\n"
//            + "E -> (. :boy');";
    private CodecUtilities util = new CodecUtilities();

    private int nextMarker;

//    public static void main(String[] args) throws Exception {
//        InputStream is = new ByteArrayInputStream(TEST.getBytes());
//        InterpretedTreeAutomaton irtg = new BolinasHrgInputCodec().read(is);
//    }
    /**
     * Returns the current behavior with respect to encoding non-nonterminal
     * unary hyperedges.
     *
     * @see #setConvertUnaryEdgesToNodeLabels(boolean)
     * @return
     */
    public boolean isConvertUnaryEdgesToNodeLabels() {
        return convertUnaryEdgesToNodeLabels;
    }

    /**
     * Select how the codec should encode non-nonterminal hyperedges with single
     * endpoints. If the argument is "true" (the default), unary hyperedges are
     * encoded as node labels. If the argument is "false", unary hyperedges are
     * encoded as labeled loops.
     *
     * @param convertUnaryEdgesToNodeLabels
     */
    public void setConvertUnaryEdgesToNodeLabels(boolean convertUnaryEdgesToNodeLabels) {
        this.convertUnaryEdgesToNodeLabels = convertUnaryEdgesToNodeLabels;
    }

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        BolinasHrgLexer l = new BolinasHrgLexer(new ANTLRInputStream(is));
        BolinasHrgParser p = new BolinasHrgParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        BolinasHrgParser.HrgContext result = p.hrg();

//        System.err.println("\nHRG parse tree:\n" + Trees.toStringTree(result, p));

        BolinasHrgGrammar hrg = new BolinasHrgGrammar();
        doHrg(result, hrg);

        return makeIrtg(hrg);
    }

    /**
     * This method turns a given HRG grammar into an IRTG.
     * 
     * @param hrg the grammar that needs to be translated.
     * @return 
     */
    private InterpretedTreeAutomaton makeIrtg(BolinasHrgGrammar hrg) {
        // create the automaton, algebra and homomorphisms that we will
        // build up step by step.
        ConcreteTreeAutomaton<String> ta = new ConcreteTreeAutomaton<>();
        GraphAlgebra ga = new GraphAlgebra();
        Homomorphism hom = new Homomorphism(ta.getSignature(), ga.getSignature());

        // this is where we get our lables from, the prefix used for names
        // does not really matter
        String prefix = "INS";

        // this variable keeps track of the name of the starting non-terminal
        String endpoint = null;

        for (BolinasRule r : hrg.getRules()) {
            // the first time we see a non-terminal it must be the start non-
            // terminal
            if (endpoint == null) {
                endpoint = r.getLhsNonterminal().getNonterminal();
            }

            // this set keeps track of nodes that always have to be sources 
            // because they are endpoints
            SortedSet<String> certainOuter = new TreeSet<>(r.getLhsNonterminal().getEndpoints());

            // sometimes we process rules that are just a single node with possibly
            // a name, in this case we can skip the whole edge processing and
            // use this specific method
            if (r.getRhsGraph().edgeSet().size() < 1 && r.getRhsNonterminals().size() < 1) {
                handleSingleNode(r, prefix, ta, certainOuter, hom, endpoint);
                continue;
            }

            // here we store the edges we use
            List<EdgeTree> edges = new ArrayList<>();
            // we find out how many nodes we have to keep track of at any
            // point by counting how often a node occurs (if it is active in
            // more than one EdgeTree, then we cannot ignore it)
            Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
            
            // in the beginning we can just count over edges
            for (NonterminalWithHyperedge nwh : r.getRhsNonterminals()) {
                for (String s : nwh.getEndpoints()) {
                    counts.addTo(s, 1);
                }
            }
            
            for (GraphEdge ge : r.getRhsGraph().edgeSet()) {
                counts.addTo(ge.getSource().getName(), 1);
                counts.addTo(ge.getTarget().getName(), 1);
            }

            // this set will now contain all the nodes that must be tracked
            SortedSet<String> uncertainOuter = new TreeSet<>();
            for (String s : counts.keySet()) {
                if (1 < counts.get(s)) {
                    uncertainOuter.add(s);
                }
            }
            
            counts.clear();

            uncertainOuter.addAll(certainOuter);

            // now we start creating EdgeTrees to represent all the elements
            // of the RHS, here the tracked nodes a given to the new EdgeTrees
            for (NonterminalWithHyperedge nwh : r.getRhsNonterminals()) {
                edges.add(new EdgeTree(nwh, uncertainOuter));
            }
            
            for (GraphEdge ge : r.getRhsGraph().edgeSet()) {
                edges.add(new EdgeTree(ge, uncertainOuter));
            }

            // now we create EdgeTrees that correspond to merges until we
            // have only one such tree left
            while (edges.size() > 1) {

                int first = -1;
                int second = -1;
                int score = -Integer.MIN_VALUE;

                // first we compute nodes that cannot be eliminated by a merge
                uncertainOuter.clear();
                counts.clear();

                for (EdgeTree et : edges) {
                    et.addCounts(counts);
                }

                for (String s : counts.keySet()) {
                    if (2 < counts.get(s)) {
                        uncertainOuter.add(s);
                    }
                }

                uncertainOuter.addAll(certainOuter);

                // now we attempt to find the two EdgeTrees that, when merged,
                // will eliminate as many nodes as possible
                for (int i = 0; i < edges.size(); ++i) {
                    EdgeTree et1 = edges.get(i);

                    for (int j = i + 1; j < edges.size(); ++j) {

                        EdgeTree et2 = edges.get(j);

                        if (et1.disjoint(et2)) {
                            continue;
                        }

                        int val = et1.joinSize(et2, uncertainOuter);

                        if (val > score) {
                            first = i;
                            second = j;
                            score = val;
                        }
                    }
                }

                // then we remove those EdgeTrees and add a new one that
                // represents their merge
                EdgeTree t = edges.remove(second);
                EdgeTree o = edges.remove(first);

                edges.add(new EdgeTree(o, t, uncertainOuter));
            }

            // here we create the LHS
            NonterminalWithHyperedge nwh = r.getLhsNonterminal();

            // and then add the translation of our complete RHS representation
            // to the grammar and the interpretation
            edges.get(0).transform(ta, hom, prefix, makeLHS(nwh),
                    nwh.getEndpoints(), r.getWeight(), r);
            
            // the LHS can only be a starting symbol if it not only matches 
            // the name of the start symbol, but also has only 1 external node
            // at the root
            if (endpoint.equals(r.getLhsNonterminal().getNonterminal())
                    && r.getLhsNonterminal().getEndpoints().size() == 1) {
                ta.addFinalState(ta.getIdForState(makeLHS(nwh)));
            }
        }

        // now we can turn the automaton and its interpretation into a complete
        // IRTG and return it
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(ta);
        Interpretation in = new Interpretation(ga, hom);
        ita.addInterpretation("Graph", in);

        return ita;
    }

    /**
     * This method handles RHSs that only consist of a single node + name.
     * 
     * @param r
     * @param stso
     * @param ta
     * @param certainOuter
     * @param hom
     * @throws IllegalStateException
     */
    private void handleSingleNode(BolinasRule r, String prefix,
            ConcreteTreeAutomaton<String> ta, SortedSet<String> certainOuter,
            Homomorphism hom, String endpoint) throws IllegalStateException {
        // if there is no node, then something went very wrong
        if (r.getRhsGraph().vertexSet().size() != 1) {
            throw new IllegalStateException("A rule has no right hand side edges and"
                    + "more or less than 1 right hand side vertix, the rule is: " + r);
        }

        // create a LHS
        String nonterminal = makeLHS(r.getLhsNonterminal());
        // create a label for the rule
        String label = Util.gensym(prefix);

        // crate a rule
        Rule rule = ta.createRule(nonterminal, label, new String[]{});
        rule.setWeight(r.getWeight());
        ta.addRule(rule);

        // construct the string corresponding to the single node
        GraphNode gn = r.getRhsGraph().vertexSet().iterator().next();

        // this node must be the external node at position 0, because every
        // bolinas rule has at least one external node
        String s = "(" + gn.getName() + "<0>" + (gn.getLabel() != null
                ? " / " + gn.getLabel() : "");
        s = s + ")";

        // create a tree for the homomorphism
        Tree<String> t = Tree.create(s);
        hom.add(label, t);

        // maybe create a final state
        if (endpoint.equals(r.getLhsNonterminal().getNonterminal())
                && r.getLhsNonterminal().getEndpoints().size() == 1) {
            ta.addFinalState(ta.getIdForState(makeLHS(r.getLhsNonterminal())));
        }
    }

    /**
     * This method allows us create a new LHS symbol in a uniform way.
     * 
     * @param nwh
     * @return
     */
    static String makeLHS(NonterminalWithHyperedge nwh) {
        return nwh.getNonterminal() + "$" + nwh.getEndpoints().size();
    }

    /**
     *
     * @param hrgContext
     * @param grammar
     */
    private void doHrg(BolinasHrgParser.HrgContext hrgContext, BolinasHrgGrammar grammar) {
        boolean isFirstRule = true;

        for (BolinasHrgParser.HrgRuleContext ruleContext : hrgContext.hrgRule()) {
            BolinasRule rule = doHrgRule(ruleContext);
            grammar.addRule(rule);

            if (isFirstRule) {
                isFirstRule = false;
                grammar.setStartSymbol(rule.getLhsNonterminal().getNonterminal());
            }
        }
    }

    /**
     *
     * @param ruleContext
     * @return
     */
    private BolinasRule doHrgRule(BolinasHrgParser.HrgRuleContext ruleContext) {
        BolinasRule ret = new BolinasRule();
        Map<Integer, String> externalNodeNames = new HashMap<>();
        Map<String, GraphNode> nameToNode = new HashMap<>();

        // iterate over term and write HRG rule into ret, storing external nodenames
        nextMarker = 0;
        String nodename = doTerm(ruleContext.term(), ret, externalNodeNames, nameToNode);
        externalNodeNames.put(-100, nodename);  // root node is always the first in the list of external nodes

        // build LHS nonterminal with endpoints
        String lhsNonterminalSymbol = ruleContext.nonterminal().getText();

        List<Integer> markers = new ArrayList<>(externalNodeNames.keySet());  // sort the node markers that were used in the RHS
        Collections.sort(markers);

        List<String> listOfExternalNodes = new ArrayList<>(); // create external nodes, in ascending order of node markers (note they need not be contiguous numbers)
        for (int i = 0; i < markers.size(); i++) {
            listOfExternalNodes.add(externalNodeNames.get(markers.get(i)));
        }

        NonterminalWithHyperedge lhs = new NonterminalWithHyperedge(lhsNonterminalSymbol, listOfExternalNodes);
        ret.setLhsNonterminal(lhs);

        // set weight
        BolinasHrgParser.WeightContext weightContext = ruleContext.weight();
        if (weightContext != null) {
            ret.setWeight(Double.parseDouble(weightContext.getText()));
        } else {
            ret.setWeight(1);
        }

//        System.err.println("bol rule: " + ret);

        return ret;
    }

    /**
     *
     * @param term
     * @param rule
     * @param externalNodeNames
     * @param nameToNode
     * @return
     */
    private String doTerm(BolinasHrgParser.TermContext term, BolinasRule rule,
            Map<Integer, String> externalNodeNames, Map<String, GraphNode> nameToNode) {
        BolinasHrgParser.NodeContext nodeContext = term.node();
        String nodename = doNode(nodeContext, rule, externalNodeNames, nameToNode);

        for (BolinasHrgParser.EdgeWithChildrenContext ewcc : term.edgeWithChildren()) {
            doEdge(ewcc, nodename, rule, externalNodeNames, nameToNode);
        }

        return nodename;
    }

    private String doNode(BolinasHrgParser.NodeContext node, BolinasRule rule, Map<Integer, String> externalNodeNames, Map<String, GraphNode> nameToNode) {
        BolinasHrgParser.IdContext id = node.id();
        BolinasHrgParser.LabelContext label = node.label();
        String nodename = null;

        // known node ID => just return it
        if (id != null) {
            if (nameToNode.containsKey(id.getText())) {
                nodename = id.getText();
            }
        }

        // otherwise, create new node
        if (nodename == null) {
            nodename = (id == null) ? util.gensym("u") : id.getText();
            String nodelabel = (label == null) ? null : label.getText();

            GraphNode gnode = new GraphNode(nodename, nodelabel);
            rule.getRhsGraph().addVertex(gnode);
            nameToNode.put(nodename, gnode);
        }

        // check if external node
        if (node.externalMarker() != null) {
            TerminalNode n = node.externalMarker().INT_NUMBER();

            if (n == null) {
                externalNodeNames.put(nextMarker++, nodename);
            } else {
                externalNodeNames.put(Integer.parseInt(n.getText()), nodename);
            }
        }

        return nodename;
    }

    private void doEdge(BolinasHrgParser.EdgeWithChildrenContext ewcc, String nodename, BolinasRule rule, Map<Integer, String> externalNodeNames, Map<String, GraphNode> nameToNode) {
        List<String> childNodes = new ArrayList<>();

        // collect all endpoints of hyperedge
        childNodes.add(nodename);

        for (BolinasHrgParser.ChildContext cc : ewcc.child()) {
            if (cc.term() != null) {
                childNodes.add(doTerm(cc.term(), rule, externalNodeNames, nameToNode));
            } else {
                childNodes.add(doNode(cc.node(), rule, externalNodeNames, nameToNode));
            }
        }

        String edgelabel = ewcc.edgelabel().EDGELABEL().getText().substring(1);  // strip :

        if (!edgelabel.endsWith("$")) {
            // "real" edge
            switch (childNodes.size()) {
                case 1:
                    if (convertUnaryEdgesToNodeLabels) {
                        GraphNode srcn = nameToNode.get(nodename);
                        srcn.setLabel(edgelabel);
                    } else {
                        addEdge(nodename, nodename, edgelabel, rule, nameToNode);
                    }
                    break;

                case 2:
                    addEdge(childNodes.get(0), childNodes.get(1), edgelabel, rule, nameToNode);
                    break;

                default:
                    throw new CodecParseException("Cannot convert hyperedge with " + childNodes.size() + " endpoints.");
            }

        } else {
            // nonterminal hyperedge
            String ntLabel = edgelabel.substring(0, edgelabel.length() - 1);
            NonterminalWithHyperedge nt = new NonterminalWithHyperedge(ntLabel, childNodes);
            rule.getRhsNonterminals().add(nt);
        }
    }

    private void addEdge(String src, String tgt, String edgelabel, BolinasRule rule, Map<String, GraphNode> nameToNode) {
        GraphNode srcn = nameToNode.get(src);
        GraphNode tgtn = nameToNode.get(tgt);
        GraphEdge e = rule.getRhsGraph().addEdge(srcn, tgtn);
        e.setLabel(edgelabel);
    }
    /**
    * A Bolinas-style hyperedge replacement grammar.
    * 
    * @author koller
    */
   private static class BolinasHrgGrammar {

       private String startSymbol;

       private final List<BolinasRule> rules;

       public BolinasHrgGrammar() {
           rules = new ArrayList<>();
       }

       public List<BolinasRule> getRules() {
           return rules;
       }

       void addRule(BolinasRule rule) {
           rules.add(rule);
       }

       public String getStartSymbol() {
           return startSymbol;
       }

       void setStartSymbol(String startSymbol) {
           this.startSymbol = startSymbol;
       }

       @Override
       public String toString() {

           StringBuilder buf = new StringBuilder();

           for( BolinasRule rule : rules ) {
               buf.append(rule.toString()).append("\n");
           }

           return buf.toString();
       }


   }
    
   
    /**
     * A rule in a Bolinas-style HRG grammar.
     * 
     * @author koller
     */
    private static class BolinasRule {
        private NonterminalWithHyperedge lhsNonterminal;

        private final DirectedGraph<GraphNode, GraphEdge> rhsGraph;

        private final List<NonterminalWithHyperedge> rhsNonterminals;

        private double weight;

        public BolinasRule() {
            rhsGraph = new DefaultDirectedGraph<>(new GraphEdgeFactory());
            rhsNonterminals = new ArrayList<>();
        }

        public NonterminalWithHyperedge getLhsNonterminal() {
            return lhsNonterminal;
        }

        void setLhsNonterminal(NonterminalWithHyperedge lhsNonterminal) {
            this.lhsNonterminal = lhsNonterminal;
        }

        public DirectedGraph<GraphNode, GraphEdge> getRhsGraph() {
            return rhsGraph;
        }

        public List<NonterminalWithHyperedge> getRhsNonterminals() {
            return rhsNonterminals;
        }

        public double getWeight() {
            return weight;
        }

        void setWeight(double weight) {
            this.weight = weight;
        }

        @Override
        public String toString() {
            return lhsNonterminal + " -> " + SGraph.graphToString(rhsGraph) + " " + rhsNonterminals + " {" + weight + "}";
        }
    }
    
    /**
    * This class represents a collection of both normal and non-terminal edges in
    * the right hand side of a HRG rule, organized into a tree in order to convert
    * them more easily into an expression in the s-graph algebra.
    * 
    * It is possible to create trees, ask how many sources could be forgotten if
    * two trees that represent portions of a RHS where merged and to construct the
    * homomorphic image that corresponds to a given tree. For the latter task
    * the class also makes a list of the necessary non-terminals accessible.
    * 
    * @author christoph_teichmann
    */
   private static class EdgeTree {

       /**
        * If this tree represents a single non-terminal edge, then it is stored
        * here.
        */
       private final NonterminalWithHyperedge nont;

       /**
        * If this tree represents the combination of two edge trees, then this will
        * hold the first of these trees.
        */
       private final EdgeTree first;

       /**
        * If this tree represents the combination of two edge trees, then this will
        * hold the second of these trees.
        */
       private final EdgeTree second;

       /**
        * If the tree represents a single edge, then it is contained here.
        */
       private final GraphEdge de;

       /**
        * This contains all the nodes that are still active.
        * 
        * The active nodes are those that still occur at positions in the RHS that
        * have not been subsumed by this tree.
        */
       private final   SortedSet<String> nodes;   

       /**
        * Constructs a new instance representing a single non-terminal edge.
        * 
        * @param nont the edge
        * @param outer all the nodes that might need tracking if they are incident
        * to the edge.
        */
       EdgeTree(NonterminalWithHyperedge nont, Set<String> outer)
       {
           this.nont = nont;
           this.de = null;

           this.nodes = new TreeSet<>(this.nont.getEndpoints());
           nodes.retainAll(outer);

           first = null;
           second = null;
       }

       /**
        * Constructs a new instance representing a single edge.
        * 
        * @param ge the edge
        * @param outer all the nodes that might need tracking if they are incident
        * to the edge.
        */
       EdgeTree(GraphEdge ge, Set<String> outer)
       {
           this.nont = null;
           this.de = ge;

           this.nodes = new TreeSet<>();
           this.nodes.add(this.de.getSource().getName());
           this.nodes.add(this.de.getTarget().getName());
           this.nodes.retainAll(outer);

           first = null;
           second = null;
       }

       /**
        * Creates an instance that represents the merge of two subgraphs.
        * 
        * @param et1 the first subgraphs representation.
        * @param et2 the second subgraphs representation.
        * @param outer all the nodes that might need tracking if they are incident
        * to the edge.
        */
       EdgeTree(EdgeTree et1, EdgeTree et2, Set<String> outer)
       {
           this.nont = null;
           this.de = null;

           first = et1;
           second = et2;

           this.nodes = new TreeSet<>();

           for(String node : et1.nodes)
           {
               if(outer.contains(node) || !et2.nodes.contains(node))
               {
                   nodes.add(node);
               }
           }

           for(String node : et2.nodes)
           {
               if(outer.contains(node) || !et1.nodes.contains(node))
               {
                   nodes.add(node);
               }
           }
       }

       /**
        * Returns the number of nodes that could be eliminated if the two graphs
        * where merged.
        * 
        * @param other
        * @param held nodes that cannot be eliminated, because they are needed
        * elsewhere
        * @return 
        */
       int joinSize(EdgeTree other, Set<String> held)
       {
           Set<String> set = new TreeSet<>();
           set.addAll(this.nodes);
           set.retainAll(other.nodes);

           set.removeAll(held);
           return set.size();
       }

       /**
        * This will add +1 to the given counter for every node that is still active
        * after the construction of this subgraph.
        * 
        * @param counter 
        */
       void addCounts(Object2IntOpenHashMap counter)
       {
           for(String s : this.nodes)
           {
               counter.addTo(s, 1);
           }
       }

       /**
        * Adds a rule corresponding to this subgraph in the given rule to the
        * given automaton and homomorphism.
        * 
        * @param ta automaton to which rules are added.
        * @param hom the homomorphism to which rules are added.
        * @param stso a source for unique strings used to generate labels
        * @param nonterminalName the name of the LHS symbol
        * @param ordering the nodes that are external nodes of the rule, in the
        * order in which they are external.
        * @param weight the weight of the rule
        * @param br the rule to be converted.
        */
       void transform(ConcreteTreeAutomaton<String> ta, Homomorphism hom,
               String prefix, String nonterminalName,
               List<String> ordering, double weight,
               BolinasRule br) 
       {
         // create a container for nodes we have already seen (so we do not add
         //  names twice)
         Set<GraphNode> seenNodes = new HashSet<>();
         // used to keep track of the number of non-terminals we have introduced
         // important to compute the variables for the homomorphism
         AtomicInteger variableNumbers = new AtomicInteger();
         List<String> l = new ArrayList<>();

         // first we generate the homomorphic image of the RHS
         Tree<String> image = transform(variableNumbers,seenNodes,l, br);

         // then we create a rule that has the appropriate child non-terminals
         String label = Util.gensym(prefix);
         Rule r = ta.createRule(nonterminalName, label,
                 l.toArray(new String[l.size()]), weight);
         ta.addRule(r);

         // before we can use the image, we have to add operations that rename the
         // sources to fit with our intended external nodes
         hom.add(label, rename(nodes, ordering, image));
       }

       /**
        * Returns a string representation for a single graph node.
        * 
        * @param seenNodes contains a node if its name is generated elsewhere
        * @param ordering legacy variable that is not used in the current version
        * @param n the node we want to represent
        * @return 
        */
       private String addNode(Set<GraphNode> seenNodes, List<String> ordering, 
               GraphNode n) {
           String s = n.getName();
           if(this.nodes.contains(n.getName()))
           {
               if(ordering != null && ordering.contains(n.getName()))
               {
                   int num = ordering.indexOf(n.getName());
                   s += "<"+num+">";
               }
               else
               {
                   int num = this.nodes.headSet(n.getName()).size();
                   s += "<"+num+">";
               }
           }

           if(!seenNodes.contains(n) && n.getLabel() != null)
           {
               seenNodes.add(n);
               s += " / "+n.getLabel();
           }

           return s;
       }

       /**
        * This method simply converts the arguments into a digestible form for the
        * main rename method.
        * @param from original source assignments
        * @param to goal source assignments
        * @param main tree to extend
        * @return 
        */
       private static Tree<String> rename(SortedSet<String> f, SortedSet<String> t, Tree<String> main) {
           BiMap<String,Integer> from = HashBiMap.create();
           int i = 0;
           for(String s : f)
           {
               from.put(s,i++);
           }

           BiMap<String,Integer> to = HashBiMap.create();
           i = 0;
           for(String s : t)
           {
               to.put(s, i++);
           }

           return rename(from,to,main);
       }

       /**
        * This method simply converts the arguments into a digestible form for the
        * main rename method.
        * @param combined original source assignments
        * @param outer goal source assignments
        * @param main tree to extend
        * @return 
        */
       private static Tree<String> rename(SortedSet<String> combined, List<String> outer, Tree<String> main) {
           BiMap<String,Integer> from = HashBiMap.create();
           int i = 0;
           for(String s : combined)
           {
               from.put(s,i++);
           }

           BiMap<String,Integer> to = HashBiMap.create();
           i = 0;
           for(String s : outer)
           {
               to.put(s, i++);
           }

           return rename(from,to,main);
       }

       /**
        * This method simply converts the arguments into a digestible form for the
        * main rename method.
        * @param combined original source assignments
        * @param outer goal source assignments
        * @param main tree to extend
        */
       private static Tree<String> rename(List<String> combined, SortedSet<String> outer, Tree<String> main) {
           BiMap<String,Integer> from = HashBiMap.create();
           int i = 0;
           for(String s : combined)
           {
               from.put(s,i++);
           }

           BiMap<String,Integer> to = HashBiMap.create();
           i = 0;
           for(String s : outer)
           {
               to.put(s, i++);
           }

           return rename(from,to,main);
       }

       /**
        * This method takes the input tree and assumes that the source assignments
        * are as in from; then builds a more complicated tree that adds operations
        * to get the source assignments as in to.
        * 
        * @param from
        * @param to
        * @param main
        * @return 
        */
       private static Tree<String> rename(BiMap<String,Integer> from, BiMap<String,Integer> to,
                           Tree<String> main)
       {
           Collection<String> c = new ArrayList<>(from.keySet());

           for(String s : c)
           {
               if(!to.containsKey(s))
               {
                   Integer source = from.get(s);
                   main = Tree.create("f_"+source, main);
                   from.remove(s);
                   continue;
               }
               Integer current = from.get(s);
               Integer goal    = to.get(s);

               if(current.equals(goal))
               {
                   continue;
               }

               if(from.inverse().containsKey(goal))
               {
                   String block = from.inverse().get(goal);
                   from.remove(s);
                   from.remove(block);
                   from.put(block, current);
                   from.put(s, goal);

                   main = Tree.create("s_"+current+"_"+goal, main);
               }
               else
               {
                   from.remove(s);
                   from.put(s, goal);
                   main = Tree.create("r_"+current+"_"+goal, main);
               }
           }

           return main;
       }

       /**
        * Converts this tree into a more readable form for debugging.
        * 
        * @return 
        */
       @Override
       public String toString()
       {
          String s = this.nodes.toString();
          s += " ";
          if(this.de != null)
          {
              return s + this.de.getSource()+"-"+this.de.getLabel()+"->"+this.de.getTarget();
          }
          else if(this.nont != null)
          {
              return s + this.nont.toString();
          }
          else
          {
              s += "( "+this.first.toString()+" , "+this.second.toString()+" )";
              return s;
          }
       }

       /**
        * Returns true if the two subgraphs share no source nodes and should
        * therefor not be merged.
        * 
        * @param et2
        * @return 
        */
       boolean disjoint(EdgeTree et2) {
           return Collections.disjoint(nodes, et2.nodes);
       }

       /**
        * Used to recursively transform smaller portions of the subgraph.
        * 
        * @param variableNumbers keeps track of the variables we have already
        * planned for.
        * @param seenNodes keeps track of the nodes for which we have already
        * generated the labels.
        * @param br the rule we are converting, necessary to look up e.g. node
        * labels.
        * @return 
        */
       private Tree<String> transform(AtomicInteger variableNumbers,
               Set<GraphNode> seenNodes, List<String> rhs, BolinasRule br) {
           if(this.de != null)
           {
               return this.handleEdge(seenNodes, br);
           }
           else if(this.nont != null)
           {
               return this.handleNonterminal(seenNodes,rhs, variableNumbers, br);
           }
           else if(this.first != null && this.second != null)
           {
               return this.handleCombination(variableNumbers,seenNodes,rhs, br);
           }

           throw new IllegalStateException("This Edgetree somehow ended up without structure");
       }

       /**
        * Returns a tree that represents a single edge.
        * 
        * @param seenNodes
        * @param br
        * @return 
        */
       private Tree<String> handleEdge(Set<GraphNode> seenNodes, BolinasRule br) {
           StringBuilder sb = new StringBuilder();
           sb.append("(");

           sb.append(this.addNode(seenNodes, null, this.de.getSource()));

           sb.append(" :");
           sb.append(this.de.getLabel());

           sb.append(" (");
           sb.append(this.addNode(seenNodes, null, this.de.getTarget()));
           sb.append(")");

           sb.append(" )");

           Tree<String> t = Tree.create(sb.toString());
           return t;
       }

       /**
        * Returns a tree that represents a single nonterminal edge.
        * 
        * @param seenNodes
        * @param rhs
        * @param variableNumbers
        * @return 
        */
       private Tree<String> handleNonterminal(Set<GraphNode> seenNodes, List<String> rhs,
               AtomicInteger variableNumbers, BolinasRule br) {

           String nt = BolinasHrgInputCodec.makeLHS(this.nont);
           int num = variableNumbers.incrementAndGet();

           rhs.add(nt);

           Tree<String> main = Tree.create("?"+num);

           // everything that follows is for the case where we have labels for
           // nodes that are only realized by nonterminals edges, which means
           // we have to merge them in step by step, since non-terminal edges do
           // not correspond to anything other than a variable in the homomorphism
           int i = 0;
           for(String s : nont.getEndpoints())
           {
               GraphNode g = null;
               for(GraphNode gn : br.getRhsGraph().vertexSet())
               {
                   if(gn.getName().equals(s))
                   {
                       g = gn;
                       break;
                   }
               }

               if(g == null || g.getLabel() == null || seenNodes.contains(g))
               {
                   ++i;
                   continue;
               }

               String k = "("+s;
               k += " <"+i+">";
               k += " / "+g.getLabel();
               seenNodes.add(g);
               k += ")";

               Tree<String> t = Tree.create(k);
               main = Tree.create("merge", t, main);
               ++i;
           }

           // here we convert the node labeling from that of the non-terminal to
           // the order used for the rest of the graph.
           return rename(this.nont.getEndpoints(),this.nodes, main);
       }

       /**
        * This handles all cases where two subgraphs are merged in order to
        * generate a larger portion.
        * 
        * Part of this process includes calling the children in order to have them
        * compute their representation first.
        * 
        * @param variableNumbers
        * @param seenNodes
        * @param rhs
        * @return 
        */
       private Tree<String> handleCombination(AtomicInteger variableNumbers,
               Set<GraphNode> seenNodes, List<String> rhs, BolinasRule br) {

           // get the children
           Tree<String> tl = this.first.transform(variableNumbers, seenNodes, rhs, br);
           Tree<String> tr = this.second.transform(variableNumbers, seenNodes, rhs, br);

           // get them to agree on a source labeling
           SortedSet<String> middle = new TreeSet<>(this.first.nodes);
           middle.addAll(this.second.nodes);

           tl = rename(this.first.nodes, middle, tl);
           tr = rename(this.second.nodes, middle, tr);

           // create the tree for the merge
           Tree<String> ret = Tree.create("merge", tl,tr);

           // have it forget sources that are no longer needed.
           return rename(middle,this.nodes,ret);
       }
   }
    
   
    /**
     * A single nonterminal hyperedge, represented by the
     * nonterminal symbol and its endpoints in the hypergraph.
     * 
     * @author koller
     */
    private static class NonterminalWithHyperedge {

        private final String nonterminal;

        private List<String> endpoints;

        public NonterminalWithHyperedge(String nonterminal, List<String> endpoints) {
            this.nonterminal = nonterminal;
            this.endpoints = endpoints;
        }

        public String getNonterminal() {
            return nonterminal;
        }

        public List<String> getEndpoints() {
            return endpoints;
        }

        @Override
        public String toString() {
            return nonterminal + endpoints;
        }
    }
   
}







