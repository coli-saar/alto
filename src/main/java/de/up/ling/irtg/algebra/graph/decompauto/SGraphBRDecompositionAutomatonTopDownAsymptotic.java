/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.BRepComponent;
import de.up.ling.irtg.algebra.graph.BRepTopDown;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.ParseTester;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.PMFactoryRestrictive;
import de.up.ling.irtg.automata.condensed.PatternMatchingInvhomAutomatonFactory;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author jonas
 */
public class SGraphBRDecompositionAutomatonTopDownAsymptotic extends TreeAutomaton<BRepTopDown>{

    public final GraphInfo completeGraphInfo;
    
    final GraphAlgebra algebra;
    
    public final Set<BRepTopDown>[] storedConstants;
    
    
    final Int2ObjectMap<Int2ObjectMap<List<Rule>>> storedRules;
    
    //final Long2ObjectMap<Long2IntMap> storedStates;
    
    private final Map<BRepComponent, BRepComponent> storedComponents;
    
    public SGraphBRDecompositionAutomatonTopDownAsymptotic(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);
        this.algebra = algebra;
        //getStateInterner().setTrustingMode(true);

        completeGraphInfo = new GraphInfo(completeGraph, algebra, signature);
        storedComponents = new HashMap<>(); 
        
        
        
        storedConstants = new HashSet[algebra.getSignature().getMaxSymbolId()+1];
        Map<String, Integer> symbols = algebra.getSignature().getSymbolsWithArities();
        for (String label : symbols.keySet()) {
            if (symbols.get(label) == 0) {
                int labelID = algebra.getSignature().getIdForSymbol(label);
                storedConstants[labelID] = new HashSet<>();
                SGraph sgraph;
                try {
                    sgraph = algebra.parseString(label);
                } catch (java.lang.Exception e) {
                    sgraph = null;
                    System.err.println("parsing error when creating Top Down automaton!");
                }
                completeGraphInfo.getSGraph().foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        storedConstants[labelID].add(new BRepTopDown(matchedSubgraph, storedComponents, completeGraphInfo));
                        //System.err.println("found constant: "+labelID+"/"+matchedSubgraph.toIsiAmrString());
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });
                
            }
        }
        Set<BRepTopDown> completeGraphStates = new HashSet<>();
        SGraph bareCompleteGraph = completeGraph.forgetSourcesExcept(new HashSet<>());
        completeGraphStates.add(new BRepTopDown(bareCompleteGraph, storedComponents, completeGraphInfo));
        for (int source = 0; source < completeGraphInfo.getNrSources(); source++) {
            Set<BRepTopDown> newHere = new HashSet<>();
            for (BRepTopDown oldRep : completeGraphStates) {
                for (BRepComponent comp : oldRep.getComponents()) {
                    Int2ObjectMap<BRepComponent> nonsplitChildren = comp.getAllNonSplits(storedComponents, completeGraphInfo);
                    for (int v : nonsplitChildren.keySet()) {
                        BRepTopDown child = oldRep.forgetReverse(source, v, comp, nonsplitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                    Int2ObjectMap<Set<BRepComponent>> splitChildren = comp.getAllSplits(storedComponents, completeGraphInfo);
                    for (int v : splitChildren.keySet()) {
                        BRepTopDown child = oldRep.forgetReverse(source, v, comp, splitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                }
            }
            completeGraphStates.addAll(newHere);
        }
        for (BRepTopDown completeRep : completeGraphStates) {
            int x = addState(completeRep);
            finalStates.add(x);
        }
        
        
        storedRules = new Int2ObjectOpenHashMap<>();
        
    
    }



    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        
        Int2ObjectMap<List<Rule>> rulesHere = storedRules.get(parentState);

        // check stored rules
        if (rulesHere != null) {
            List<Rule> rules = rulesHere.get(labelId);
            if (rules != null) {
                ParseTester.cachedAnswers+=rules.size();
                switch (signature.getArity(labelId)) {
                    case 0: ParseTester.averageLogger.increaseValue("constants recognised"); break;
                    case 1: ParseTester.averageLogger.increaseValue("unaries recognised"); break;
                    case 2: ParseTester.averageLogger.increaseValue("merges recognised"); break;
                }
                return rules;
            }
        }

        String label = signature.resolveSymbolId(labelId);
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        BRepTopDown parent = getStateForId(parentState);
        List<Rule> rules = new ArrayList<>();
        
        if (label.equals(GraphAlgebra.OP_MERGE)) {
            ParseTester.averageLogger.increaseValue("merge tests");
            Set<BRepComponent> parentComponents = parent.getComponents();
            
            getAllNonemptyComponentDistributions(parentComponents).forEach(pair -> {
                BRepTopDown child0 = parent.getChildFromComponents(pair.getLeft());
                BRepTopDown child1 = parent.getChildFromComponents(pair.getRight());
                rules.add(makeRule(parentState, labelId, new BRepTopDown[]{child0, child1}));
                rules.add(makeRule(parentState, labelId, new BRepTopDown[]{child1, child0}));
                if (!child0.isConnected() || !child1.isConnected()) {
                    ParseTester.averageLogger.increaseValueBy("total disconnected merge rules", 2);
                }
            });
            ParseTester.averageLogger.increaseValueBy("total merge rules", rules.size());
            
            
            
        } else if (label.startsWith(GraphAlgebra.OP_MERGE)) {
            ParseTester.averageLogger.increaseValue("comibed rename-merge tests");
            List<BRepTopDown[]> allSplits = new ArrayList<>();
            Set<BRepComponent> parentComponents = parent.getComponents();
            
            getAllNonemptyComponentDistributions(parentComponents).forEach(pair -> {
                BRepTopDown child0 = parent.getChildFromComponents(pair.getLeft());
                BRepTopDown child1 = parent.getChildFromComponents(pair.getRight());
                allSplits.add(new BRepTopDown[]{child0, child1});
                allSplits.add(new BRepTopDown[]{child1, child0});
            });
            for (BRepTopDown[] childStates : allSplits) {
                
                String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_MERGE.length()+1);
                int[] renameSources = completeGraphInfo.getlabelSources(signature.getIdForSymbol(renameLabel));
                
                BRepTopDown renamedRight = childStates[1].renameReverse(renameSources[0], renameSources[1]);
                if (renamedRight != null) {
                    rules.add(makeRule(parentState, labelId, new BRepTopDown[]{childStates[0], renamedRight}));
                }
                
            }
        } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
            ParseTester.averageLogger.increaseValue("forget tests");
            int forgottenSource = completeGraphInfo.getlabelSources(labelId)[0];
            
            if (parent.getSourceNode(forgottenSource) == -1) {
                ParseTester.averageLogger.increaseValue("successfull forget tests");
                for (BRepComponent comp : parent.getComponents()) {
                    Int2ObjectMap<BRepComponent> nonsplitChildren = comp.getAllNonSplits(storedComponents, completeGraphInfo);
                    for (int v : nonsplitChildren.keySet()) {
                        BRepTopDown child = parent.forgetReverse(forgottenSource, v, comp, nonsplitChildren.get(v));
                        if (child != null) {
                            rules.add(makeRule(parentState, labelId, new BRepTopDown[]{child}));
                        }
                    }
                    Int2ObjectMap<Set<BRepComponent>> splitChildren = comp.getAllSplits(storedComponents, completeGraphInfo);
                    for (int v : splitChildren.keySet()) {
                        BRepTopDown child = parent.forgetReverse(forgottenSource, v, comp, splitChildren.get(v));
                        if (child != null) {
                            rules.add(makeRule(parentState, labelId, new BRepTopDown[]{child}));
                        }
                    }
                }
                ParseTester.averageLogger.increaseValueBy("total forget rules", rules.size());
            }
            //else just dont add a rule
            
            
        } else if (label.startsWith(GraphAlgebra.OP_RENAME)) {
            ParseTester.averageLogger.increaseValue("rename tests");
            int[] renamedSources = completeGraphInfo.getlabelSources(labelId);
            BRepTopDown child = parent.renameReverse(renamedSources[0], renamedSources[1]);
            if (child != null) {
                ParseTester.averageLogger.increaseValue("successfull rename tests");
                rules.add(makeRule(parentState, labelId, new BRepTopDown[]{child}));
            }
            
            
        } else {
            ParseTester.averageLogger.increaseValue("constant tests");
            if (storedConstants[labelId].contains(parent)) {
            ParseTester.averageLogger.increaseValue("constants found");
                rules.add(makeRule(parentState, labelId, new BRepTopDown[0]));
            }
        }
        
        ParseTester.newAnswers+= rules.size();
        return memoize(rules, labelId, parentState);
        
    }

    
    
    
    
    
    Rule makeRule(int parentState, int labelId, BRepTopDown[] children) {
        int[] childStates = new int[children.length];
        
        for (int i = 0; i<children.length; i++) {
            childStates[i] = addState(children[i]);
        }
        return createRule(parentState, labelId, childStates, 1);
    }
    
    
    
    
    private Iterable<Rule> memoize(List<Rule> rules, int labelId, int parentState) {
        Int2ObjectMap<List<Rule>> rulesHere = storedRules.get(parentState);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(parentState, rulesHere);
        }

        rulesHere.put(labelId, rules);
        return rules;
    }
    
    
    
    
    @Override
    public boolean isBottomUpDeterministic() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = completeGraphInfo.getSGraph().getNode(nodename);

                if (!completeGraphInfo.getSGraph().getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + completeGraphInfo.getSGraph());
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : completeGraphInfo.getSGraph().getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : completeGraphInfo.getSGraph().getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    
    
    private List<Pair<Set<BRepComponent>, Set<BRepComponent>>> getAllNonemptyComponentDistributions(Set<BRepComponent> components) {
        if (components.isEmpty()) {
            return new ArrayList<>();
        } else {
            Set<BRepComponent> input = new HashSet<>(components);
            BRepComponent comp = components.iterator().next();
            input.remove(comp);
            Set<BRepComponent> with = new HashSet<>();
            with.add(comp);
            
            return getAllNonemptyComponentDistributionsRecursive(input, new ImmutablePair<>(with, new HashSet<>()));
        }
    }
    
    private List<Pair<Set<BRepComponent>, Set<BRepComponent>>> getAllNonemptyComponentDistributionsRecursive(Set<BRepComponent> todo, Pair<Set<BRepComponent>, Set<BRepComponent>> decided) {
        if (todo.isEmpty()) {
            List<Pair<Set<BRepComponent>, Set<BRepComponent>>> ret = new ArrayList<>();
            if (!decided.getRight().isEmpty() && !decided.getLeft().isEmpty()) {
                ret.add(decided);
            }
            return ret;
        } else {
            Set<BRepComponent> newTodo = new HashSet<>(todo);
            BRepComponent comp = todo.iterator().next();
            newTodo.remove(comp);
            Set<BRepComponent> withLeft = new HashSet<>(decided.getLeft());
            Set<BRepComponent> withRight = new HashSet<>(decided.getRight());
            Set<BRepComponent> withoutLeft = new HashSet<>(decided.getLeft());
            Set<BRepComponent> withoutRight = new HashSet<>(decided.getRight());
            withLeft.add(comp);
            withoutRight.add(comp);
            Pair<Set<BRepComponent>, Set<BRepComponent>> newDecidedWith = new ImmutablePair<>(withLeft, withRight);
            Pair<Set<BRepComponent>, Set<BRepComponent>> newDecidedWithout = new ImmutablePair<>(withoutLeft, withoutRight);
            List<Pair<Set<BRepComponent>, Set<BRepComponent>>> ret = getAllNonemptyComponentDistributionsRecursive(newTodo, newDecidedWith);
            ret.addAll(getAllNonemptyComponentDistributionsRecursive(newTodo, newDecidedWithout));
            return ret;
        }
    }
    
    @Override
    public boolean supportsBottomUpQueries() {
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean supportsTopDownQueries() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    static String corpusPath = "corpora-and-grammars/corpora/amr-bank-v1.3.txt";
    static String grammarPath = "corpora-and-grammars/grammars/sgraph_bolinas_comparison/lexicalized/rules.txt";
    
    public static void main(String[] args) throws Exception {
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        inducer.getCorpus().sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));
        PatternMatchingInvhomAutomatonFactory pm = new PMFactoryRestrictive(irtg.getInterpretation("int").getHomomorphism(), irtg.getInterpretation("int").getAlgebra());
        
        
        int start = 0;//maybe start later to get to interesting graphs sooner
        int stop = 200;//inducer.getCorpus().size();

        int iterations = 1;
        int internalIterations = 1;


        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();



        sw.record(0);

        for (int j = 0; j < iterations; j++) {
            for (int i = start; i < stop; i++) {
                System.err.println("i = " + i+":");
                parseInstance(inducer.getCorpus(), pm, (GraphAlgebra)irtg.getInterpretation("int").getAlgebra(), i, null, internalIterations, internalSw);
            }
        }
        sw.record(1);

        sw.printMilliseconds("parsing trees from " + start + " to " + stop + "(" + (iterations * internalIterations) + " iterations)");

    }
        
       

    public static void parseInstance(List<IrtgInducer.TrainingInstance> corpus, PatternMatchingInvhomAutomatonFactory pm, GraphAlgebra alg, int i, Writer resultWriter, int internalIterations, CpuTimeStopwatch internalSw) {
        IrtgInducer.TrainingInstance ti = corpus.get(i);
        if (ti == null) {
            return;
        }
        internalSw.record(0);
        TreeAutomaton chart = null;
//        System.err.println("\n" + ti.graph);
        for (int j = 0; j < internalIterations; j++) {
            SGraphBRDecompositionAutomatonTopDownAsymptotic rhs = (SGraphBRDecompositionAutomatonTopDownAsymptotic)alg.decompose(ti.graph, SGraphBRDecompositionAutomatonTopDownAsymptotic.class);
            chart = pm.invhom(rhs);
            
            //chart.viterbi();
        }
        internalSw.record(1);
        internalSw.printMilliseconds("time: ");
        System.err.println(ti.graph.toIsiAmrString());
        System.err.println(chart.countTrees());
    }
    
}
