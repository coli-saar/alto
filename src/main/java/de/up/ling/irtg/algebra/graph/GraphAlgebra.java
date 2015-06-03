/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.up.ling.irtg.algebra.EvaluatingAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TikzSgraphOutputCodec;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;

/**
 * The algebra of s-graphs. The values of this algebra are s-graphs,
 * i.e. objects of class {@link SGraph} (which see). The algebra interprets
 * the following operations (where G, G1, G2 are s-graphs):
 * <ul>
 *  <li>The operation <code>merge(G1,G2)</code>, <i>merges</i> the
 *      two graphs using {@link SGraph#merge(de.up.ling.irtg.algebra.graph.SGraph) }.</li>
 *  <li>The <i>forget</i> operation <code>f_a(G)</code> forgets the
 *      a-source of G. The resulting s-graph is like G, except that
 *      it does not have an a-source. The operation f_a_b forgets both
 *      a and b.</li>
 *  <li>The <i>forget-all</i> operation <code>f(G)</code> forgets
 *      all sources of G.</li>
 *  <li>The <i>forget-except</i> operation <code>fe_a(G)</code> forgets
 *      all sources except a. The operation fe_a_b forgets all sources
 *      except a and b.</li>
 *  <li>The <i>forget-except-root</i> operation <code>fr(G)</code>
 *      forgets all sources except for <code>root</code>. It is equivalent
 *      to <code>fe_root</code>.</li>
 *  <li>The <i>rename</i> operation <code>r_a_b(G)</code> renames
 *      the a-source of G into b, using {@link SGraph#renameSource(java.lang.String, java.lang.String) }.
 *      The operation <code>r_a</code> is an abbreviation for
 *      <code>r_root_a</code>.</li>
 * </ul>
 * 
 * Any other string is interpreted as a constant which denotes
 * an s-graph. The strings are parsed using {@link #parseString(java.lang.String) },
 * i.e. they are expected to be in the extended ISI AMR format syntax.
 * 
 * @author koller
 */
public class GraphAlgebra extends EvaluatingAlgebra<SGraph> {
    // operation symbols of this algebra
    public static final String OP_MERGE = "merge";
    public static final String OP_BOLINASMERGE = "m_";
    public static final String OP_RENAME = "r_";
    public static final String OP_SWAP = "s_";
    public static final String OP_FORGET_ALL = "f";
    public static final String OP_FORGET_ALL_BUT_ROOT = "fr";
    public static final String OP_FORGET_EXCEPT = "fe_";
    public static final String OP_FORGET = "f_";
    
    public static boolean useTopDownAutomaton = false;

