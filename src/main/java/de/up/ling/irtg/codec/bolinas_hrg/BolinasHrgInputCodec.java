/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.bolinas_hrg;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.codec.CodecMetadata;
import de.up.ling.irtg.codec.CodecUtilities;
import de.up.ling.irtg.codec.ExceptionErrorStrategy;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * An input codec for reading hyperedge replacement grammars (HRGs) in the input
 * format for the <a href="http://www.isi.edu/publications/licensed-sw/bolinas/">Bolinas parser</a>. 
 * The codec reads a monolingual graph grammar
 * and converts it into an IRTG with a single interpretation, called "Graph",
 * over the {@link GraphAlgebra}.<p>
 *
 * Because the graph algebra only represents graphs (and not hypergraphs), the
 * conversion will only be successful if every ordinary hyperedge in the rules (i.e.,
 * every hyperedge that is not labeled with a nonterminal) has
 * one or two endpoints. These hyperedges are translated as follows:
 * <ul>
 *   <li> Hyperedges with two endpoints are translated into ordinary labeled edges.</li>
 *   <li> By default, hyperedges with a single endpoint (and label L) are translated 
 * into node labels (i.e., the source node of the edge is taken to carry the
 * label L).</li>
 *   <li> You can call {@link #setConvertUnaryEdgesToNodeLabels(boolean) } to switch
 * to a behavior where hyperedges with a single endpoint are translated into loops,
 * i.e. into edges from the source node to itself with the given edge label.</li>
 * </ul><p>
 * 
 * Whether you want the loop encoding of unary edges or the node label encoding
 * depends on how you represent node labels in the graphs you're trying to parse.
 * The unmodified AMR-Bank uses node labels, which is why the node-label encoding
 * is the default behvior of the codec.<p>
 * 
 * Nonterminal hyperedges are treated differently, and may still have an
 * arbitrary number of endpoints.<p>
 * 
 * The codec allows you to specify external nodes of the graph on the right-hand
 * side of a rule either with an anonymous marker (".*") or with an explicit marker ("*.2").
 * The Bolinas documentation does not specify precisely how anonymous and explicit
 * markers can be mixed, so we recommend against mixing both kinds in the same rule.
 * Anonymous markers are translated into external nodes in ascending order, from
 * left to right in the rule. Explicit markers are translated into external nodes
 * in ascending order; note that it is okay to use e.g. *.1 and *.3 but not *.2.
 * The root of the RHS graph is always translated into the first external node.<p>
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
     * Select how the codec should encode non-nonterminal hyperedges with single endpoints.
     * If the argument is "true" (the default), unary hyperedges are encoded as
     * node labels. If the argument is "false", unary hyperedges are encoded as
     * labeled loops.
     * 
     * @param convertUnaryEdgesToNodeLabels 
     */
    public void setConvertUnaryEdgesToNodeLabels(boolean convertUnaryEdgesToNodeLabels) {
        this.convertUnaryEdgesToNodeLabels = convertUnaryEdgesToNodeLabels;
    }
    
    
//   // why does this method exist? see InputCodec#read
//    public static InterpretedTreeAutomaton create(String input) throws IOException {
//        InputStream is = new ByteArrayInputStream(input.getBytes());
//        return new BolinasHrgInputCodec().read(is);
//    }

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws ParseException, IOException {
        BolinasHrgLexer l = new BolinasHrgLexer(new ANTLRInputStream(is));
        BolinasHrgParser p = new BolinasHrgParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        BolinasHrgParser.HrgContext result = p.hrg();

        BolinasHrgGrammar hrg = new BolinasHrgGrammar();
        doHrg(result, hrg);

//        System.err.println(hrg);
        return makeIrtg(hrg);
    }

    /**
     *
     * @param hrg
     * @return
     */
    private InterpretedTreeAutomaton makeIrtg(BolinasHrgGrammar hrg) {
        ConcreteTreeAutomaton<String> ta = new ConcreteTreeAutomaton<>();
        GraphAlgebra ga = new GraphAlgebra();
        Homomorphism hom = new Homomorphism(ta.getSignature(), ga.getSignature());

        StringSource stso = new StringSource("INS");

        String endpoint = null;

        for (BolinasRule r : hrg.getRules()) {
            if (endpoint == null) {
                endpoint = r.getLhsNonterminal().getNonterminal();
            }
            
            SortedSet<String> certainOuter = new TreeSet<>(r.getLhsNonterminal().getEndpoints());
            
            if(r.getRhsGraph().edgeSet().size() < 1 && r.getRhsNonterminals().size() < 1)
            {
                handleSingleNode(r, stso, ta, certainOuter, hom, endpoint);
                continue;
            }
            
            List<EdgeTree> edges = new ArrayList<>();
            Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
            for (NonterminalWithHyperedge nwh : r.getRhsNonterminals()) {
                for (String s : nwh.getEndpoints()) {
                    counts.addTo(s, 1);
                }
            }
            for (GraphEdge ge : r.getRhsGraph().edgeSet()) {
                counts.addTo(ge.getSource().getName(), 1);
                counts.addTo(ge.getTarget().getName(), 1);
            }

            SortedSet<String> uncertainOuter = new TreeSet<>();
            for (String s : counts.keySet()) {
                if (1 < counts.get(s)) {
                    uncertainOuter.add(s);
                }
            }
            counts.clear();

            uncertainOuter.addAll(certainOuter);

            for (NonterminalWithHyperedge nwh : r.getRhsNonterminals()) {
                edges.add(new EdgeTree(nwh, uncertainOuter));
            }
            for (GraphEdge ge : r.getRhsGraph().edgeSet()) {
                edges.add(new EdgeTree(ge, uncertainOuter));
            }

            while (edges.size() > 1) {
                
                int first = -1;
                int second = -1;
                int score = -Integer.MIN_VALUE;

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
                
                for (int i = 0; i < edges.size(); ++i) {
                    EdgeTree et1 = edges.get(i);
                    
                    for (int j = i + 1; j < edges.size(); ++j) {
                        
                        EdgeTree et2 = edges.get(j);
                        
                        if(et1.disjoint(et2))
                        {
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

                EdgeTree t = edges.remove(second);
                EdgeTree o = edges.remove(first);

                edges.add(new EdgeTree(o, t, uncertainOuter));
            }

            NonterminalWithHyperedge nwh = r.getLhsNonterminal();

            edges.get(0).transform(ta, hom, stso, makeLHS(nwh),
                    new HashSet<>(), nwh.getEndpoints(), r.getWeight(), r);
            if (endpoint.equals(r.getLhsNonterminal().getNonterminal()) &&
                    r.getLhsNonterminal().getEndpoints().size() == 1) {
                ta.addFinalState(ta.getIdForState(makeLHS(nwh)));
            }
        }

        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(ta);
        Interpretation in = new Interpretation(ga, hom);
        ita.addInterpretation("Graph", in);

        return ita;
    }

    /**
     * 
     * @param r
     * @param stso
     * @param ta
     * @param certainOuter
     * @param hom
     * @throws IllegalStateException 
     */
    private void handleSingleNode(BolinasRule r, StringSource stso,
            ConcreteTreeAutomaton<String> ta, SortedSet<String> certainOuter,
            Homomorphism hom, String endpoint) throws IllegalStateException 
    {
        if(r.getRhsGraph().vertexSet().size() != 1)
        {
            throw new IllegalStateException("A rule has no right hand side edges and"
                    + "more or less than 1 right hand side vertix, the rule is: "+r);
        }
        
        String nonterminal = makeLHS(r.getLhsNonterminal());
        String label = stso.get();
        
        Rule rule = ta.createRule(nonterminal, label, new String[] {});
        rule.setWeight(r.getWeight());
        ta.addRule(rule);
        
        GraphNode gn = r.getRhsGraph().vertexSet().iterator().next();
        
        String s = "("+gn.getName()+"<0>"+(gn.getLabel() != null ?
                " / "+gn.getLabel() : "");
        s = s +")";
        
        Tree<String> t = Tree.create(s);
        hom.add(label, t);
        
        if (endpoint.equals(r.getLhsNonterminal().getNonterminal()) &&
                    r.getLhsNonterminal().getEndpoints().size() == 1) {
                ta.addFinalState(ta.getIdForState(makeLHS(r.getLhsNonterminal())));
            }
    }

    /**
     *
     * @param nwh
     * @return
     */
    static String makeLHS(NonterminalWithHyperedge nwh) {
        return nwh.getNonterminal() + "__" + nwh.getEndpoints().size();
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

        // known node ID => just return it
        if (id != null) {
            if (nameToNode.containsKey(id.getText())) {
                return id.getText();
            }
        }

        // otherwise, create new node
        String nodename = (id == null) ? util.gensym("u") : id.getText();
        String nodelabel = (label == null) ? null : label.getText();

        GraphNode gnode = new GraphNode(nodename, nodelabel);
        rule.getRhsGraph().addVertex(gnode);
        nameToNode.put(nodename, gnode);

        // check if external node
        if (node.externalMarker() != null) {
            TerminalNode n = node.externalMarker().INT_NUMBER();
            
            if( n == null ) {
                externalNodeNames.put(nextMarker++, nodename);
            } else {
                externalNodeNames.put(Integer.parseInt(n.getText()), nodename);
            }
            
            
//            int num = (n == null) ? (nextMarker++) : Integer.parseInt(n.getText());
//            externalNodeNames.put(num, nodename);
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
                    throw new ParseException("Cannot convert hyperedge with " + childNodes.size() + " endpoints.");
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
}
