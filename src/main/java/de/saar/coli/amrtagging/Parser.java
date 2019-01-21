/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.Type;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import static de.up.ling.irtg.util.Util.gensym;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeBottomUpVisitor;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The projective decoder of the ACL 2018 paper. Can be used via the Parser2ExtFormat command line interface.
 * @author JG
 */
public class Parser {
    
    private static final SGraph UNPARSEABLE_GRAPH = new IsiAmrInputCodec().read("(n/unparseable)");
    private static final SGraph FAILED_EVAL_GRAPH = new IsiAmrInputCodec().read("(n/failedEval)");
    private static final double DEFAULT_EDGE_SCORE = 0.001;
    //private static final String[] ALL_SOURCES = new String[]{AMSignatureBuilder.DOMAIN, AMSignatureBuilder.MOD, AMSignatureBuilder.OBJ, AMSignatureBuilder.SUBJ,
    //    "o1", "o2", "o3", "o4", "o5", "o6", "o7", "o8", "o9", "op1", "op2", "op3", "op4", "op5", "op6", "op7", "op8", "op9"};
    
    public static final String LEXMARKER_OUT = "LEX@";
    
    private final InterpretedTreeAutomaton irtg;
    private final List<String> sentInternal;
    private final Map<String, String> ruleLabel2tag;
	private Interpretation<List<String>> stringInterpretation;
	private Interpretation<Pair<SGraph, ApplyModifyGraphAlgebra.Type>> graphInterpretation;
    
    /**
     * 
     * @param fragmentProbs assumed to be sorted for each word
     * @param labelProbs assumed to be sorted for each word
     * @param edgeLabel2pos2pos2prob
     * @param sent
     * @param maxK number of tags per word
     * @param edgeExponent
     * @param edgeFactor
     * @param tagExponent
     * @param addNull
     * @param addEdges
     * @param stringAlg
     * @throws ParseException 
     */
    Parser(List<List<Pair<String, Double>>> fragmentProbs, List<List<Pair<String, Double>>> labelProbs,
            Map<String, Int2ObjectMap<Int2DoubleMap>> edgeLabel2pos2pos2prob, List<String> sent, int maxK,
            double edgeExponent, double edgeFactor, double tagExponent,
            boolean addNull, boolean addEdges, StringAlgebra stringAlg) throws ParseException {
        //add index tags to the sentence
        sentInternal = new ArrayList<>();
        for (int i = 0; i<sent.size(); i++) {
            sentInternal.add(sent.get(i)+"@"+i);
        }
        
        ruleLabel2tag = new HashMap<>();
        
        int l = fragmentProbs.size();
        Set<Type>[] types = new Set[l];
        for (int i = 0; i<l; i++) {
            types[i] = new HashSet<>();
            List<Pair<String, Double>> tAndPs = fragmentProbs.get(i);
            for (int k = 0; k<Math.min(maxK, tAndPs.size()); k++) {
                Pair<String, Double> tAndP = tAndPs.get(k);
                if (tAndP.left.contains(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)) {
                    types[i].addAll(new Type(tAndP.left.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)[1]).getAllSubtypes());
                }
            }
        }
        //Set<Type> allTypes = new HashSet<>();
        //Arrays.stream(types).forEach(set -> allTypes.addAll(set));
        
        //build homomorphisms and grammar automaton
        Signature grammarSig = new Signature();
        Signature graphSig = new Signature();
        ConcreteTreeAutomaton<String> grammarAuto = new ConcreteTreeAutomaton<>(grammarSig);
        Homomorphism stringHom = new Homomorphism(grammarSig, stringAlg.getSignature());
        ApplyModifyGraphAlgebra graphAlg = new ApplyModifyGraphAlgebra(graphSig);
        Homomorphism graphHom = new Homomorphism(grammarSig, graphSig);
        
        String concatSymbol = StringAlgebra.CONCAT;
        