    private Int2ObjectMap<SGraph> constantLabelInterpretations;
    /**
     * the returned map maps all constant labels in this algebra's signature to their corresponding SGraphs.
     * This returns the original set stored in the algebra, so it must not be modified.
     * @return
     */
    public Int2ObjectMap<SGraph> getAllConstantLabelInterpretations() {
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
    public Set<String> getAllSourceNames() {
        if (sources == null) {
            sources = getAllSourcesFromSignature(signature);
        }
        return sources;
    }
        
    
    public GraphAlgebra() {
        super();
    }
    
    public GraphAlgebra(Signature signature) {
        super();
        this.signature = signature;
        
        
    }
    
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
    
    @Override
    public TreeAutomaton decompose(SGraph value) {
        if (useTopDownAutomaton)
            return decompose(value, SGraphBRDecompositionAutomatonTopDownAsymptotic.class);
        else
            return decompose(value, SGraphBRDecompositionAutomatonBottomUp.class);
    }
    
    /**
     * given an SGraph, this returns the corresponding decomposition automaton of class c.
     * @param value
     * @param c
     * @return
     */
    public TreeAutomaton decompose(SGraph value, Class c){
        //try {
            if (c == SGraphDecompositionAutomaton.class){
                return new SGraphDecompositionAutomaton(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonBottomUp.class) {
                return new SGraphBRDecompositionAutomatonBottomUp(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonTopDownAsymptotic.class) {
                return new SGraphBRDecompositionAutomatonTopDownAsymptotic(value, this, getSignature());
            }
            else return null;
    }
    
    /**
     * Writes (nearly) all the rules in the decomposition automaton of the SGraph value into in the Writer, and does not store the rules in memory.
     * Iterates through the rules in bottom up order.
     * There are no rename operations on states only reachable via rename.
     * Rules of the form c-> m(a, b) and c-> m(b,a) are both written into the writer (this is different from previous implementations).
     * @param value
     * @param writer
     * @return
     * @throws Exception
     */
    public boolean writeCompleteDecompositionAutomaton(SGraph value, Writer writer) throws Exception{
        SGraphBRDecompositionAutomatonBottomUp botupAutomaton = new SGraphBRDecompositionAutomatonBottomUp(value, this, getSignature());
        return botupAutomaton.writeAutomatonRestricted(writer);
    }
    

    static Iterable<String> getForgottenSources(String opString, SGraph sgraph) {
        if ( opString.startsWith(OP_FORGET) || opString.startsWith(OP_FORGET_EXCEPT)) {
            String[] parts = opString.split("_");
            Set<String> sources = new HashSet<>();

            for (int i = 1; i < parts.length; i++) {
                sources.add(parts[i]);
            }

            if (opString.startsWith(OP_FORGET_EXCEPT)) {
                sources = Sets.difference(sgraph.getAllSources(), sources);
            }

            return sources;
        } else if (opString.equals(OP_FORGET_ALL)) {
            return sgraph.getAllSources();
        } else if( opString.equals(OP_FORGET_ALL_BUT_ROOT)) {
            Set<String> sources = new HashSet<>(sgraph.getAllSources());
            sources.remove("root");
            return sources;
        } else {
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
            } else if (label.startsWith(OP_MERGE)) {
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
            } else if (label.equals(OP_FORGET_ALL)) {
                // forget all sources
                return childrenValues.get(0).forgetSourcesExcept(Collections.EMPTY_SET);
            } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
                // forget all sources, except "root"
                return childrenValues.get(0).forgetSourcesExcept(Collections.singleton("root"));
            } else if (label.startsWith(OP_FORGET_EXCEPT) || label.startsWith(OP_FORGET)) {
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
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));
                return sgraph.withFreshNodenames();
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
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
//        System.err.println("parse string: " + representation);
        
        try {
            return IsiAmrParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
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

    @Override
    public Map<String, String> getRepresentations(SGraph object) {
        Map<String, String> ret = super.getRepresentations(object);

        ret.put("ISI-style AMR", object.toIsiAmrString());

        return ret;
    }
    
    
    private static Set<String> getAllSourcesFromSignature(Signature signature) {
        //find all sources used in algebra:
        Set<String> ret = new HashSet<>();
        for (String symbol : signature.getSymbols())//this adds all sources from the signature, (but be careful, this is kind of a hack) should work now. Maybe better just give this a list of sources directly?
        {
            if (symbol.startsWith(GraphAlgebra.OP_FORGET) || symbol.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {
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
            } else if (symbol.startsWith(GraphAlgebra.OP_BOLINASMERGE)){
                ret.add(GraphInfo.getBOLINASROOTSTRING());
                ret.add(GraphInfo.getBOLINASSUBROOTSTRING());
            } else if (signature.getArityForLabel(symbol) == 0) {
                String[] parts = symbol.split("<");
                for (int i = 1; i<parts.length; i++) {//do not want the first element in parts!
                    List<String> smallerParts = Arrays.asList(parts[i].split(">")[0].split(","));
                    if (smallerParts.contains("")) {
                       System.err.println("empty sourcename!");  
                    }
                    ret.addAll(smallerParts);
                }
            } else if (symbol.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)) {
                ret.add("root");
            }
        }
        return ret;

    }

    
    
    
    
    
    /*public static void main(String[] args) throws Exception {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream( SGraphBRDecompositionAutomatonTopDown.HRG.getBytes( Charset.defaultCharset() ) ));
        Map<String, String> map = new HashMap<>();
        map.put("graph","(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))");
        TreeAutomaton chart = irtg.parse(map);
        System.err.println(chart);
    }*/

    
    
}
