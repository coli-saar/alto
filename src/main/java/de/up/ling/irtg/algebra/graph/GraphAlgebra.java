/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.EvaluatingAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.codec.TikzSgraphOutputCodec;
import de.up.ling.irtg.codec.isiamr.IsiAmrParser;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 * The algebra of s-graphs. The values of this algebra are s-graphs,
 * i.e. objects of class {@link SGraph} (which see). The algebra interprets
 * the following operations (where G, G1, G2 are s-graphs, and a, b are source names):
 * <ul>
 *  <li>The operation <code>merge(G1,G2)</code>, <i>merges</i> the
 *      two graphs using {@link SGraph#merge(de.up.ling.irtg.algebra.graph.SGraph) }.</li>
 *  <li>The <i>forget</i> operation <code>f_a(G)</code> forgets the
 *      a-source of G. The resulting s-graph is like G, except that
 *      it does not have an a-source. The operation f_a_b forgets both
 *      a and b.</li>
 *  <li>The <i>rename</i> operation <code>r_a_b(G)</code> renames
 *      the a-source of G into b, using {@link SGraph#renameSource(java.lang.String, java.lang.String) }.
 *      The operation <code>r_a</code> is an abbreviation for
 *      <code>r_root_a</code>.</li>
 * <li>The <i>swap</i> operation <code>s_a_b(G)</code> renames
 *      the a-source of G into b and the b-source into a.</li>
 *  <li>The <i>combined merge and rename</i> operation <code>merge_a_b(G1,G2)</code>,
 *      first applies the rename from a to b in G2 and then <i>merges</i>
 *      the result with G1.
 * </ul>
 * 
 * Any other string is interpreted as a constant which denotes
 * an s-graph. The strings are parsed using {@link #parseString(java.lang.String) },
 * i.e. they are expected to be in the extended ISI AMR format syntax.
 * 
 * @author koller
 */

/*
 * Old operations:
 *  <li>The <i>forget-all</i> operation <code>f(G)</code> forgets
 *      all sources of G. This is currently not supported in the top-down
 *      automaton.</li>
 *  <li>The <i>forget-except</i> operation <code>fe_a(G)</code> forgets
 *      all sources except a. The operation fe_a_b forgets all sources
 *      except a and b. This is currently not supported in the top-down
 *      automaton.</li>
 *  <li>The <i>forget-except-root</i> operation <code>fr(G)</code>
 *      forgets all sources except for <code>root</code>. It is equivalent
 *      to <code>fe_root</code>. This is currently not supported in the top-down
 *      automaton.</li>
*/

public class GraphAlgebra extends EvaluatingAlgebra<SGraph> {
    // operation symbols of this algebra
    public static final String OP_MERGE = "merge";
    public static final String OP_COMBINEDMERGE = "merge_";
    public static final String OP_RENAME = "r_";
    public static final String OP_SWAP = "s_";
    //public static final String OP_FORGET_ALL = "f";
    //public static final String OP_FORGET_ALL_BUT_ROOT = "fr";
    //public static final String OP_FORGET_EXCEPT = "fe_";
    public static final String OP_FORGET = "f_";
    
    private boolean useTopDownAutomaton = false;

    /**
     * Describes whether this algebra uses top-down or bottom-up decomposition automata.
     * @return 
     */
    public boolean usesTopDownAutomaton() {
        return useTopDownAutomaton;
    }
    
    /**
     * Sets whether this algebra uses top-down or bottom-up decomposition automata.
     * @param useTopDownAutomaton
     */
    public void setUseTopDownAutomaton(boolean useTopDownAutomaton) {
        this.useTopDownAutomaton= useTopDownAutomaton;
    }
    
    @Override
    public List<Pair<Pair<String,String>,  Pair<Function<SGraph, Object>, Class>>> getDecompositionImplementations(String interpretationName) {
        List<Pair<Pair<String,String>,  Pair<Function<SGraph, Object>, Class>>> ret = new ArrayList<>();
        try {
            Function<SGraph, Object> bottomup = graph -> decompose(graph, SGraphBRDecompositionAutomatonBottomUp.class);
            Function<SGraph, Object> topdown = graph -> decompose(graph, SGraphBRDecompositionAutomatonTopDown.class);
            Class returnType = getClass().getMethod("decompose", new Class[]{SGraph.class, Class.class}).getReturnType();
            ret.add(new Pair(new Pair("Bottom-up", "bup"), new Pair(bottomup, returnType)));
            ret.add(new Pair(new Pair("Top-down", "tdown"), new Pair(topdown, returnType)));
        } catch (java.lang.Exception e) {
            System.err.println("Could not collect decomposition implementations for interpretation " + interpretationName +": "+e.toString());
        }
        return ret;
    }
    
    
    

    private Int2ObjectMap<SGraph> constantLabelInterpretations;
    /**
     * the returned map maps all constant labels in this algebra's signature to their corresponding SGraphs.
     * This returns the original set stored in the algebra, so it must not be modified.
     * @return
     */
    Int2ObjectMap<SGraph> getAllConstantLabelInterpretations() {
        if (constantLabelInterpretations == null) {
            precomputeAllConstants();
        }
        return constantLabelInterpretations;
    }
    
    private Set<String> sources;
    
    /**
     * returns the set of all source names occurring in the algebra's signature.
     * This returns the original set stored in the algebra, so it must not be modified.
     * @return
     */
    Set<String> getAllSourceNames() {
        if (sources == null) {
            sources = getAllSourcesFromSignature(signature);
        }
        return sources;
    }
        
    /**
     * Creates an empty graph algebra.
     */
    public GraphAlgebra() {
        super();
    }
    
    /**
     * Creates a graph algebra with the given signature.
     * @param signature 
     */
    public GraphAlgebra(Signature signature) {
        super();
        this.signature = signature;
        
        
    }
    
    /**
     * Computes s-graphs for all constant symbols and stores them for future reference. 
     */
    private void precomputeAllConstants() {
        constantLabelInterpretations = new Int2ObjectOpenHashMap<>();
        for (int id = 1; id <= signature.getMaxSymbolId(); id++) {
            if (signature.getArity(id) == 0) {
                String label = signature.resolveSymbolId(id);
                try {
                    constantLabelInterpretations.put(id, parseString(label));
                } catch (ParserException ex) {
                    throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage() + "when initializing constants for algebra");
                }
            }
        }
    }
    
    /**
     * Returns a bottom-up or a top-down decomposition automaton for the s-graph
     * {@code value} (which one can be set via {@code setUseTopDownAutomaton}, 
     * default is bottom-up).
     * @param value
     * @return 
     */
    @Override
    public TreeAutomaton decompose(SGraph value) {
        if (useTopDownAutomaton)
            return decompose(value, SGraphBRDecompositionAutomatonTopDown.class);
        else
            return decompose(value, SGraphBRDecompositionAutomatonBottomUp.class);
    }
    
    /**
     * Given an SGraph, this returns the corresponding decomposition automaton of class c.
     * @param value
     * @param c
     * @return
     */
    public TreeAutomaton decompose(SGraph value, Class c){
        //try {
            if (c == SGraphDecompositionAutomaton.class){
                return new SGraphDecompositionAutomaton(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonBottomUp.class) {
                return new SGraphBRDecompositionAutomatonBottomUp(value, this);
                
            } else if (c == SGraphBRDecompositionAutomatonTopDown.class) {
                return new SGraphBRDecompositionAutomatonTopDown(value, this);
            }
            else return null;
    }
    
    /**
     * Writes (nearly) all the rules in the decomposition automaton of the
     * SGraph value (with respect to the signature in this algebra) into the
     * Writer, and does not store the rules in memory.
     * Iterates through the rules in bottom up order.
     * To avoid cycles, there are no rename operations on states only reachable
     * via rename. Rules of the form c-> m(a, b) and c-> m(b,a) are both written
     * into the writer.
     * @param value
     * @param writer
     * @return
     * @throws Exception
     */
    public boolean writeRestrictedAutomaton(SGraph value, Writer writer) throws Exception{
        SGraphBRDecompositionAutomatonBottomUp botupAutomaton = new SGraphBRDecompositionAutomatonBottomUp(value, this);
        return botupAutomaton.writeAutomatonRestricted(writer);
    }
    
    /**
     * Writes (nearly) all the rules in the decomposition automaton of the
     * SGraph value (with respect to the incomplete decomposition algebra)
     * into the Writer, and does not store the rules in memory.
     * Iterates through the rules in bottom up order.
     * To avoid cycles, there are no rename operations on states only reachable
     * via rename. Rules of the form c-> m(a, b) and c-> m(b,a) are both written
     * into the writer.
     * @param value
     * @param sourceCount
     * @param writer
     * @return
     * @throws Exception
     */
    public static boolean writeRestrictedDecompositionAutomaton(SGraph value, int sourceCount, Writer writer) throws Exception{
        GraphAlgebra alg = makeIncompleteDecompositionAlgebra(value, sourceCount);
        SGraphBRDecompositionAutomatonBottomUp botupAutomaton = new SGraphBRDecompositionAutomatonBottomUp(value, alg);
        return botupAutomaton.writeAutomatonRestricted(writer);
    }
    
    /**
     * Returns all sources that are forgotten if {@code opString} is applied to {@code sgraph}.
     * Only works if {@code opString} is one of the forget operations (includeing forget all etc).
     * @param opString
     * @param sgraph
     * @return 
     */
    static Iterable<String> getForgottenSources(String opString, SGraph sgraph) {
        if ( opString.startsWith(OP_FORGET)) { //|| opString.startsWith(OP_FORGET_EXCEPT)) {
            String[] parts = opString.split("_");
            Set<String> sources = new HashSet<>();

            for (int i = 1; i < parts.length; i++) {
                sources.add(parts[i]);
            }

            /*if (opString.startsWith(OP_FORGET_EXCEPT)) {
                sources = Sets.difference(sgraph.getAllSources(), sources);
            }*/

            return sources;
        } /*else if (opString.equals(OP_FORGET_ALL)) {
            return sgraph.getAllSources();
        } else if( opString.equals(OP_FORGET_ALL_BUT_ROOT)) {
            Set<String> sources = new HashSet<>(sgraph.getAllSources());
            sources.remove("root");
            return sources;
        }*/ else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public SGraph evaluate(String label, List<SGraph> childrenValues) {
        try {
            if (label == null) {
                return null;
            } else if (label.equals(OP_MERGE)) {
                return childrenValues.get(0).merge(childrenValues.get(1));
            } else if (label.startsWith(OP_COMBINEDMERGE)) {
                String[] parts = label.split("_");
                
                return childrenValues.get(0).merge(childrenValues.get(1).renameSource(parts[1], parts[2]));
                
            } else if (label.startsWith(OP_RENAME)) {
                String[] parts = label.split("_");

                if (parts.length == 2) {
                    parts = new String[]{"r", "root", parts[1]};
                }

                return childrenValues.get(0).renameSource(parts[1], parts[2]);
            } else if (label.startsWith(OP_SWAP)) {
                String[] parts = label.split("_");

                if (parts.length == 2) {
                    parts = new String[]{"r", "root", parts[1]};
                }

                return childrenValues.get(0).swapSources(parts[1], parts[2]);
            /*} else if (label.equals(OP_FORGET_ALL)) {
                // forget all sources
                return childrenValues.get(0).forgetSourcesExcept(Collections.EMPTY_SET);
            } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
                // forget all sources, except "root"
                return childrenValues.get(0).forgetSourcesExcept(Collections.singleton("root"));
            */
            } else if (/*label.startsWith(OP_FORGET_EXCEPT) || */label.startsWith(OP_FORGET)) {
                // forget all sources, except ...
                String[] parts = label.split("_");
                Set<String> retainedSources = new HashSet<>();
                for (int i = 1; i < parts.length; i++) {
                    retainedSources.add(parts[i]);
                }

                if (label.startsWith(OP_FORGET)) {
                    retainedSources = Sets.difference(childrenValues.get(0).getAllSources(), retainedSources);
                }

                return childrenValues.get(0).forgetSourcesExcept(retainedSources);
            } else {
                SGraph sgraph = (new IsiAmrInputCodec()).read(new ByteArrayInputStream(label.getBytes()));
                return sgraph.withFreshNodenames();
            }
        } catch (de.up.ling.irtg.codec.CodecParseException | IOException ex) {
            Logger.getLogger(GraphAlgebra.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    protected boolean isValidValue(SGraph value) {
        return true;
    }

    /**
     * Parses a string into an s-graph, using {@link IsiAmrParser#parse(java.io.Reader) }.
     * 
     * @param representation
     * @return
     * @throws ParserException 
     */
    @Override
    public SGraph parseString(String representation) throws ParserException {
        return new IsiAmrInputCodec().read(representation);
    }

    /**
     * Returns a Swing component that visualizes an object of this algebra.
     * The graph is visualized using the <a href="http://www.jgraph.com/">JGraph</a>
     * graph drawing library.
     * 
     * @see TikzSgraphOutputCodec
     * @param graph
     * @return 
     */
    @Override
    public JComponent visualize(SGraph graph) {
        return SGraphDrawer.makeComponent(graph);
    }

//    @Override
//    public Map<String, String> getRepresentations(SGraph object) {
//        Map<String, String> ret = super.getRepresentations(object);
//
//        ret.put("ISI-style AMR", object.toIsiAmrString());
//
//        return ret;
//    }
    
    /**
     * Returns the set of all source names appearing in {@code signature}.
     * @param signature
     * @return 
     */
    private static Set<String> getAllSourcesFromSignature(Signature signature) {
        //find all sources used in algebra:
        Set<String> ret = new HashSet<>();
        for (String symbol : signature.getSymbols())//this adds all sources from the signature, (but be careful, this is kind of a hack) should work now. Maybe better just give this a list of sources directly?
        {
            if (symbol.startsWith(GraphAlgebra.OP_FORGET)/* || symbol.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)*/) {
                String[] parts = symbol.split("_");
                for (int i = 1; i<parts.length; i++) {
                    if (parts[i].equals("")) {
                        System.err.println("empty sourcename!");
                    } 
                    ret.add(parts[i]);
                }
            } else if (symbol.startsWith(GraphAlgebra.OP_RENAME) || symbol.startsWith(GraphAlgebra.OP_SWAP)) {
                String[] parts = symbol.split("_");
                if (parts.length == 2) {
                    ret.add("root");
                }
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].equals("")) {
                        System.err.println("empty sourcename!");
                    } 
                    ret.add(parts[i]);
                }
            } else if (symbol.startsWith(OP_COMBINEDMERGE)){
                String[] parts = symbol.split("_");
                ret.add(parts[1]);
                ret.add(parts[2]);
            } else if (signature.getArityForLabel(symbol) == 0) {
                String[] parts = symbol.split("<");
                for (int i = 1; i<parts.length; i++) {//do not want the first element in parts!
                    List<String> smallerParts = Arrays.asList(parts[i].split(">")[0].split(","));
                    if (smallerParts.contains("")) {
                       System.err.println("empty sourcename!");  
                    }
                    ret.addAll(smallerParts);
                }
            } /*else if (symbol.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)) {
                ret.add("root");
            }*/
        }
        return ret;
    }

    
    
    /**
     * Creates a GraphAlgebra based on {@code graph} with {@code nrSources} many
     * sources (named 1,..,nrSources).
     * The resulting algebra contains as constants all atomic subgraphs (single
     * edges, and single labled nodes), with all possible source combinations.
     * Further, the merge operation and all possible versions of forget and
     * rename. It is encouraged to use {@code makeIncompleteDecompositionAlgebra}
     * instead, since its result is equally expressive and smaller (due to
     * less spurious constants).
     * @param graph
     * @param nrSources
     * @throws Exception
     * @return
     */
    public static GraphAlgebra makeCompleteDecompositionAlgebra(SGraph graph, int nrSources) throws Exception//only add empty algebra!!
    {
        Signature sig = new Signature();
        Set<String> sources = new HashSet<>();
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }
        Set<String> seenEdgeLabels = new HashSet<>();
        Set<String> seenNodeLabels = new HashSet<>();
        for (String source1 : sources) {
            sig.addSymbol("f_" + source1, 1);
            for (String vName : graph.getAllNodeNames()) {
                String nodeLabel = graph.getNode(vName).getLabel();
                if (!seenNodeLabels.contains(nodeLabel)){
                    seenNodeLabels.add(nodeLabel);
                    sig.addSymbol("(" + vName + "<" + source1 + "> / " + nodeLabel + ")", 0);
                }
            }
            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    sig.addSymbol("r_" + source1 + "_" + source2, 1);
                    sig.addSymbol("s_" + source1 + "_" + source2, 1);
                    for (String vName1 : graph.getAllNodeNames()) {
                        for (String vName2 : graph.getAllNodeNames()) {
                            if (!vName1.equals(vName2)) {
                                GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                                if (e != null) {
                                    String edgeLabel = e.getLabel();
                                    if (!seenEdgeLabels.contains(edgeLabel)){
                                        seenEdgeLabels.add(edgeLabel);
                                        sig.addSymbol("(" + vName1 + "<" + source1 + "> :" + edgeLabel + " (" + vName2 + "<" + source2 + ">))", 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        sig.addSymbol("merge", 2);
        return new GraphAlgebra(sig);
    }

    /**
     * Creates a GraphAlgebra based on {@code graph} with {@code nrSources} many
     * sources (named 1,..,nrSources).
     * The resulting algebra contains as constants all atomic subgraphs (single
     * edges, and single labled nodes), but only one source is used for nodes,
     * and one more source for edges (both possibilities to name the vertices
     * incident to the edge with these two sources are included).
     * Further, the merge operation and all possible versions of forget and 
     * rename.
     * @param graph
     * @param nrSources
     * @throws Exception
     * @return
     */
    public static GraphAlgebra makeIncompleteDecompositionAlgebra(SGraph graph, int nrSources) throws Exception//only add empty algebra!!
    {
        Signature sig = new Signature();
        Set<String> sources = new HashSet<>();
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources) {
            sig.addSymbol("f_" + source1, 1);
            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    sig.addSymbol("r_" + source1 + "_" + source2, 1);
                    //sig.addSymbol("s_" + source1 + "_" + source2, 1);
                }
            }
        }
        Set<String> seenNodeLabels = new HashSet<>();
        for (String vName : graph.getAllNodeNames()) {
            String internalVName = "u";//=vName was bad, since we then have a harder time to recognize the same rules again
            String nodeLabel = graph.getNode(vName).getLabel();
            if (!seenNodeLabels.contains(nodeLabel)){
                seenNodeLabels.add(nodeLabel);
                if (nodeLabel.contains(":")) {
                    nodeLabel = "\""+nodeLabel+"\"";
                }
                sig.addSymbol("(" + internalVName + "<" + sources.iterator().next() + "> / " + nodeLabel + ")", 0);
            }
        }
        Set<String> seenEdgeLabels = new HashSet<>();
        for (String vName1 : graph.getAllNodeNames()) {
            for (String vName2 : graph.getAllNodeNames()) {
                String internalVName1 = "u";
                String internalVName2 = "v";
                if (!vName1.equals(vName2)) {
                    GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                    if (e != null) {
                        String edgeLabel = e.getLabel();
                        if (!seenEdgeLabels.contains(edgeLabel)){
                            seenEdgeLabels.add(edgeLabel);
                            Iterator<String> it = sources.iterator();
                            String s1 = it.next();
                            String s2 = it.next();
                            sig.addSymbol("(" + internalVName1 + "<" + s1 + "> :" + edgeLabel + " (" + internalVName2 + "<" + s2 + ">))", 0);
                            sig.addSymbol("(" + internalVName2 + "<" + s2 + "> :" + edgeLabel + " (" + internalVName1 + "<" + s1 + ">))", 0);
                        }
                    }
                }
            }
        }
        sig.addSymbol("merge", 2);
        return new GraphAlgebra(sig);
    }

    
    /**
     * Writes an IRTG grammar file based on {@code graph} with {@code nrSources}
     * many sources (named 1,..,nrSources). The resulting irtg represents a
     * GraphAlgebra which contains as constants all atomic subgraphs (single
     * edges, and single labled nodes), but only one source is used for nodes,
     * and one more source for edges (both possibilities to name the vertices
     * incident to the edge with these two sources are included).
     * Further, the merge operation and all possible versions of forget and rename.
     * The one final state is S, the one nonfinal state is X.
     * 
     * @param alg empty GraphAlgebra, carries the result.
     * @param graph
     * @param nrSources
     * @throws Exception
     */
    // @todo Keep this only until a more elegant solution based on
    // makeIncompleteDecompositionAlgebra is found.
    public static void writeIncompleteDecompositionIRTG(GraphAlgebra alg, SGraph graph, int nrSources, PrintWriter writer) throws Exception//only add empty algebra!!
    {
        String terminal = "S!";
        String nonterminal = "X";
        String transition = " -> ";
        String strGraph = "[graph] ";

        writer.println(terminal + transition + "m( " + nonterminal + ", " + nonterminal + ")");
        writer.println(strGraph + "merge" + "(?1, ?2)");
        writer.println();

        Signature sig = alg.getSignature();
        Set<String> sources = new HashSet<>();
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources) {

            sig.addSymbol("f_" + source1, 1);
            writer.println(nonterminal + transition + "f" + source1 + "(" + nonterminal + ")");
            writer.println(strGraph + "f_" + source1 + "(?1)");
            writer.println();

            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    String algString = "r_" + source1 + "_" + source2;
                    sig.addSymbol(algString, 1);
                    writer.println(nonterminal + transition + "r" + source1 + source2 + "(" + nonterminal + ")");
                    writer.println(strGraph + algString + "(?1)");
                    writer.println();
                    
                    String algString2 = "s_" + source1 + "_" + source2;
                    sig.addSymbol(algString2, 1);
                    writer.println(nonterminal + transition + "s" + source1 + source2 + "(" + nonterminal + ")");
                    writer.println(strGraph + algString2 + "(?1)");
                    writer.println();
                }
            }
        }

        Set<String> seenNodeLabels = new HashSet<>();
        
        for (String vName : graph.getAllNodeNames()) {
            String nodeLabel = graph.getNode(vName).getLabel();
            if (!seenNodeLabels.contains(nodeLabel)){
                seenNodeLabels.add(nodeLabel);
                String algString = "(" + vName + "<" + sources.iterator().next() + "> / " + nodeLabel + ")";
                sig.addSymbol(algString, 0);
                writer.println(nonterminal + transition + nodeLabel + "VERTEX");
                writer.println(strGraph + "\"" + algString + "\"");
                writer.println();
            }
        }

        Set<String> seenEdgeLabels = new HashSet<>();
        for (String vName1 : graph.getAllNodeNames()) {
            for (String vName2 : graph.getAllNodeNames()) {
                if (!vName1.equals(vName2)) {
                    GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                    if (e != null) {
                        String edgeLabel = e.getLabel();
                        if (!seenEdgeLabels.contains(edgeLabel)){
                            seenEdgeLabels.add(edgeLabel);
                            Iterator<String> it = sources.iterator();
                            String s1 = it.next();
                            String s2 = it.next();

                            String algString = "(" + vName1 + "<" + s1 + "> :" + edgeLabel + " (" + vName2 + "<" + s2 + ">))";
                            sig.addSymbol(algString, 0);
                            writer.println(nonterminal + transition + edgeLabel + "EDGE");
                            writer.println(strGraph + "\"" + algString + "\"");
                            writer.println();

                            algString = "(" + vName1 + "<" + s2 + "> :" + edgeLabel + " (" + vName2 + "<" + s1 + ">))";
                            sig.addSymbol(algString, 0);
                            writer.println(nonterminal + transition + edgeLabel + "EDGE2");
                            writer.println(strGraph + "\"" + algString + "\"");
                            writer.println();
                        }
                    }
                }
            }
        }
        sig.addSymbol("merge", 2);

        writer.println(nonterminal + transition + "m( " + nonterminal + ", " + nonterminal + ")");
        writer.println(strGraph + "merge" + "(?1, ?2)");
        writer.println();
    }
    
    
     private static final String testString1 = "(a / gamma  :alpha (b / beta))";
    private static final String testString2
            = "(n / need-01\n"
            + "      :ARG0 (t / they)\n"
            + "      :ARG1 (e / explain-01)\n"
            + "      :time (a / always))";
    private static final String testString3 = "(p / picture :domain (i / it) :topic (b2 / boa :mod (c2 / constrictor) :ARG0-of (s / swallow-01 :ARG1 (a / animal))))";
    private static final String testString4 = "(bel / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testString5 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.
    private static final String testString5sub1 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG1 (bel2 / believe  :ARG0 b  )))";//kleines beispiel für graph der 3 sources braucht
    private static final String testString6 = "(s / see-01\n"
            + "      :ARG0 (i / i)\n"
            + "      :ARG1 (p / picture\n"
            + "            :mod (m / magnificent)\n"
            + "            :location (b2 / book\n"
            + "                  :name (n / name :op1 \"True\" :op2 \"Stories\" :op3 \"from\" :op4 \"Nature\")\n"
            + "                  :topic (f / forest\n"
            + "                        :mod (p2 / primeval))))\n"
            + "      :mod (o / once)\n"
            + "      :time (a / age-01\n"
            + "            :ARG1 i\n"
            + "            :ARG2 (t / temporal-quantity :quant 6\n"
            + "                  :unit (y / year))))";
    
    private static final String testString7 = "(a6 / and\n" +
"      :op1 (l / look-02\n" +
"            :ARG0 (p / picture\n" +
"                  :name (n / name :op1 \"Drawing\" :op2 \"Number\" :op3 \"Two\")\n" +
"                  :poss i)\n" +
"            :ARG1 (t2 / this))\n" +
"      :op2 (r / respond-01\n" +
"            :ARG0 (g / grown-up)\n" +
"            :ARG1 (i / i)\n" +
"            :ARG2 (a / advise-01\n" +
"                  :ARG0 g\n" +
"                  :ARG1 i\n" +
"                  :ARG2 (a3 / and\n" +
"                        :op1 (l2 / lay-01\n" +
"                              :ARG0 i\n" +
"                              :ARG1 (t3 / thing\n" +
"                                    :ARG1-of (d2 / draw-01\n" +
"                                          :ARG0 i)\n" +
"                                    :topic (b2 / boa\n" +
"                                          :mod (c2 / constrictor)\n" +
"                                          :mod (o / or\n" +
"                                                :op1 (i2 / inside)\n" +
"                                                :op2 (o2 / outside))))\n" +
"                              :ARG2 (a2 / aside))\n" +
"                        :op2 (d3 / devote-01\n" +
"                              :ARG0 i\n" +
"                              :ARG1 i\n" +
"                              :ARG2 (a4 / and\n" +
"                                    :op1 (g2 / geography)\n" +
"                                    :op2 (h / history)\n" +
"                                    :op3 (a5 / arithmetic)\n" +
"                                    :op4 (g3 / grammar))\n" +
"                              :mod (i3 / instead))))\n" +
"            :time (t4 / time\n" +
"                  :mod (t5 / this))))";//n = 31, sources needed = 3

    private static final String testStringChain = "(a / a :Z (b / b :Z (c / c :Z (d / d :Z (e / e)))))";

    private static final String testStringBoy1 = "(w / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))";
    private static final String testStringBoy2 = "(w<root> / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))";//the boy wants to go
    private static final String testStringBoy3 = "(w<root> / want  :ARG0 (b / boy)  :ARG1 (l / like  :ARG0 (g / girl)  :ARG1 b))";//the boy wants the girl to like him.
    private static final String testStringBoy4 = "(bel<root> / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testStringBoy5 = "(bel1<root> / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.

    private static final String testStringSameLabel1 = "(w1<root> / want  :ARG0 (b / boy)  :ARG1 (w2 / want  :ARG0 b  :ARG1 (g / go :ARG0 b)))";
    private static final String TESTSET = "_testset_";
    private static final String[] testset = new String[]{testString1, testString3, testString5sub1, testString5, testString6};
    private static final int[] testSourceNrs = new int[]{2, 2, 3, 4, 3};

//    /**
//     * Main for internal testing purposes.
//     * Computes decomposition algebras and corresponding complete decomposition automata for strings defined in the source code.
//     * Further parameters can be set in the source code.
//     * @param args no arguments
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        
//        boolean testIRTG = false;
//        boolean writeFile = false;
//        
//        
//        if (testIRTG) {
//            InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/testString5sub1_3sources.irtg"));
//            for (int i = 0; i<1000; i++){
//                
//                Map<String, String> map = new HashMap<>();
//                map.put("graph", testString5sub1);
//
//                long startTime = System.currentTimeMillis();
//               
//                //irtg.getInterpretation("graph").
//                TreeAutomaton chart = irtg.parse(map);
//                System.err.println(chart);
//                System.err.println(chart.viterbi());
//               
//                long stopTime = System.currentTimeMillis();
//                long elapsedTime = stopTime - startTime;
//                System.out.println("IRTG parse time is " + elapsedTime + "ms");
//                
////                System.err.println("chart:\n" + chart);
//            }
//            return;
//        }
//        
//        if (writeFile) {
//            //String failedSet = "{1394, 487, 1101, 996, 1428, 1496, 702, 1553, 1254, 1206, 1163, 738, 1438, 1273, 1018, 1464, 752, 1476, 1074, 1184, 1050, 919, 966, 1328, 1416, 832, 1378, 1181, 1469, 1360, 1156, 1083, 847, 661, 1198, 1429, 1554, 1148, 547, 1540, 1423, 1461, 1418, 1207, 1182, 569, 477, 687, 1435, 1460, 691, 883, 1453, 1458, 1382, 1012, 1069, 622, 1539, 1347, 1291, 1551, 620, 1321, 1045, 1185, 900, 1229, 1520, 1175, 1057, 1406, 1137, 1203, 1121, 1068, 1094, 1052, 1403, 1282, 1383, 1271, 1252, 802, 612, 1104, 1245, 667, 1216, 921, 1341, 1512, 867, 1266, 1170, 1119, 611, 1221, 1389, 1401, 920, 1112, 1343, 1089, 1305, 886, 1223, 1233, 1547, 1358, 529, 1534, 1498, 1146, 916, 1502, 1518, 1142, 1208, 1317, 1220, 963, 849, 1171, 962, 1419, 1374, 1244, 662, 563, 817, 1543, 1392, 1196, 1376, 1532, 1436, 891, 1519, 600, 1209, 775, 1448, 1346, 1525, 1414, 1299, 1147, 1351, 740, 1549, 1516, 1463, 458, 1508, 1381, 1310, 1556, 1313, 351, 1410, 1082, 1215, 590, 1421, 1471, 995, 1397, 1332, 1167, 1307, 1224, 1320, 1342, 1467, 1262, 1548, 736, 1440, 791, 1153, 873, 1177, 1366, 861, 677, 1269, 721, 1493, 1214, 1046, 1492, 1186, 1488, 1517, 1380, 1125, 1412, 917, 1363, 1323, 1350, 1174, 1248, 1225, 880, 1510, 1319, 1139, 1480, 1090, 1155, 947, 1396, 1490, 1300, 1415, 1427, 1297, 1031, 1338, 645, 1481, 707, 1546, 655, 1398, 1511, 1115, 1459, 792, 1086, 927, 1240, 161, 911, 1081, 753, 603, 1432, 1456, 1422, 964, 1053, 1530, 1352, 722, 689, 1356, 711, 528, 1442, 1316, 1281, 1559, 1446, 1293, 1377, 1533, 1336, 1222, 990, 968, 1088, 1085, 1251, 651, 833, 932, 1242, 1437, 1509, 1550, 1286, 1478, 1395, 1495, 850, 1337, 1426, 1409, 1191, 1388, 977, 1306, 804, 1103, 1091, 1558, 974, 1193, 837, 1491, 1150, 1117, 1066, 1189, 1055, 512, 1541, 1044, 1303, 656, 953, 1560, 1362, 1249, 1048, 630, 856, 1040, 1387, 1335, 1212, 1309, 1371, 1523, 1375, 1361, 1288, 1267, 1204, 1477, 1368, 1263, 939, 1487, 1236, 1494, 1314, 1187, 1065, 831, 1312, 1454, 660, 1391, 1538, 1284, 820, 751, 1058, 523, 1239, 1124, 1535, 866, 1093, 1035, 1296, 1330, 1544, 1536, 425, 1199, 1470, 1434, 1130, 806, 918, 1552, 1322, 1238, 907, 870, 1301, 1344, 737, 1499, 1183, 1445, 875, 1290, 1154, 859, 1064, 787, 1327, 901, 1450, 467, 386, 1545, 815, 1127, 1515, 1507, 1036, 1384, 1424, 1059, 717, 1504, 902, 1526, 1108, 1529, 1152, 1276, 1482, 1468, 1272, 1399, 1329, 895, 1261, 1555, 1032, 1275, 762, 1514, 1027, 1024, 672, 1302, 1439, 462, 1506, 1537, 1527, 1443, 686, 1056, 445, 1462, 1022, 1136, 1168, 1339, 1017, 1557, 757, 909, 816, 1311, 1486, 1413, 1047, 1062, 391, 1542, 1260, 1522, 1497, 969, 1379, 1500}";
//            //String[] parts = failedSet.split(",");
//            //System.out.println(parts.length);
//            writeFile();
//            return;
//        }
//        
//
//        String input = testString7;
//        int nrSources = 4;
//        int repetitions = 10;
//        boolean onlyCheckAcceptance = false;
//        boolean doBenchmark = true;
//        boolean cleanVersion = true;
//        boolean showSteps = false;
//        boolean makeRulesTopDown = false;
//        Set<Integer> noFullDecomposition = new HashSet<>();
//        //noFullDecomposition.add(3);
//        //noFullDecomposition.add(4);
//
//        long startTime = System.currentTimeMillis();
//        long stopTime;
//        long elapsedTime;
//
//        if (input.equals(TESTSET)) {
//            runTest(noFullDecomposition);
//        } else {
//            //activate this to create algebra from IRTG:
//            /*InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrg.irtg"));
//
//             GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("graph").getAlgebra();
//             SGraph graph = alg.parseString(input);*/
//            //activate this to automatically create algebra that has atomic subgraphs:
//            SGraph graph = new IsiAmrInputCodec().read(input);
//            GraphAlgebra alg = makeIncompleteDecompositionAlgebra(graph, nrSources);
//
//            stopTime = System.currentTimeMillis();
//            elapsedTime = stopTime - startTime;
//            System.out.println("Setup time for  GraphAlgebra is " + elapsedTime + "ms");
//            startTime = System.currentTimeMillis();
//
//            runIteration(graph, alg, onlyCheckAcceptance, cleanVersion, showSteps, makeRulesTopDown);
//
//            if (doBenchmark) {
//                stopTime = System.currentTimeMillis();
//                long elapsedTime0 = stopTime - startTime;
//                startTime = System.currentTimeMillis();
//
//                for (int i = 0; i < repetitions; i++) {
//                    runIteration(graph, alg, onlyCheckAcceptance, cleanVersion, showSteps, makeRulesTopDown);
//                }
//
//                stopTime = System.currentTimeMillis();
//                long elapsedTime1 = stopTime - startTime;
//
//                startTime = System.currentTimeMillis();
//
//                for (int i = 0; i < repetitions; i++) {
//                    runIteration(graph, alg, onlyCheckAcceptance, cleanVersion, showSteps, makeRulesTopDown);
//                }
//
//                stopTime = System.currentTimeMillis();
//                long elapsedTime2 = stopTime - startTime;
//                System.out.println("Decomposition time for first run is " + elapsedTime0 + "ms");
//                System.out.println("Decomposition time for next " + repetitions + " is " + elapsedTime1 + "ms");
//                System.out.println("Decomposition time for further next " + repetitions + " is " + elapsedTime2 + "ms");
//
//            }
//        }
//
//        //auto.printAllRulesTopDown();
//        //auto.printShortestDecompositionsTopDown();
//        //String res = auto.toStringBottomUp();
//        //System.out.println(res);
//    }
//
//    private static void runIteration(SGraph graph, GraphAlgebra alg, boolean onlyCheckAcceptance, boolean cleanVersion, boolean showSteps, boolean makeRulesTopDown) {
//        SGraphBRDecompositionAutomatonBottomUp auto = (SGraphBRDecompositionAutomatonBottomUp) alg.decompose(graph, SGraphBRDecompositionAutomatonBottomUp.class);
//        
//        if (onlyCheckAcceptance) {
//            throw new UnsupportedOperationException("Currently not supported!");
//            /*if (instr.doesAccept(alg)) {
//                System.out.println("Accepted!");
//            } else {
//                System.out.println("Not accepted!");
//            }*/
//        } else {
//            if (cleanVersion) {
//                auto.processAllRulesBottomUp(rule-> {});
//            } else {
//                throw new UnsupportedOperationException("Currently not supported!");
//                //instr.iterateThroughRulesBottomUp1(alg, showSteps, makeRulesTopDown);
//            }
//        }
//        
//    }
//
//    private static void runTest(Set<Integer> noFullDecomposition) throws Exception {
//        int nrRepetitions = 10;
//        int warmupRepetitions= 5;
//        int doesAcceptSourcesCount = 4;
//
//        long startTime;
//        long stopTime;
//        long elapsedTime;
//        System.out.println("Starting test with " + nrRepetitions + " repetitions.");
//
//        GraphAlgebra[] alg = new GraphAlgebra[testset.length];
//        GraphAlgebra[] doesAcceptAlg = new GraphAlgebra[testset.length];
//        SGraph[] graph = new SGraph[testset.length];
//        for (int i = 0; i < testset.length; i++) {
//            graph[i] = new IsiAmrInputCodec().read(testset[i]);
//            alg[i] = makeIncompleteDecompositionAlgebra(graph[i], testSourceNrs[i]);
//            doesAcceptAlg[i] = makeIncompleteDecompositionAlgebra(graph[i], doesAcceptSourcesCount);
//        }
//
//        //warmup
//        for (int i = 0; i < testset.length; i++) {
//            for (int j = 0; j < warmupRepetitions; j++) {
//                runIteration(graph[i], doesAcceptAlg[i], true, true, false, false);
//            }
//            if (!noFullDecomposition.contains(i)) {
//                for (int j = 0; j < warmupRepetitions; j++) {
//                    runIteration(graph[i], alg[i], false, true, false, false);
//                }
//            }
//        }
//
//        //actual test
//        for (int i = 0; i < testset.length; i++) {
//
//            startTime = System.currentTimeMillis();
//            for (int j = 0; j < nrRepetitions; j++) {
//                runIteration(graph[i], doesAcceptAlg[i], true, true, false, false);
//            }
//            stopTime = System.currentTimeMillis();
//            elapsedTime = stopTime - startTime;
//            System.out.println("doesAccept for i=" + i + "; Time =" + elapsedTime);
//
//            if (!noFullDecomposition.contains(i)) {
//                startTime = System.currentTimeMillis();
//                for (int j = 0; j < nrRepetitions; j++) {
//                    runIteration(graph[i], alg[i], false, true, false, false);
//                }
//                stopTime = System.currentTimeMillis();
//                elapsedTime = stopTime - startTime;
//                System.out.println("iterateThroughRules1 for i=" + i + "; Time =" + elapsedTime);
//            }
//        }
//    }
//
//    private static void writeFile() throws Exception {
//        String filename = "examples/testString5sub1_3SourcesNew.irtg";
//        PrintWriter writer = new PrintWriter(filename);
//        writer.println("interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra");
//        writer.println();
//        
//        GraphAlgebra alg = new GraphAlgebra();
//        SGraph graph = alg.parseString(testString5sub1);
//        writeIncompleteDecompositionIRTG(alg, graph, 4, writer);
//
//       /*for (int i = 0; i < testset.length; i++) {;
//        }*/
//
//        writer.close();
//    }
    
    
}
