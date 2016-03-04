/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphGrammarInductionAlgebra;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.BinaryPartnerFinder;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
public class TestAlignmentConnectivity {
    
    private final static String combineLabel = "comb";
    private final static String reverseCombineLabel = "rcomb";
    private final static String unalignedLabelPrefix = "unaligned";
    private final static String reverseUnalignedLabelPrefix = "rUnaligned";
    
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, ParserException {
        
        //System.err.println("reading input...");
        
        //setup input
        if (args.length<7) {
            System.out.println("Need 7 arguments (not all found), these are: sourceCount corpusPath targetFolderPath alignmentTargetFolderPath alignmentFilePath nodeNameListFilePath maxAlignmentDistInGraph");
            String defaultArgs = "3 examples/AMRAllCorpusExplicit.txt output/test/ output/test/alignments/ examples/amrHere/AMRExplicit.align examples/amrHere/AMRExplicit.names 3";
            System.out.println("using default arguments instead: "+defaultArgs);
            args = defaultArgs.split(" ");
        }
        int maxSources = Integer.valueOf(args[0]);
        String corpusPath = args[1];
        String targetPath = args[2];
        String alignmentsTargetPath = args[3];
        BufferedReader alignmentReader = new BufferedReader(new FileReader(args[4]));
        BufferedReader nodeNameListReader = new BufferedReader(new FileReader(args[5]));
        int maxAlignmentDistInGraph = Integer.valueOf(args[6]);
        FileWriter filteredAlignmentWriter = new FileWriter(targetPath+"filtered.align");
        
        
        //setup corpus form input
        Reader corpusReader = new FileReader(corpusPath);
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(null);
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        Corpus corpus = Corpus.readCorpus(corpusReader, irtg4Corpus);
        
        //int max = 3;
        
        int j = 0;
        for (Instance instance : corpus) {
            
            
                //System.err.println("starting instance "+j+"...");
                SGraph graph = (SGraph)instance.getInputObjects().get("graph");

                StringAlgebra stringAlg = new StringAlgebra();

                //alignments
                String alignmentString = alignmentReader.readLine();
                List<String> alignmentsLeft = new ArrayList<>();
                alignmentsLeft.addAll(Arrays.asList(alignmentString.split(" +")));
                int initialAlignmentCount = alignmentsLeft.size();
                String nodeNameString = nodeNameListReader.readLine();
                Pair<Map<String, Set<Integer>>, Int2IntMap> node2AlignAndAlign2StrPos = parseAlignments(alignmentString, nodeNameString);
                Object2IntMap<String> nodeName2Alignment = new Object2IntOpenHashMap<>();
                Int2ObjectMap<String> alignment2NodeName = new Int2ObjectOpenHashMap<>();
                List<Integer> orderedAlignments = new ArrayList<>();
                boolean alignmentsWorkOut = doAlignments(node2AlignAndAlign2StrPos, nodeName2Alignment, alignment2NodeName, orderedAlignments);
                List<String> stringInput = (List<String>)stringAlg.parseString(orderedAlignments.stream()
                    .map(al -> String.valueOf(al)).collect(Collectors.joining(" ")));
                /*for (String nodeName : graph.getAllNodeNames()) {
                    if (!nodeName2Alignment.containsKey(nodeName)) {
                        alignmentsWorkOut = false;
                    }
                }*/
                if (!alignmentsWorkOut) {
                    System.err.println("graph "+j+" ("+graph.getAllNodeNames().size()+" nodes): incompatible alignments");
                    j++;
                    continue;
                }




                int alignmentToRemove = alignmentToRemove(stringInput, graph, alignment2NodeName, maxSources);
                StringJoiner sj = null;
                if (maxAlignmentDistInGraph > 0) {
                    while (alignmentToRemove >= 0) {
                        //if alignmentToRemove is -1, then the dist was less than maxAlignmentDistInGraph

                        //System.out.println(alignmentToRemove);
                        alignmentsLeft.remove(alignmentToRemove);
                        sj = new StringJoiner(" ");
                        for (String alignmentLeft : alignmentsLeft) {
                            sj.add(alignmentLeft);
                        }
                        doAlignments(parseAlignments(sj.toString(), nodeNameString), nodeName2Alignment, alignment2NodeName, orderedAlignments);
                        stringInput = (List<String>)stringAlg.parseString(orderedAlignments.stream()
                            .map(al -> String.valueOf(al)).collect(Collectors.joining(" ")));
                        alignmentToRemove = alignmentToRemove(stringInput, graph, alignment2NodeName, maxAlignmentDistInGraph);
                    }
                }
                filteredAlignmentWriter.write(((sj == null)? alignmentString : sj.toString())+"\n");


                




                Signature irtgSignature = new Signature();
                irtgSignature.addSymbol(combineLabel, 2);
                //irtgSignature.addSymbol(reverseCombineLabel, 2);

                /*Signature graphSignature = new Signature();
                graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_COMBINE, 2);
                graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_EXPLICIT, 1);*/


                //graph signature, wrapper signature, and maps
                Map<String, String> constLabel2StringConstLabel = new HashMap<>();
                Map<String, String> constLabel2NodeName = new HashMap<>();
                int i = 0;
                for (String nodeName : graph.getAllNodeNames()) {
                    String constLabel = "const"+i;
                    String graphLabel = "G"+i;
                    //graphSignature.addSymbol(graphLabel, 0);
                    if (nodeName2Alignment.containsKey(nodeName)) {
                        irtgSignature.addSymbol(constLabel, 0);
                        constLabel2NodeName.put(constLabel, nodeName);
                        int alignment = nodeName2Alignment.get(nodeName);
                        constLabel2StringConstLabel.put(constLabel, String.valueOf(alignment));
                    } else {
                        irtgSignature.addSymbol(unalignedLabelPrefix+constLabel, 1);
                        constLabel2NodeName.put(unalignedLabelPrefix+constLabel, nodeName);
                    }
                    i++;
                }


                //System.err.println(stringInput);
                TreeAutomaton<StringAlgebra.Span> stringAuto = stringAlg.decompose(stringInput);

                //System.err.println("automaton setup...");
                //synchr parsing
                ConnectivitySynchParsingAutomaton synchAuto = new ConnectivitySynchParsingAutomaton(irtgSignature, stringAuto, constLabel2StringConstLabel, constLabel2NodeName,
                        new GraphInfo(graph, makeSuitableGraphAlgebra(maxSources, graph.getGraph().edgeSet().stream().map(e -> e.getLabel()).collect(Collectors.toSet()))));
                //System.err.println("parsing...");
                TreeAutomaton<Pair<StringAlgebra.Span, GraphGrammarInductionAlgebra.BrAndEdges>> concAuto = synchAuto.asConcreteTreeAutomatonBottomUp();
                //System.err.println("collecting rules...");
                concAuto.makeAllRulesExplicit();

                int alignmentsRemoved = initialAlignmentCount-alignmentsLeft.size();
                System.err.println("graph "+j+" ("+graph.getAllNodeNames().size()+" nodes): " +((synchAuto.getFinalStates().isEmpty())? "failed" : "success") +" ------ "+alignmentsRemoved+"/"+initialAlignmentCount+" alignments removed");

            j++;
            /*if (j>= max) {
                break;//for the moment, limit the amount of instances
            }*/
        }
        filteredAlignmentWriter.close();
    }
    
