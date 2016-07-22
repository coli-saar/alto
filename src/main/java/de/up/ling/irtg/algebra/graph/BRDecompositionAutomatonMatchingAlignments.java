/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Iterables;
import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.BinaryPartnerFinder;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.script.AMR_String_Tree_preprocessing.DecompsForAlignedGraphStringGrammarInduction;
import de.up.ling.irtg.util.TupleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author groschwitz
 */
public class BRDecompositionAutomatonMatchingAlignments extends SGraphBRDecompositionAutomatonBottomUp {

    private final double violationPenalty;
    
    private final Map<String, Set<Integer>> nodeName2Alignment;
    private final Int2IntMap alignment2StringPos;
    
    private final Int2IntMap state2Violations;
    private final Int2ObjectMap<IntSet> stringPos2alignments;
    
    public BRDecompositionAutomatonMatchingAlignments(SGraph completeGraph, GraphAlgebra algebra, Map<String, Set<Integer>> nodeName2Alignment, Int2IntMap alignment2StringPos,
            double violationPenalty) {
        super(completeGraph, algebra);
        this.nodeName2Alignment = matchAnonymousNodenames2Graph(completeGraph, nodeName2Alignment);
        this.alignment2StringPos = alignment2StringPos;
        this.violationPenalty = violationPenalty;
        state2Violations = new Int2IntOpenHashMap();
        stringPos2alignments = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i <= Collections.max(alignment2StringPos.values()); i++) {
            stringPos2alignments.put(i, new IntOpenHashSet());
        }
        for (Int2IntMap.Entry entry : alignment2StringPos.int2IntEntrySet()) {
            stringPos2alignments.get(entry.getIntValue()).add(entry.getIntKey());
        }
    }
    
    private Map<String, Set<Integer>> matchAnonymousNodenames2Graph(SGraph completeGraph, Map<String, Set<Integer>> nodeName2Alignment) {
        Map<String, Set<Integer>> ret = new HashMap<>();
        
        List<String> anonNamesAlign = new ArrayList<>();
        
        for (Entry<String, Set<Integer>> entry : nodeName2Alignment.entrySet()) {
            if (entry.getKey().startsWith("_u")) {
                anonNamesAlign.add(entry.getKey());
            } else {
                ret.put(entry.getKey(), entry.getValue());
            }
        }
        
        List<String> anonNamesGraph = new ArrayList<>();
        for (String name : completeGraph.getAllNodeNames()) {
            if (name.startsWith("_u")) {
                anonNamesGraph.add(name);
            }
        }
        
        Collections.sort(anonNamesAlign);
        Collections.sort(anonNamesGraph);
        //System.err.println("Alignment: " + anonNamesAlign);
        //System.err.println("Graph: " + anonNamesGraph);
        
        
        for (int i = 0; i<anonNamesGraph.size(); i++) {
            if (i < anonNamesAlign.size()) {
                ret.put(anonNamesGraph.get(i), nodeName2Alignment.get(anonNamesAlign.get(i)));
            } else {
                System.err.println("WARNING: node "+anonNamesGraph.get(i) + " with label '"+completeGraph.getNode(anonNamesGraph.get(i)).getLabel()+"' could not be found in alignments");
            }
            
            
        }
        
        return ret;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        List<Rule> ret = new ArrayList<>();
        for (Rule rule : super.getRulesBottomUp(labelId, childStates)) {
            if (rule.getLabel(this).equals(GraphAlgebra.OP_MERGE)) {
                ret.add(createRule(rule.getParent(), labelId, childStates, Math.pow(violationPenalty, getAlignmentViolations(rule.getParent()))));
            } else {
                ret.add(rule);
            }
        }
        return Iterables.filter(ret, rule -> confirmRule(rule));
    }
    
    private boolean confirmRule(Rule rule) {
        //boolean matchesAlignments = stateMatchesAlignments(rule.getParent());
        boolean mergeAllowed_orNoMerge = mergeIsAllowed_orNotMerge(rule);
        return mergeAllowed_orNoMerge;//(matchesAlignments && mergeAllowed_orNoMerge);
    }
    
    private boolean mergeIsAllowed_orNotMerge(Rule rule) {
        if (!rule.getLabel(this).equals(GraphAlgebra.OP_MERGE)) {
            return true;
        } else {
            BoundaryRepresentation left = getStateForId(rule.getChildren()[0]);
            boolean leftIsSingleEdge = isSingleEdge(left);
            BoundaryRepresentation right = getStateForId(rule.getChildren()[1]);
            boolean rightIsSingleEdge = isSingleEdge(right);
            
            if (leftIsSingleEdge) {
                return false; //if only one single edge, it must be the right one. Both single edges not allowed either.
            } else if (!leftIsSingleEdge && rightIsSingleEdge) {
                int edge = right.getInBoundaryEdges().getFirst(); //works since right is singleEdge
                
                //check if target node of edge is in left graph (that is forbidden)
                for (int source : right.getAssignedSources(completeGraphInfo.getEdgeTarget(edge))) {
                    if (left.getSourceNode(source) != -1) {
                        return false;
                    }
                }
                
                //check if any lexicographic smaller edge leaving source node of edge is not in left (must all be in)
                GraphNode sourceNode = completeGraphInfo.getSGraph().getNode(completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeSource(edge)));
                for (GraphEdge otherEdge : completeGraphInfo.getSGraph().getGraph().outgoingEdgesOf(sourceNode)) {
                    if (otherEdge.getSource().equals(sourceNode) && otherEdge != completeGraphInfo.getEdge(edge) 
                            && (completeGraphInfo.getEdge(edge).getLabel().compareTo(otherEdge.getLabel()) > 0) //then edge label comes after otherEdge label
                                    && !left.isInBoundary(otherEdge)) {
                        return false;
                    }
                }
                
                //otherwise merge is allowed
                return true;
            } else {
                //if neither are single edges, its fine.
                return true;
            }
        }
        
    }
    
    private boolean isSingleEdge(BoundaryRepresentation rep) {
        if (rep.getInBoundaryEdges().size() != 1) {
            return false;
        } else {
            int edge = rep.getInBoundaryEdges().getFirst();
            return rep.isSource(completeGraphInfo.getEdgeSource(edge)) && rep.isSource(completeGraphInfo.getEdgeTarget(edge))
                    && completeGraphInfo.getEdgeSource(edge) != completeGraphInfo.getEdgeTarget(edge);
        }
    }
            
    private int getAlignmentViolations(int state) {
        if (state2Violations.containsKey(state)) {
            return state2Violations.get(state);
        } else {
            int ret = 0;
            SGraph graph = getStateForId(state).getGraph();
            IntSet stringPositions = new IntOpenHashSet();
            IntSet alignments = new IntOpenHashSet();
            for (String nodeName : graph.getAllNodeNames()) {
                String lookupTerm = nodeName;//(nodeName.startsWith("_u")) ? graph.getNode(nodeName).getLabel() : nodeName;
                Set<Integer> alignmentsHere = nodeName2Alignment.get(lookupTerm);
                if (lookupTerm != null) {
                    //if lookupTerm is null, the label is not contained here which
                    //counts as the node not being contained. Then we don't need to check the alignments.
                    if (alignmentsHere == null) {
                        //System.err.println("WARNING: node with name '" + lookupTerm + "' and label '" + graph.getNode(nodeName).getLabel()+"' not found!");
                    } else {
                        for (int alignment : nodeName2Alignment.get(lookupTerm)) {
                            alignments.add(alignment);
                            stringPositions.add(alignment2StringPos.get(alignment));
                        }
                    }
                }
            }
            if (!stringPositions.isEmpty()) {
                int maxPos = Collections.max(stringPositions);
                int minPos = Collections.min(stringPositions);
                for (int i = minPos; i <= maxPos; i++) {
                    for (int alignment : stringPos2alignments.get(i)) {
                        if (!alignments.contains(alignment)) {
                            ret ++;
                        }
                    }
                }
            }
            state2Violations.put(state, ret);
            return ret;
        }
    }
    
    
    /**
     * Writes the decomposition automata for all specified graphs in the corpus,
     * with one restriction: There are no rename operations on states only
     * reachable via rename. Rules of the form c-> m(a, b) and c-> m(b,a) are
     * both written into the writer (this is different from previous
     * implementations).
     * Output files are formatted CORPUSID_NODECOUNTnodes.rtg
     * @param targetFolderPath The folder into which the automata are written.
     * @param corpus The corpus to parse. The graph interpretation must be
     * labeled "graph".
     * @param startIndex At which index to start (graphs are ordered by node count)
     * @param stopIndex At which index to stop (graphs are ordered by node count)
     * @param sourceCount How many sources to use in decomposition.
     * @param maxNodes A hard cap on the number of nodes allowed per graph
     * (violating graphs are not parsed). 0 for no restriction.
     * @param maxPerNodeCount If for a node count n this is less then the number
     * of graphs with n nodes, then {@code maxPerNodeCount} many are chosen
     * randomly.
     * @param alignmentReader
     * @param nodeNameListReader
     * @throws Exception
     */
    public static void writeDecompositionAutomata(String targetFolderPath, Corpus corpus, int startIndex, int stopIndex, int sourceCount, int maxNodes, int maxPerNodeCount,
            BufferedReader alignmentReader, BufferedReader nodeNameListReader, double threshold) throws Exception {
        
        
        /*List<Instance>[] graphsToParse = new List[maxNodes];
        
        int id = 1;
        for (Instance instance : corpus) {
            instance.setComments("id", String.valueOf(id));
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            if (graph.getAllNodeNames().size()<=maxNodes) {
                GraphAlgebra alg = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, sourceCount);
                
                Writer rtgWriter = new StringWriter();
                SGraphBRDecompositionAutomatonBottomUp botupAuto = new SGraphBRDecompositionAutomatonBottomUp(graph, alg);
                botupAuto.actuallyWrite = false;
                boolean foundFinalState = botupAuto.writeAutomatonRestricted(rtgWriter);
                rtgWriter.close();
                if (foundFinalState) {
                    int n = graph.getAllNodeNames().size();
                    if (graphsToParse[n-1]==null) {
                        graphsToParse[n-1]=new ArrayList<>();
                    }
                    graphsToParse[n-1].add(instance);
                }
            }
            id++;
        }
        
        //add the instances we want to parse to our custom corpus.
        Corpus corpusToParse = new Corpus();
        Random r = new Random();
        for (int i = 0; i<maxNodes; i++) {
            if (graphsToParse[i] != null) {
                System.out.println("Adding " +Math.min(graphsToParse[i].size(), maxPerNodeCount) + " graphs with " + (i+1) + " nodes.");
                if (graphsToParse[i].size()<=maxPerNodeCount) {
                    for (Instance target : graphsToParse[i]) {
                        corpusToParse.addInstance(target);
                    }
                } else {
                    for (int k = 0; k<maxPerNodeCount; k++) {
                        int targetIndex = r.nextInt(graphsToParse[i].size());
                        Instance target = graphsToParse[i].remove(targetIndex);

                        corpusToParse.addInstance(target);
                    }
                }
            }
        }
        System.out.println("Chosen IDs:");
        corpusToParse.forEach(instance-> System.out.println(instance.getComments().get("id")));
        System.out.println("---------------------");
        
        corpusToParse.sort(Comparator.comparingInt(instance -> ((SGraph)instance.getInputObjects().get("graph")).getAllNodeNames().size()
        ));*/
        
        
        IntList successfull = new IntArrayList();
        
        Iterator<Instance> it = corpus.iterator();
        int i = 0;
        int instancesTried = 0;
        while (it.hasNext()) {
            Instance instance = it.next();
            if (i>= startIndex && (i < stopIndex || stopIndex < 0)) {
                SGraph graph = (SGraph)instance.getInputObjects().get("graph");
                Pair<Map<String, Set<Integer>>, Int2IntMap> alignmentMaps = DecompsForAlignedGraphStringGrammarInduction.parseAlignments(alignmentReader.readLine(), nodeNameListReader.readLine());
                if (graph.getAllNodeNames().size()<=maxNodes) {
                    instancesTried++;
                    System.err.println(i);
                    int instanceID = i;//Integer.valueOf(instance.getComments().get("id"));
                    GraphAlgebra alg = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, sourceCount);

                    
                    String fileName = targetFolderPath+String.valueOf(instanceID)+"_"+graph.getAllNodeNames().size()+"nodes";
                    Writer rtgWriter = new FileWriter(fileName+".rtg");
                    Writer alignmentWriter = new FileWriter(fileName+".al");
                    BRDecompositionAutomatonMatchingAlignments botupAuto = new BRDecompositionAutomatonMatchingAlignments(graph, alg, alignmentMaps.left, alignmentMaps.right, 0.5);
                    boolean foundFinalState = botupAuto.writeAutomatonRestricted(rtgWriter, alignmentWriter, threshold);
                    alignmentWriter.close();
                    rtgWriter.close();
                    if (foundFinalState) {
                        successfull.add(instanceID);
                    }
                }
            }
            i++;
        }
        System.err.println(String.valueOf(successfull.size()) + " graphs were parsed successfully, out of " + String.valueOf(instancesTried));
        
        
    }
    
    
    /**
     * Writes all rules in this decomposition automaton, with one restriction:
     * There are no rename operations on states only reachable via rename.
     * Rules of the form c-> m(a, b) and c-> m(b,a) are both written into the
     * writer (this is different from previous implementations).
     * @param autoWriter
     * @param alignmentWriter
     * @param threshold
     * @return
     */
    public boolean writeAutomatonRestricted(Writer autoWriter, Writer alignmentWriter, double threshold) {
        stateInterner.setTrustingMode(true);
        count = 0;

        boolean tempDoStore = isStoring();
        setStoring(false);
        
        boolean ret = processAllRulesBottomUpUntilThreshold(rule -> {
            if (actuallyWrite) {
                try {
                    String label = rule.getLabel(this);
                    if (!label.equals(GraphAlgebra.OP_MERGE) && !label.startsWith(GraphAlgebra.OP_FORGET) && !label.startsWith(GraphAlgebra.OP_RENAME) && !label.startsWith(GraphAlgebra.OP_SWAP)) {
                        BoundaryRepresentation rep = getStateForId(rule.getParent());
                        if (!isSingleEdge(rep)) {
                            //then we have a constant that derives a node label. Based on the assumption that constants are only single edges or loops
                            StringBuilder sb = new StringBuilder(encodeShort(rule.getParent()));
                            Set<Integer> alignments = nodeName2Alignment.get(completeGraphInfo.getNodeForInt(rep.getSourceNode(0)));//based on the assumption that loops have only source name 0.
                            if (alignments != null) {
                                for (int al : alignments) {
                                    sb.append(" " + String.valueOf(alignment2StringPos.get(al)));
                                }
                            }
                            
                            alignmentWriter.write(sb.toString()+"\n");
                        }
                    }
                    writeRule(autoWriter, rule);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, 0.01);
        
        setStoring(tempDoStore);
        
        return ret;
    }
    
    /**
     * Iterates through all rules top-down, applying processingFunction to each
     * rule found. Returns true if a final state was found.
     *
     * @param processingFunction
     * @param threshold multiplicative
     * @return
     */
    public boolean processAllRulesBottomUpUntilThreshold(Consumer<Rule> processingFunction, double threshold) {
        boolean ret = false;
        
        double best = -1;

        //init basic agenda structure
        Object2DoubleMap<Rule> rule2Inside = new Object2DoubleOpenHashMap<>();
        Int2DoubleMap state2Inside = new Int2DoubleOpenHashMap();
        Queue<Rule> agenda = new PriorityQueue<>((Rule o1, Rule o2) -> {
            if (rule2Inside.get(o1)<rule2Inside.get(o2)) {
                return 1;
            } else if (rule2Inside.get(o1)>rule2Inside.get(o2)) {
                return -1;
            } else {
                return 0;
            }
        });
        
        IntSet seen = new IntOpenHashSet();
        Int2ObjectMap<IntList> symbols = new Int2ObjectOpenHashMap<>();

        //initialize agenda by processing constants
        Map<String, Integer> symbolsFromAuto = getSignature().getSymbolsWithArities();
        int j = 0;
        for (String s : symbolsFromAuto.keySet()) {
            int arity = symbolsFromAuto.get(s);
            IntList symbolsHere = symbols.get(arity);
            if (symbolsHere == null) {
                symbolsHere = new IntArrayList();
                symbols.put(arity, symbolsHere);
            }
            symbolsHere.add(getSignature().getIdForSymbol(s));
        }
        IntList constants = symbols.get(0);
        if (constants != null) {
            for (int c : constants) {
                //try {
                Iterator<Rule> it = getRulesBottomUp(c, new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();

                    rule2Inside.put(rule, rule.getWeight());
                    agenda.add(rule);
                }
            }
        }

        //now iterate
        Int2ObjectMap<IntList> labelsToIterate = new Int2ObjectOpenHashMap<>(symbols);
        labelsToIterate.remove(0);//already checked constants above

        BinaryPartnerFinder bpFinder = makeNewBinaryPartnerFinder();

        while (!agenda.isEmpty()) {
            Rule rule = agenda.poll();
            if (best != -1 && rule2Inside.get(rule) < best * threshold) {
                break;
            }
            int a = rule.getParent();
            if (processingFunction != null) {
                processingFunction.accept(rule);
            }
            
            if (!seen.contains(a)) {
                seen.add(a);
                
                //now process the fact we found a
                state2Inside.put(a, rule2Inside.getDouble(rule));
                if (getFinalStates().contains(a)) {
                    ret = true;
                    if (best == -1) {
                        best = rule2Inside.getDouble(rule);
                    }
                }

                for (int arity : labelsToIterate.keySet()) {

                    for (int label : symbols.get(arity)) {
                        //find all rules
                        List<Iterable<Rule>> foundRules = new ArrayList<>();
                        switch (arity) {
                            case 1:
                                foundRules.add(getRulesBottomUp(label, new int[]{a}));
                                break;
                            case 2:
                                IntCollection partnerList = bpFinder.getPartners(label, a);
                                for (int p : partnerList) {
                                    foundRules.add(getRulesBottomUp(label, new int[]{a, p}));
                                    foundRules.add(getRulesBottomUp(label, new int[]{p, a}));
                                }
                                break;
                            default:
                                Set[] partners = new Set[arity - 1];
                                Arrays.fill(partners, seen);
                                TupleIterator<Integer> partnerIterator = new TupleIterator<>(partners, new Integer[partners.length]);
                                while (partnerIterator.hasNext()) {
                                    Integer[] partnerTuple = partnerIterator.next();
                                    for (int pos = 0; pos < arity; pos++) {
                                        int[] children = new int[arity];
                                        for (int k = 0; k < pos; k++) {
                                            children[k] = partnerTuple[k];
                                        }
                                        children[pos] = a;
                                        for (int k = pos; k < partnerTuple.length; k++) {
                                            children[k + 1] = partnerTuple[k];
                                        }
                                        foundRules.add(getRulesBottomUp(label, children));
                                    }
                                }
                        }

                        //process found rules and add newfound rules to agenda
                        foundRules.forEach(ruleIt -> {
                            ruleIt.forEach(newRule -> {
                                double newInside = newRule.getWeight();
                                for (int child : newRule.getChildren()) {
                                    newInside *= state2Inside.get(child);
                                }
                                rule2Inside.put(newRule, newInside);
                                agenda.add(newRule);
                            });
                        });
                    }
                }

                bpFinder.addState(a);
            }
        }

        return ret;
    }
    
    /**
     * Writes the decomposition automata for a given corpus.
     * Calls {@code writeDecompositionAutomata} with the given arguments.
     * Call without arguments to receive help.
     * Takes eight arguments:
     * 1. The path to the the corpus. It should have "graph" and "string"
     * interpretations.
     * 2. How many sources to use in decomposition.
     * 3. At which index to start (inclusive, graphs are ordered by node count)
     * 4. At which index to stop (exclusive, graphs are ordered by node count)
     * Here indices are 0-based.
     * 5. A hard cap on the number of nodes allowed per graph
     * (violating graphs are not parsed). 0 for no restriction.
     * 6. A parameter k. If for a node count n, k is less then the number
     * of graphs with n nodes, then k are chosen randomly. Use 0 to use all graphs.
     * 7. targetFolderPath: The folder into which the automata are written.
     * 8. the file with alignments in form 4-5 6-7 8-7 etc (-> fast align)
     * 9. the file which indicates which nodename corresponds to which integer in the alignments
     * 10. the threshold at which to cut rules.
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        if (args.length<10) {
            System.out.println("Need ten arguments (not all found), these are: corpusPath sourceCount startIndex stopIndex maxNodes maxPerNodeCount targetFolderPath alignmentFilePath nodeNameListFilePath threshold");
            String defaultArgs = "examples/AMRAllCorpus.txt 3 754 755 0 0 output/ examples/amrHere/AMRAllTrain.align examples/amrHere/AMRAllTrain.names 0.2";
            System.out.println("using default arguments instead: "+defaultArgs);
            args = defaultArgs.split(" ");
        }
        
        String corpusPath = args[0];
        int sourceCount = Integer.valueOf(args[1]);
        int start = Integer.valueOf(args[2]);
        int stop = Integer.valueOf(args[3]);
        int maxNodes = Integer.valueOf(args[4]);
        if (maxNodes == 0) {
            maxNodes = 256;//this is arbitrarily chosen.
        }
        int maxPerNodeCount = Integer.valueOf(args[5]);
        if (maxPerNodeCount == 0) {
            maxPerNodeCount = Integer.MAX_VALUE;
        }
        String targetPath = args[6];
        FileReader alignmentReader = new FileReader(args[7]);
        FileReader nodeNameListReader = new FileReader(args[8]);
        
        Reader corpusReader = new FileReader(corpusPath);
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(null);
        Interpretation graphInt = new Interpretation(new GraphAlgebra(), null);
        Interpretation stringInt = new Interpretation(new StringAlgebra(), null);
        irtg.addInterpretation("graph", graphInt);
        irtg.addInterpretation("string", stringInt);
        Corpus corpus = Corpus.readCorpus(corpusReader, irtg);
        
        writeDecompositionAutomata(targetPath, corpus, start, stop, sourceCount, maxNodes, maxPerNodeCount, new BufferedReader(alignmentReader), new BufferedReader(nodeNameListReader), Double.valueOf(args[9]));
    }
    
    
    
    
    
}
