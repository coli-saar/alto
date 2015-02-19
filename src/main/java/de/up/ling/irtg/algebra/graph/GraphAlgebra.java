/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonStoreTopDownExplicit;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonMPFTrusting;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonTopDownAysmptotic;
import com.google.common.collect.Sets;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.BinaryPartnerFinder;
import de.up.ling.irtg.algebra.EvaluatingAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import static de.up.ling.irtg.algebra.graph.GraphInfo.BOLINASROOTSTRING;
import static de.up.ling.irtg.algebra.graph.GraphInfo.BOLINASSUBROOTSTRING;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonOnlyWrite;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonTopDown;
import de.up.ling.irtg.algebra.graph.mpf.DynamicMergePartnerFinder;
import de.up.ling.irtg.algebra.graph.mpf.MergePartnerFinder;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TikzSgraphOutputCodec;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
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

    public final Int2ObjectMap<SGraph> constantLabelInterpretations;
    Set<String> sources;
        
        
    private boolean isPure = false;
    private int mergeLabelID;
    
    public GraphAlgebra() {
        super();
        
        constantLabelInterpretations = new Int2ObjectOpenHashMap<>();
    }
    
    public GraphAlgebra(Signature signature) {
        super();
        isPure = true;
        this.signature = signature;
        for (String label : signature.getSymbols()) {
            if (signature.getArityForLabel(label) == 2) {
                if (label.equals(OP_MERGE)) {
                    mergeLabelID = signature.getIdForSymbol(label);
                } else {
                    isPure = false;
                }
            }
        }
        
        
        constantLabelInterpretations = new Int2ObjectOpenHashMap<>();
        precomputeAllConstants();
        sources = getAllSourcesFromSignature(signature);
    }
    
    private void precomputeAllConstants() {
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
        //return new SGraphDecompositionAutomaton(value, this, getSignature());
        //return new SGraphBRDecompositionAutomaton(value, this, getSignature());
        
        //return new SGraphBRDecompositionAutomatonStoreTopDownExplicit(value, this, getSignature());//currently bugged
        //return decompose(value, SGraphBRDecompositionAutomatonBottomUp.class);
        return decompose(value, SGraphBRDecompositionAutomatonTopDownAysmptotic.class);
        //return decompose(value, SGraphBRDecompositionAutomatonTopDown.class);
    }
    
    public TreeAutomaton decompose(SGraph value, Class c){
        if (sources == null) {
            sources = getAllSourcesFromSignature(signature);
        }
        if (constantLabelInterpretations.isEmpty()) {
            precomputeAllConstants();
        }
        //try {
            if (c == SGraphDecompositionAutomaton.class){
                return new SGraphDecompositionAutomaton(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonBottomUp.class) {
                return new SGraphBRDecompositionAutomatonBottomUp(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonMPFTrusting.class) {
                return new SGraphBRDecompositionAutomatonMPFTrusting(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonStoreTopDownExplicit.class) {
                return new SGraphBRDecompositionAutomatonStoreTopDownExplicit(value, this, getSignature());
                
            } else if (c == SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas.class) {
                return new SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas(value, this, getSignature());
            } else if (c == SGraphBRDecompositionAutomatonTopDown.class) {
                return new SGraphBRDecompositionAutomatonTopDown(value, this, getSignature());
            } else if (c == SGraphBRDecompositionAutomatonTopDownAysmptotic.class) {
                return new SGraphBRDecompositionAutomatonTopDownAysmptotic(value, this, getSignature());
            }
            else return null;
        //} catch (java.lang.Exception e) {
        //    System.err.println(e.toString());
        //    return null;
        //}
    }
    
    public TreeAutomaton decompose(SGraph value, Writer writer) throws Exception{
        if (constantLabelInterpretations.isEmpty()) {
            precomputeAllConstants();
        }
        return new SGraphBRDecompositionAutomatonOnlyWrite(value, this, getSignature(), writer);
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
            } else if (label.startsWith(OP_RENAME)) {
                String[] parts = label.split("_");

                if (parts.length == 2) {
                    parts = new String[]{"r", "root", parts[1]};
                }

                return childrenValues.get(0).renameSource(parts[1], parts[2]);
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
    
    
    public static Set<String> getAllSourcesFromSignature(Signature signature) {
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
                ret.add(BOLINASROOTSTRING);
                ret.add(BOLINASSUBROOTSTRING);
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

    
    @Override
    public BinaryPartnerFinder makeNewBinaryPartnerFinder(TreeAutomaton auto) {
        if (isPure) {
            return new MPFBinaryPartnerFinder((SGraphBRDecompositionAutomatonBottomUp)auto); //To change body of generated methods, choose Tools | Templates.
        } else {
            return new ImpureMPFBinaryPartnerFinder((SGraphBRDecompositionAutomatonBottomUp)auto);
        }
    }
    
    private class MPFBinaryPartnerFinder extends BinaryPartnerFinder{
        MergePartnerFinder mpf;
        BitSet seen = new BitSet();
        public MPFBinaryPartnerFinder(SGraphBRDecompositionAutomatonBottomUp auto) {
            mpf = new DynamicMergePartnerFinder(0 , auto.completeGraphInfo.getNrSources(), auto.completeGraphInfo.getNrNodes(), auto);
        }
        
        @Override
        public IntCollection getPartners(int labelID, int stateID) {
            if (labelID == mergeLabelID) {
                return mpf.getAllMergePartners(stateID);
            } else {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }

        @Override
        public void addState(int stateID) {
            if (!seen.get(stateID)) {
                mpf.insert(stateID);
                seen.set(stateID);
            }
        }
        
    }
    
    private class ImpureMPFBinaryPartnerFinder extends BinaryPartnerFinder{
        MergePartnerFinder mpf;
        IntSet backupset;
        public ImpureMPFBinaryPartnerFinder(SGraphBRDecompositionAutomatonBottomUp auto) {
            mpf = new DynamicMergePartnerFinder(0 , auto.completeGraphInfo.getNrSources(), auto.completeGraphInfo.getNrNodes(), auto);
            backupset = new IntOpenHashSet();
        }
        
        @Override
        public IntCollection getPartners(int labelID, int stateID) {
            if (signature.resolveSymbolId(labelID).equals(OP_MERGE)) {
                return mpf.getAllMergePartners(stateID);
            } else {
                return backupset;
            }
        }

        @Override
        public void addState(int stateID) {
            if (!backupset.contains(stateID)) {
                mpf.insert(stateID);
                backupset.add(stateID);
            }
        }
        
    }
    
    
    
    public static void main(String[] args) throws Exception {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream( SGraphBRDecompositionAutomatonTopDown.HRG.getBytes( Charset.defaultCharset() ) ));
        Map<String, String> map = new HashMap<>();
        map.put("graph","(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))");
        TreeAutomaton chart = irtg.parse(map);
        System.err.println(chart);
    }

    
    
}