    private static GraphAlgebra makeSuitableGraphAlgebra(int maxSources, Set<String> edgeLabels) {
        Signature sig = new Signature();
        
        sig.addSymbol(GraphAlgebra.OP_MERGE, 2);
        
        for (int s = 0; s<maxSources; s++) {
            sig.addSymbol(GraphAlgebra.OP_FORGET+s, 1);
            for (int s2 = s; s2<maxSources; s2++) {
                sig.addSymbol(GraphAlgebra.OP_RENAME+s+"_"+s2, 1);
                sig.addSymbol(GraphAlgebra.OP_RENAME+s2+"_"+s, 1);
                sig.addSymbol(GraphAlgebra.OP_SWAP+s+"_"+s2, 1);
                sig.addSymbol(GraphAlgebra.OP_SWAP+s2+"_"+s, 1);
            }
        }
        
        //constants
        Set<String> attachToSource = GraphGrammarInductionAlgebra.getAttachToSourceLabels();
        for (String l : edgeLabels) {
            for (int s = 1; s<maxSources; s++) {
                SGraph edgeGraph = new SGraph();
                GraphNode sourceNode = edgeGraph.addNode("src", null);
                GraphNode targetNode = edgeGraph.addNode("tgt", null);
                edgeGraph.addEdge(sourceNode, targetNode, l);
                if (attachToSource.contains(l)) {
                    edgeGraph.addSource(String.valueOf(0), sourceNode.getName());
                    edgeGraph.addSource(String.valueOf(s), targetNode.getName());
                } else {
                    edgeGraph.addSource(String.valueOf(s), sourceNode.getName());
                    edgeGraph.addSource(String.valueOf(0), targetNode.getName());
                }
                String edgeString = edgeGraph.toIsiAmrString();
                sig.addSymbol(edgeString, 0);
            }
        }
        
        return new GraphAlgebra(sig);
    }
    
