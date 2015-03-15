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
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecMetadata;
import de.up.ling.irtg.codec.CodecUtilities;
import de.up.ling.irtg.codec.ExceptionErrorStrategy;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.hom.Homomorphism;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
 * An input codec for reading hyperedge replacement grammars (HRGs)
 * in the input format for the Bolinas parser. The codec reads a
 * monolingual graph grammar and converts it into an IRTG with a
 * single interpretation over the {@link GraphAlgebra}.<p>
 * 
 * Because the
 * graph algebra only represents graphs (and not hypergraphs), the
 * conversion will only be successful if every ordinary edge in the
 * rules has one or two endpoints. Edges with one endpoint are
 * translated into loops. Nonterminal hyperedges are treated
 * differently, and may still have an arbitrary number of endpoints.<p>
 * 
 * A note of caution: This class is not thread-safe. If you want to
 * use it in a multi-threaded environment, you should make a separate 
 * codec object for each thread.
 *
 * @author koller
 */
@CodecMetadata(name = "bolinas_hrg", description = "Hyperedge replacement grammar (Bolinas format)", extension = "hrg", type = InterpretedTreeAutomaton.class)
public class BolinasHrgInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    private static final String TEST = "N_1_0_1_2 -> ( 0. :N_0_0_1$  ( 1.*0 :N_0_0$ ) :N_0_0_2$  2.*1 );	0.0022123893805309734";
    