        //add constants and NULL operations
        for (int i = 0; i<l; i++) {
            List<Pair<String, Double>> ifp = fragmentProbs.get(i);//graph fragment probabilities at word i //f for fragment, i for i, p for probability
            List<Pair<String, Double>> ilp = labelProbs == null ? null : labelProbs.get(i);//only get label probs if there are any //l for label, i for i, p for probability
            double min = 1.0;
            boolean foundNull = false;
            for (int k = 0; k<Math.min(maxK, ifp.size()); k++) {
                Pair<String, Double> fAndP = ifp.get(k);
                if (fAndP.left.contains(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)) {
                    //i.e. we actually have a graph fragment
                    String label = "\""+LEXMARKER_OUT+i+"\"";//mark label with word position (this may be replaced in next if clause), for use in a different script.
                    if (ilp != null) {
                        //if we have labels given, just take best non-null label
                        label = ilp.get(0).left;
                        if (label.equals("NULL") && ilp.size() > 1) {
                            label = ilp.get(1).left;
                        }
                    }
                    String fullGraphString = Util.raw2readable(fAndP.left).replace(DependencyExtractor.LEX_MARKER, Util.raw2readable(label));
                    String type = fAndP.left.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)[1];
                    int head = i;
                    String nt = head+"|"+new Type(type).toString();//to make strings consistent with method below
                    String ruleLabel = gensym("const_"+i+"__");
                    ruleLabel2tag.put(ruleLabel, fAndP.left);
                    grammarAuto.addRule(grammarAuto.createRule(nt, ruleLabel, new String[0], Math.pow(fAndP.right, tagExponent)));
                    min = Math.min(min, Math.pow(fAndP.right, tagExponent));
                    graphHom.add(ruleLabel, Tree.create(fullGraphString));
                    stringHom.add(ruleLabel, Tree.create(sentInternal.get(i)));
                } else {
                    //then we add an IGNORE rule
                    foundNull = true;
                    for (int head = 0; head<l; head++) {
                        for (Type type : types[head]) {
                            if (head != i) {
                                String nt = head+"|"+type;
                                String ruleLabel = gensym("NULL_"+i+"__");
                                grammarAuto.addRule(grammarAuto.createRule(nt, ruleLabel, new String[]{nt}, Math.pow(fAndP.right, tagExponent)));
                                Tree tNull = Tree.create(sentInternal.get(i));
                                Tree tVar = Tree.create("?1");
                                Tree<String> stringOp;
                                if (head < i) {
                                    stringOp = Tree.create(concatSymbol, tVar, tNull);
                                } else {
                                    stringOp = Tree.create(concatSymbol, tNull, tVar);
                                }
                                stringHom.add(ruleLabel, stringOp);
                                graphHom.add(ruleLabel, Tree.create("?1"));
                            }
                        }
                    }
                }
            }
            if (addNull && !foundNull) {
                // additionally add an IGNORE rule if settings require it.
                for (int head = 0; head<l; head++) {
                    for (Type type : types[head]) {
                        if (head != i) {
                            String nt = head+"|"+type;
                            String ruleLabel = gensym("NULL_"+i+"__");
                            grammarAuto.addRule(grammarAuto.createRule(nt, ruleLabel, new String[]{nt}, min*0.00001));
                            Tree tNull = Tree.create(sentInternal.get(i));
                            Tree tVar = Tree.create("?1");
                            Tree<String> stringOp;
                            if (head < i) {
                                stringOp = Tree.create(concatSymbol, tVar, tNull);
                            } else {
                                stringOp = Tree.create(concatSymbol, tNull, tVar);
                            }
                            stringHom.add(ruleLabel, stringOp);
                            graphHom.add(ruleLabel, Tree.create("?1"));
                        }
                    }
                }
            }
        }
        
        
        //binary operations
        List<Tree<String>> stringTrees = new ArrayList<>();
        List<Tree<String>> reverseStringTrees = new ArrayList<>();
        for (int id = 1; id <= stringAlg.getSignature().getMaxSymbolId(); id++) {
            if (stringAlg.getSignature().getArity(id) == 2) {
                String label = stringAlg.getSignature().resolveSymbolId(id);
                Tree<String> tree = Tree.create(label, Tree.create("?1"), Tree.create("?2"));
                stringTrees.add(tree);
                Tree<String> reverseTree = Tree.create(label, Tree.create("?2"), Tree.create("?1"));
                reverseStringTrees.add(reverseTree);
            }
        }
        //add APPs and MODs
        for (int h1 = 0; h1<l; h1++) {
            for (int h2=h1+1; h2<l; h2++) {
                //APP_lr
                for (Type t1 : types[h1]) {
                    for (String s : t1.keySet()) {
                        Type res = t1.simulateApply(s);
                        if (res != null) {
                            String nt = h1+"|"+res.toString();
                            String nt1 = h1+"|"+t1.toString();
                            for (Type t2 : types[h2]) {
                                if (t1.canApplyTo(t2, s)) {
                                    String nt2 = h2+"|"+t2.toString();
                                    for (Tree<String> stringTree : stringTrees) {
                                        addOp(stringHom, graphHom, h1, h2, s, nt, grammarAuto, stringTree, ApplyModifyGraphAlgebra.OP_APPLICATION,
                                            "lr", new String[]{nt1, nt2}, false, edgeLabel2pos2pos2prob, addEdges, edgeExponent, edgeFactor);
                                    }
                                }
                            }
                        }
                    }
                }
                //APP_rl //TODO unify duplicate code
                for (Type t2 : types[h2]) {
                    for (String s : t2.keySet()) {
                        Type res = t2.simulateApply(s);
                        if (res != null) {
                            String nt = h2+"|"+res.toString();
                            String nt2 = h2+"|"+t2.toString();
                            for (Type t1 : types[h1]) {
                                if (t2.canApplyTo(t1, s)) {
                                    String nt1 = h1+"|"+t1.toString();
                                    for (Tree<String> reverseStringTree : reverseStringTrees) {
                                        addOp(stringHom, graphHom, h1, h2, s, nt, grammarAuto, reverseStringTree, ApplyModifyGraphAlgebra.OP_APPLICATION,
                                                "rl", new String[]{nt2, nt1}, true, edgeLabel2pos2pos2prob, addEdges, edgeExponent, edgeFactor);
                                    }
                                }
                            }
                        }
                    }
                }
                //MOD_lr //TODO unify duplicate code
                for (Type t1 : types[h1]) {
                    String nt = h1+"|"+t1.toString();
                    String nt1 = h1+"|"+t1.toString();
                    for (Type t2 : types[h2]) {
                        for (String s : t2.keySet()) {
                            if (t1.canBeModifiedBy(t2, s)) {
                                String nt2 = h2+"|"+t2.toString();
                                for (Tree<String> stringTree : stringTrees) {
                                    addOp(stringHom, graphHom, h1, h2, s, nt, grammarAuto, stringTree, ApplyModifyGraphAlgebra.OP_MODIFICATION,
                                            "lr", new String[]{nt1, nt2}, false, edgeLabel2pos2pos2prob, addEdges, edgeExponent, edgeFactor);
                                }
                            }
                        }
                    }
                }
                //MOD_rl //TODO unify duplicate code
                for (Type t2 : types[h2]) {
                    String nt = h2+"|"+t2.toString();
                    String nt2 = h2+"|"+t2.toString();
                    for (Type t1 : types[h1]) {
                        for (String s : t1.keySet()) {
                            if (t2.canBeModifiedBy(t1, s)) {
                                String nt1 = h1+"|"+t1.toString();
                                for (Tree<String> reverseStringTree : reverseStringTrees) {
                                    addOp(stringHom, graphHom, h1, h2, s, nt, grammarAuto, reverseStringTree, ApplyModifyGraphAlgebra.OP_MODIFICATION,
                                            "rl", new String[]{nt2, nt1}, true, edgeLabel2pos2pos2prob, addEdges, edgeExponent, edgeFactor);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        //unary string side operations
        for (int stateID : grammarAuto.getAllStates()) {
            String state = grammarAuto.getStateForId(stateID);
            for (int sigID = 1; sigID <= stringAlg.getSignature().getMaxSymbolId(); sigID++) {
                if (stringAlg.getSignature().getArity(sigID) == 1) {
                    String label = stringAlg.getSignature().resolveSymbolId(sigID);
                    String ruleLabel = gensym("unary_"+label);
                    grammarAuto.addRule(grammarAuto.createRule(state, ruleLabel, new String[]{state}, 1.0));
                    stringHom.add(ruleLabel, Tree.create(label, Tree.create("?1")));
                    graphHom.add(ruleLabel, Tree.create("?1"));
                }
            }
        }
        //add final states to grammar automaton
        for (int stateID : grammarAuto.getAllStates()) {
            String state = grammarAuto.getStateForId(stateID);
            if (state.split("\\|")[1].equals("()")) {
                grammarAuto.addFinalState(stateID);
            }
        }
        
        irtg = new InterpretedTreeAutomaton(grammarAuto);
		stringInterpretation = new Interpretation(stringAlg, stringHom, "string");
        irtg.addInterpretation(stringInterpretation);
		graphInterpretation = new Interpretation(graphAlg, graphHom, "graph");
        irtg.addInterpretation(graphInterpretation);
    }
    
    private static void addOp(Homomorphism stringHom, Homomorphism graphHom, int h1, int h2, String s, String nt,
            ConcreteTreeAutomaton<String> grammarAuto, Tree<String> concatTree, String opType, String lr, String[] nonterminals,
            boolean reverse, Map<String, Int2ObjectMap<Int2DoubleMap>> edgeLabel2pos2pos2prob, boolean addUnmentionedEdges,
            double exponent, double factor) {
        
        int first = reverse ? h2 : h1;
        int second = reverse ? h1 : h2;
        String graphOp = opType+s;
        String ruleLabel = gensym(opType+concatTree.getLabel().replaceAll("_", "")+"_"+lr+"_"+s+"_"+h1+"_"+h2+"__");
        Pair<Double, Boolean> weightAndSuccess = getProb(first, second, graphOp, exponent, factor, edgeLabel2pos2pos2prob);
        if (weightAndSuccess.right || addUnmentionedEdges) {
            grammarAuto.addRule(grammarAuto.createRule(nt, ruleLabel, nonterminals, weightAndSuccess.left));
            stringHom.add(ruleLabel, concatTree);
            graphHom.add(ruleLabel, Tree.create(graphOp, Tree.create("?1"), Tree.create("?2")));
        }
    }
    
    private static Pair<Double, Boolean> getProb(int first, int second, String graphOp, double exponent,
            double factor, Map<String, Int2ObjectMap<Int2DoubleMap>> edgeLabel2pos2pos2prob) {
        if (edgeLabel2pos2pos2prob == null) {
            return new Pair<>(Math.pow(DEFAULT_EDGE_SCORE, exponent)*factor, false);
        }
        Int2ObjectMap<Int2DoubleMap> pos2pos2prob = edgeLabel2pos2pos2prob.get(graphOp);
        if (pos2pos2prob == null) {
            return new Pair<>(Math.pow(DEFAULT_EDGE_SCORE, exponent)*factor, false);
        }
        Int2DoubleMap pos2prob = pos2pos2prob.get(first);
        if (pos2prob == null || !pos2prob.containsKey(second)) {
            return new Pair<>(Math.pow(DEFAULT_EDGE_SCORE, exponent*factor), false);
        }
        return new Pair<>(Math.pow(pos2prob.get(second), exponent)*factor, true);
    }
    
    Pair<SGraph, Tree<String>> run() throws ParserException {
		List<String> input = stringInterpretation.getAlgebra().parseString(sentInternal.stream().collect(Collectors.joining(" ")));
        TreeAutomaton<Pair<String, List<String>>> auto = irtg.parseSimple(stringInterpretation, input);
        auto.makeAllRulesExplicit();
        //System.err.println(auto);
        Tree<String> vit = auto.viterbi();
        if (vit == null) {
            return new Pair<>(UNPARSEABLE_GRAPH, null);
        } else {
            try {
                SGraph ret = graphInterpretation.interpret(vit).left;
                return new Pair<>(ret, vit);
            } catch (java.lang.Exception ex) {
                System.err.println("Evaluation in algebra failed!");
                System.err.println(de.up.ling.irtg.util.Util.getStackTrace(ex));
                System.err.println(vit);
                //System.err.println(irtg); // probably not smart to always print the full IRTG
                return new Pair<>(FAILED_EVAL_GRAPH, null);
            }
        }
    }
    
    void printIRTG() {
        System.err.println(irtg);
    }
    
    Pair<String[], List<String>> getConstraintsFromTree(Tree<String> tree, int sentLength) {
        //MOD_*_rl_mod_0_1__20
        //const_1__5
        String[] tags = new String[sentLength];
        Arrays.fill(tags, "NULL");
        List<String> ops = new ArrayList<>();
        Homomorphism hom = graphInterpretation.getHomomorphism();
        tree.dfs((TreeBottomUpVisitor<String, Void>) (node, childrenValues) -> {
            String[] parts = node.getLabel().split("_");
            if (parts[0].equals("const")) {
                tags[Integer.parseInt(parts[1])] = ruleLabel2tag.get(node.getLabel());
            } else if (parts[0].startsWith("MOD") || parts[0].startsWith("APP")) {
                int first;
                int second;
                if (parts[2].equals("lr")) {
                    first = Integer.parseInt(parts[4]);
                    second = Integer.parseInt(parts[5]);
                } else {
                    first = Integer.parseInt(parts[5]);
                    second = Integer.parseInt(parts[4]);
                }
                ops.add(hom.get(node.getLabel()).getLabel()+"["+first+","+second+"]");
            }
            return null;
        });
        return new Pair<>(tags, ops);
    }
    
    
}