    private static int alignmentToRemove(List<String> stringInput, SGraph graph, Int2ObjectMap<String> alignment2NodeName, int maxAllowed) {
        int spanWidth = 1;
        int lookRange = 1;
        GraphInfo graphInfo = new GraphInfo(graph, new GraphAlgebra());
        
        int ret = -1;
        int maxFound = -1;
        
        if (stringInput.size() <= 2) {
            //then we don't want to return anything else
            
            return -1;
        }
        
        for (int i = 0; i <= stringInput.size()-spanWidth; i++) {
            int min = Integer.MAX_VALUE;
            for (int k = 0; k<spanWidth; k++) {
                String baseNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i+k)));
                for (int minus = 1; minus <= lookRange; minus++) {
                    if (i-minus > 0) {
                        String otherNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i-minus)));
                        try {
                            min = Math.min(min, graphInfo.dist(graphInfo.getIntForNode(baseNodeName), graphInfo.getIntForNode(otherNodeName)));
                        } catch (java.lang.NullPointerException ex) {
                            //System.err.println("base node name " + ((baseNodeName == null)? "null" : baseNodeName));
                            //System.err.println("other node name " + ((otherNodeName == null)? "null" : otherNodeName));
                        }
                    }
                }
                for (int plus = 1; plus <= lookRange; plus++) {
                    if (i+spanWidth-1+plus < stringInput.size()) {
                        String otherNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i+spanWidth-1+plus)));
                        try {
                            min = Math.min(min, graphInfo.dist(graphInfo.getIntForNode(baseNodeName), graphInfo.getIntForNode(otherNodeName)));
                        } catch (java.lang.NullPointerException ex) {
                            //System.err.println("base node name " + ((baseNodeName == null)? "null" : baseNodeName));
                            //System.err.println("other node name " + ((otherNodeName == null)? "null" : otherNodeName));
                        }
                    }
                }
            }
            if (min > maxFound) {
                ret = Integer.parseInt(stringInput.get(i));
                maxFound = min;
            }
            //System.err.println(min);
        }
        if (maxFound > maxAllowed) {
            return ret;
        } else {
            return -1;
        }
    }
    
    private static boolean doAlignments(Pair<Map<String, Set<Integer>>, Int2IntMap> node2AlignAndAlign2StrPos,
            Object2IntMap<String> nodeName2Alignment,
            Int2ObjectMap<String> alignment2NodeName,
            List<Integer> orderedAlignments) {
        
        
        Int2IntMap align2StrPos = node2AlignAndAlign2StrPos.right;
        
        boolean ret = true;
        
        nodeName2Alignment.clear();
        alignment2NodeName.clear();
        orderedAlignments.clear();
        
        for (Entry<String, Set<Integer>> entry : node2AlignAndAlign2StrPos.left.entrySet()) {
            if (entry.getValue().size() > 1) {
                ret = false;
            } else if (entry.getValue().size() == 1) {
                orderedAlignments.add(entry.getValue().iterator().next());
                nodeName2Alignment.put(entry.getKey(), entry.getValue().iterator().next());
                alignment2NodeName.put(entry.getValue().iterator().next(), entry.getKey());
            }
        }
        Collections.sort(orderedAlignments, (s1, s2) -> align2StrPos.get(s1)-align2StrPos.get(s2));
        return ret;
    }
    
    
    
    private static class ConnectivitySynchParsingAutomaton extends TreeAutomaton<Pair<StringAlgebra.Span, IntSet>> {

        private final Int2IntMap state2LhsState = new Int2IntOpenHashMap();
        private final Int2IntMap lhsState2State = new Int2IntOpenHashMap();
        private final int concatID;
        private final TreeAutomaton<StringAlgebra.Span> lhs;
        private final Map<String, String> constLabel2StringConstLabel;
        private final Map<String, String> constLabel2NodeName;
        private final GraphInfo graphInfo;
        
        public ConnectivitySynchParsingAutomaton(Signature signature, TreeAutomaton<StringAlgebra.Span> lhs,
                Map<String, String> constLabel2StringConstLabel,
                Map<String, String> constLabel2NodeName,
                GraphInfo graphInfo) {
            super(signature);
            this.lhs = lhs;
            concatID = lhs.getSignature().getIdForSymbol(StringAlgebra.CONCAT);
            this.constLabel2StringConstLabel = constLabel2StringConstLabel;
            this.constLabel2NodeName = constLabel2NodeName;
            this.graphInfo = graphInfo;
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            String label = signature.resolveSymbolId(labelId);
            
            List<Rule> ret = new ArrayList<>();
            
            if (label.equals(combineLabel)) {
                
                
                //lhs
                int[] lhsChildren = new int[2];
                lhsChildren[0] = state2LhsState.get(childStates[0]);
                lhsChildren[1] = state2LhsState.get(childStates[1]);
                Iterable<Rule> lhsRules = lhs.getRulesBottomUp(concatID, lhsChildren);
                
                //the next iteration is over max one rule since lhs is deterministic for concat, so no loss of efficiency here
                //basically a shortcut for checking if this is empty
                for (Rule lhsRule : lhsRules) {
                    IntSet leftSet = getStateForId(childStates[0]).right;
                    IntSet rightSet = getStateForId(childStates[1]).right;
                    if (isConnected(leftSet, rightSet)) {
                        IntSet merge = new IntOpenHashSet();
                        merge.addAll(leftSet);
                        merge.addAll(rightSet);
                        ret.add(createRule(addState(new Pair(lhs.getStateForId(lhsRule.getParent()), merge)), labelId, childStates, 1.0));
                    }
                }
                
            } else if (label.startsWith(unalignedLabelPrefix)) {
                
                //get constant for unaligned node
                IntSet rhsChild = getStateForId(childStates[0]).right;
                IntSet rhsConst = new IntOpenHashSet();
                rhsConst.add(graphInfo.getIntForNode(constLabel2NodeName.get(label)));
                
                if (isConnected(rhsChild, rhsConst)) {
                    IntSet merge = new IntOpenHashSet();
                    merge.addAll(rhsChild);
                    merge.addAll(rhsConst);
                    ret.add(createRule(addState(new Pair(lhs.getStateForId(state2LhsState.get(childStates[0])), merge)), labelId, childStates, 1.0));
                }
                
                
                
                
            } else {
                //then we have a constant
                
                StringAlgebra.Span lhsState = lhs.getStateForId(lhs.getRulesBottomUp(lhs.getSignature().getIdForSymbol(constLabel2StringConstLabel.get(label)),
                        new int[0]).iterator().next().getParent());
                
                IntSet rhsState = new IntOpenHashSet();
                rhsState.add(graphInfo.getIntForNode(constLabel2NodeName.get(label)));
                
                ret.add(createRule(addState(new Pair(lhsState, rhsState)), labelId, childStates, 1.0));
                
                
            }
            
            return ret;
            
        }
        
        private boolean isConnected(IntSet left, IntSet right) {
            
            for (IntIterator leftIt = left.iterator();leftIt.hasNext();) {
                int leftNode = leftIt.nextInt();
                for (IntIterator rightIt = right.iterator();rightIt.hasNext();) {
                    int rightNode = rightIt.nextInt();
                    if (graphInfo.dist(leftNode, rightNode) == 1) {
                        return true;
                    }
                }
            }
            return false;
        }
        

        @Override
        protected int addState(Pair<StringAlgebra.Span, IntSet> state) {
            int ret = super.addState(state);
            int leftID = lhs.getIdForState(state.left);
            lhsState2State.put(leftID, ret);
            state2LhsState.put(ret, leftID);
            if (lhs.getFinalStates().contains(leftID)) {
                //only want to check if alignments work, rest of graph can always be added, so we don't need to check
                addFinalState(ret);
            }
            return ret;
        }

        
        @Override
        public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean supportsBottomUpQueries() {
            return true;
        }

        @Override
        public boolean supportsTopDownQueries() {
            return false;
        }

        @Override
        public BinaryPartnerFinder makeNewBinaryPartnerFinder() {
            return new BinaryPartnerFinder() {

                private BinaryPartnerFinder lhsBPF = lhs.makeNewBinaryPartnerFinder();
                
                @Override
                public IntCollection getPartners(int labelID, int stateID) {
                    return lhsBPF.getPartners(lhs.getSignature().getIdForSymbol(StringAlgebra.CONCAT), state2LhsState.get(stateID))
                            .stream().map(lhsID -> lhsState2State.get(lhsID)).collect(Collector.of(
                                () -> new IntArrayList(),
                                (IntList list, Integer val) -> list.add(val),
                                (IntList list, IntList list2) -> {
                                    IntList ret = new IntArrayList();
                                    ret.addAll(list);
                                    ret.addAll(list2);
                                    return ret;
                                }));
                }

                @Override
                public void addState(int stateID) {
                    lhsBPF.addState(state2LhsState.get(stateID));
                }
            };
        }
        
        

        @Override
        public boolean isBottomUpDeterministic() {
            return true; //true for the current incarnation, could change in future
        }
        
    }
    
    public static Pair<Map<String, Set<Integer>>, Int2IntMap> parseAlignments(String alignmentString, String nodeNameString) {
        //setup
        Map<String, Set<Integer>> nodeName2Alignments = new HashMap<>();
        Int2IntMap alignment2StringPos = new Int2IntOpenHashMap();
        
        
        if (alignmentString.length() < 3) {
            //then it cannot describe an alignment
            return new Pair(nodeName2Alignments, alignment2StringPos);
        }
        
        String[] nodeNames = nodeNameString.split(" +");
        String[] alignments = alignmentString.split(" +");
        
        //check for duplicate names, and add empty Sets
        for (int i = 0; i < nodeNames.length; i++) {
            for (int j = i+1; j<nodeNames.length; j++) {
                if (nodeNames[i].equals(nodeNames[j])) {
                    System.err.println("WARNING: Node name '"+nodeNames[i]+"' appears twice in '"+nodeNameString+"'!");
                }
            }
            nodeName2Alignments.put(nodeNames[i], new HashSet<>());
        }
        
        
        //parse actual alignments
        for (int i = 0; i < alignments.length; i++) {
            String[] stringAndGraph = alignments[i].split("-");
            String nodeName = nodeNames[Integer.parseInt(stringAndGraph[1])];
            nodeName2Alignments.get(nodeName).add(i);
            alignment2StringPos.put(i, Integer.parseInt(stringAndGraph[0]));
        }
        
        return new Pair(nodeName2Alignments, alignment2StringPos);
    }
    
}