//    private static final String TEST = "T -> (. :want' :arg0 (x. :E$) :arg1 (. :T$ x.));\n"
//            + "T -> (. :believe' :arg0 (. :girl') :arg1 (. :T$ .*)); \n"
//            + "T -> (. :want' :arg1 .*);\n"
//            + "E -> (. :boy');";

    private CodecUtilities util = new CodecUtilities();
    private int nextMarker;

    public static void main(String[] args) throws Exception {
        InputStream is = new ByteArrayInputStream(TEST.getBytes());
        InterpretedTreeAutomaton irtg = new BolinasHrgInputCodec().read(is);
    }

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws ParseException, IOException {
        BolinasHrgLexer l = new BolinasHrgLexer(new ANTLRInputStream(is));
        BolinasHrgParser p = new BolinasHrgParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        BolinasHrgParser.HrgContext result = p.hrg();

        BolinasHrgGrammar hrg = new BolinasHrgGrammar();
        doHrg(result, hrg);

        System.err.println(hrg);

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
        
        String endpoint  = null;
        
        for(Rule r : hrg.getRules())
        {
            if(endpoint == null)
            {
                endpoint = r.getLhsNonterminal().getNonterminal();
            }
            
            SortedSet<String> certainOuter = new TreeSet<>(r.getLhsNonterminal().getEndpoints());
            
            List<EdgeTree> edges = new ArrayList<>();
            Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
            for(NonterminalWithHyperedge nwh : r.getRhsNonterminals())
            {
                for(String s : nwh.getEndpoints())
                {
                    counts.addTo(s, 1);
                }
            }
            for(GraphEdge ge : r.getRhsGraph().edgeSet())
            {
                counts.addTo(ge.getSource().getName(), 1);
                counts.addTo(ge.getTarget().getName(), 1);
            }
            
            SortedSet<String> uncertainOuter = new TreeSet<>();
            for(String s : counts.keySet())
            {
                if(1 < counts.get(s))
                {
                    uncertainOuter.add(s);
                }
            }
            counts.clear();
            
            uncertainOuter.addAll(certainOuter);
            
            for(NonterminalWithHyperedge nwh : r.getRhsNonterminals())
            {
                edges.add(new EdgeTree(nwh, uncertainOuter));
            }
            for(GraphEdge ge : r.getRhsGraph().edgeSet())
            {
                edges.add(new EdgeTree(ge, uncertainOuter));
            }
            
            while(edges.size() > 1)
            {
                int first = -1;
                int second = -1;
                int score = -10000;
                
                uncertainOuter.clear();
                counts.clear();
                
                for(EdgeTree et : edges)
                {
                    et.addCounts(counts);
                }
                
                for(String s : counts.keySet())
                {
                    if(2 < counts.get(s))
                    {
                        uncertainOuter.add(s);
                    }
                }
                uncertainOuter.addAll(certainOuter);
                
                for(int i=0;i<edges.size();++i)
                {
                    for(int j=i+1;j<edges.size();++j)
                    {
                        int val = edges.get(i).joinSize(edges.get(j), uncertainOuter);
                        if(val > score)
                        {
                            first = i;
                            second = j;
                        }
                    }
                }
                
                EdgeTree t = edges.remove(second);
                EdgeTree o = edges.remove(first);
                
                edges.add(new EdgeTree(o, t, uncertainOuter));
            }
            
            NonterminalWithHyperedge nwh = r.getLhsNonterminal();
            
            edges.get(0).transform(ta,hom,stso, makeLHS(nwh),
                    new HashSet<>(), nwh.getEndpoints());
            if(endpoint.equals(r.getLhsNonterminal().getNonterminal()))
            {
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
     * @param nwh
     * @return 
     */
    static String makeLHS(NonterminalWithHyperedge nwh) {
        return nwh.getNonterminal()+"_"+nwh.getEndpoints().size();
    }

    /**
     * 
     * @param hrgContext
     * @param grammar 
     */
    private void doHrg(BolinasHrgParser.HrgContext hrgContext, BolinasHrgGrammar grammar) {
        boolean isFirstRule = true;

        for (BolinasHrgParser.HrgRuleContext ruleContext : hrgContext.hrgRule()) {
            Rule rule = doHrgRule(ruleContext);
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
    private Rule doHrgRule(BolinasHrgParser.HrgRuleContext ruleContext) {
        Rule ret = new Rule();
        Map<Integer, String> externalNodeNames = new HashMap<>();
        Map<String, GraphNode> nameToNode = new HashMap<>();

        // iterate over term and write HRG rule into ret, storing external nodenames
        nextMarker = 0;
        String nodename = doTerm(ruleContext.term(), ret, externalNodeNames, nameToNode);
        externalNodeNames.put(-1, nodename);

        // build LHS nonterminal with endpoints
        String lhsNonterminalSymbol = ruleContext.nonterminal().getText();
        List<String> listOfExternalNodes = new ArrayList<>();
        for (int i = 0; i < externalNodeNames.size(); i++) {
            listOfExternalNodes.add(externalNodeNames.get(i-1));
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
    private String doTerm(BolinasHrgParser.TermContext term, Rule rule, Map<Integer, String> externalNodeNames, Map<String, GraphNode> nameToNode) {
        BolinasHrgParser.NodeContext nodeContext = term.node();
        String nodename = doNode(nodeContext, rule, externalNodeNames, nameToNode);

        for (BolinasHrgParser.EdgeWithChildrenContext ewcc : term.edgeWithChildren()) {
            doEdge(ewcc, nodename, rule, externalNodeNames, nameToNode);
        }

        return nodename;
    }

    private String doNode(BolinasHrgParser.NodeContext node, Rule rule, Map<Integer, String> externalNodeNames, Map<String, GraphNode> nameToNode) {
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
        String nodelabel = (label == null) ? nodename : label.getText();

        GraphNode gnode = new GraphNode(nodename, nodelabel);
        rule.getRhsGraph().addVertex(gnode);
        nameToNode.put(nodename, gnode);

        // check if external node
        if (node.externalMarker() != null) {
            TerminalNode n = node.externalMarker().INT_NUMBER();
            int num = (n == null) ? (nextMarker++) : Integer.parseInt(n.getText());
            externalNodeNames.put(num, nodename);
        }

        return nodename;
    }

    private void doEdge(BolinasHrgParser.EdgeWithChildrenContext ewcc, String nodename, Rule rule, Map<Integer, String> externalNodeNames, Map<String, GraphNode> nameToNode) {
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

        if (ewcc.edgelabel() != null) {
            // "real" edge
            String src, tgt;

            switch (childNodes.size()) {
                case 1:
                    src = nodename;
                    tgt = nodename;
                    break;

                case 2:
                    src = childNodes.get(0);
                    tgt = childNodes.get(1);
                    break;

                default:
                    throw new ParseException("Cannot convert hyperedge with " + childNodes.size() + " endpoints.");
            }

            GraphNode srcn = nameToNode.get(src);
            GraphNode tgtn = nameToNode.get(tgt);
            GraphEdge e = rule.getRhsGraph().addEdge(srcn, tgtn);
            e.setLabel(ewcc.edgelabel().getText());
        } else {
            // nonterminal hyperedge
            NonterminalWithHyperedge nt = new NonterminalWithHyperedge(ewcc.nonterminalEdgelabel().NAME().getText(), childNodes);
            rule.getRhsNonterminals().add(nt);
        }
    }
}
